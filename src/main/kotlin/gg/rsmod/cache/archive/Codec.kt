package gg.rsmod.cache.archive

import com.github.michaelbull.result.*
import gg.rsmod.cache.domain.*
import gg.rsmod.cache.io.FileSystemFile
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.WriteOnlyPacket
import gg.rsmod.cache.util.*
import io.netty.buffer.Unpooled
import java.util.zip.CRC32
import kotlin.math.min

internal object DataBlockPointerCodec {

    fun decode(packet: ReadOnlyPacket): Result<FileSystemDataBlockPointer, DomainMessage> {
        val length = packet.g3
        val offset = packet.g3

        if (length <= 0) {
            return Err(DataBlockPointerInvalidLength)
        }

        if (offset <= 0) {
            return Err(DataBlockPointerInvalidOffset)
        }

        return Ok(FileSystemDataBlockPointer(offset, length))
    }

    fun encode(pointer: FileSystemDataBlockPointer, packet: WriteOnlyPacket) {
        packet.writeMedium(pointer.length)
        packet.writeMedium(pointer.offset)
    }
}

internal object DataBlockCodec {

    fun decode(packet: ReadOnlyPacket): FileSystemDataBlock {
        val group = packet.g2
        val currBlock = packet.g2
        val nextBlock = packet.g3
        val archive = packet.g1
        return FileSystemDataBlock(archive = archive, group = group, currBlock = currBlock, nextBlock = nextBlock)
    }

    fun encode(dataBlock: FileSystemDataBlock, packet: WriteOnlyPacket) {
        packet.writeShort(dataBlock.group)
        packet.writeShort(dataBlock.currBlock)
        packet.writeMedium(dataBlock.nextBlock)
        packet.writeByte(dataBlock.archive)
    }

    fun decodeExtended(packet: ReadOnlyPacket): FileSystemDataBlock {
        val group = packet.g4
        val currBlock = packet.g2
        val nextBlock = packet.g3
        val archive = packet.g1
        return FileSystemDataBlock(archive = archive, group = group, currBlock = currBlock, nextBlock = nextBlock)
    }

    fun encodeExtended(dataBlock: FileSystemDataBlock, packet: WriteOnlyPacket) {
        packet.writeInt(dataBlock.group)
        packet.writeShort(dataBlock.currBlock)
        packet.writeMedium(dataBlock.nextBlock)
        packet.writeByte(dataBlock.archive)
    }
}

internal object DataCodec {

    fun decode(
        dataFile: FileSystemFile,
        archive: Int,
        group: Int,
        extended: Boolean,
        offset: Int,
        headerLength: Int,
        blockLength: Int,
        totalLength: Int,
        tmpDataBuf: ByteArray
    ): Result<FileSystemData, DomainMessage> {
        val data = ByteArray(totalLength)

        var totalBytesRead = 0
        var currBlock = 0
        var nextBlock = offset

        while (totalBytesRead < totalLength) {
            val payloadLength = min(blockLength - headerLength, totalLength - totalBytesRead)
            val dataLength = headerLength + payloadLength

            dataFile.seek(nextBlock.toLong() * blockLength)
            val dataRead = dataFile.read(tmpDataBuf, 0, dataLength)

            if (dataRead != dataLength) {
                return Err(DataBlockReadMalformation)
            }

            val block = if (extended) {
                DataBlockCodec.decodeExtended(ReadOnlyPacket.of(tmpDataBuf))
            } else {
                DataBlockCodec.decode(ReadOnlyPacket.of(tmpDataBuf))
            }

            if (block.archive != archive) {
                throw IllegalStateException("Archive mismatch: [expected=$archive, read=${block.archive}]")
            }

            if (block.group != group) {
                throw IllegalStateException("Group mismatch: [expected=$archive, read=${block.group}]")
            }

            if (block.currBlock != currBlock) {
                throw IllegalStateException("Block mismatch: [expected=$currBlock, read=${block.currBlock}]")
            }

            System.arraycopy(tmpDataBuf, headerLength, data, totalBytesRead, payloadLength)

            nextBlock = block.nextBlock
            totalBytesRead += payloadLength
            currBlock++
        }

        return Ok(FileSystemData(archive, group, data))
    }
}

internal object CompressionCodec {

    fun decode(
        packet: ReadOnlyPacket,
        crc: CRC32,
        keys: IntArray,
        validCompressionLength: IntRange
    ): Result<ByteArray, DomainMessage> {
        val compression = packet.g1
        val compressedLength = packet.g4

        if (compressedLength !in validCompressionLength) {
            return Err(CompressedLengthOutOfBounds)
        }

        crc.update(packet.array, 0, 5)

        val encryptedData = ByteArray(compressedLength + (if (compression == Compression.NONE) 0 else Int.SIZE_BYTES))
        packet.g1Array(encryptedData)
        crc.update(encryptedData)

        val decryptedData = Xtea.decrypt(encryptedData, keys)

        if (packet.readableBytes >= Short.SIZE_BYTES) {
            val version = packet.g2
            if (version == -1) {
                throw IllegalStateException("Invalid version: $version")
            }
        }

        if (compression == Compression.NONE) {
            return Ok(decryptedData)
        }

        val decompressedLength = ((decryptedData[0].toInt() and 0xFF) shl 24) or
                ((decryptedData[1].toInt() and 0xFF) shl 16) or
                ((decryptedData[2].toInt() and 0xFF) shl 8) or
                (decryptedData[3].toInt() and 0xFF)

        val decompressedContent = ByteArray(decryptedData.size - Int.SIZE_BYTES)
        System.arraycopy(decryptedData, Int.SIZE_BYTES, decompressedContent, 0, decompressedContent.size)

        val decompressedData = when (compression) {
            Compression.GZIP -> GZip.decompress(decompressedContent, compressedLength)
            Compression.BZIP2 -> BZip2.decompress(decompressedContent, compressedLength)
            else -> throw UnsupportedOperationException("Unhandled compression type: $compression")
        }

        if (decompressedData.size != decompressedLength) {
            throw AssertionError("Length of decompressed data does not match signature: [expected=$decompressedLength, actual=${decompressedData.size}]")
        }

        return Ok(decompressedData)
    }

    fun encode(
        data: ByteArray,
        compression: Int,
        version: Int?,
        keys: IntArray,
        packet: WriteOnlyPacket
    ): Result<Unit, DomainMessage> {
        val compressed = when (compression) {
            Compression.NONE -> data
            Compression.GZIP -> GZip.compress(data)
            Compression.BZIP2 -> BZip2.compress(data)
            else -> return Err(InvalidCompressionType)
        }

        packet.writeByte(compression)
        packet.writeInt(compressed.size)
        packet.writeBytes(compressed)

        if (version != null) {
            packet.writeShort(version)
        }

        if (!keys.contentEquals(Xtea.EMPTY_KEY_SET)) {
            // TODO this won't work. allow encode to modify the packet directly
            Xtea.encode(
                Unpooled.wrappedBuffer(packet.array),
                5,
                compressed.size + (if (compression == Compression.NONE) 5 else 9),
                keys
            )
        }
        return Ok(Unit)
    }
}

internal object MasterIndexCodec {

    fun decode(
        dataFile: FileSystemFile,
        masterIndexFile: FileSystemFile,
        masterIndex: Int,
        indexFiles: IntArray,
        headerLength: Int,
        indexLength: Int,
        dataLength: Int
    ): Result<Map<Int, Index>, DomainMessage> {
        val indexes = mutableMapOf<Int, Index>()

        val indexBuf = ByteArray(indexLength)
        val dataBuf = ByteArray(dataLength)

        indexFiles.forEach { indexFile ->
            masterIndexFile.seek(indexFile.toLong() * indexLength)
            masterIndexFile.read(indexBuf, 0, indexLength)

            val blockRes = DataBlockPointerCodec.decode(ReadOnlyPacket.of(indexBuf))
            // TODO: please god sku help
            if (blockRes.getError() != null) {
                return Err(blockRes.getError()!!)
            }

            val blockMetadata = blockRes.get()!!
            val length = blockMetadata.length
            val offset = blockMetadata.offset

            val dataBlock = DataCodec.decode(
                dataFile = dataFile, archive = masterIndex, group = indexFile,
                extended = false, offset = offset, headerLength = headerLength,
                blockLength = dataLength, totalLength = length, tmpDataBuf = dataBuf
            )

            // TODO: please god sku help
            if (dataBlock.getError() != null) {
                return Err(dataBlock.getError()!!)
            }

            val decompressed = CompressionCodec.decode(
                ReadOnlyPacket.of(dataBlock.get()!!.data),
                CRC32(), Xtea.EMPTY_KEY_SET, 0..1_000_000
            )

            // TODO: please god sku help
            if (decompressed.getError() != null) {
                return Err(dataBlock.getError()!!)
            }

            indexes[indexFile] = IndexCodec.decode(ReadOnlyPacket.of(decompressed.get()!!))
        }

        return Ok(indexes)
    }

    fun encode(packet: WriteOnlyPacket) {
        TODO()
    }
}

internal object IndexCodec {

    fun decode(packet: ReadOnlyPacket): Index {
        val formatType = packet.g1
        val format = when (formatType) {
            FormatType.NONE -> 0
            else -> packet.g4
        }

        val flags = packet.g1
        val hashedNames = (Index.HASHED_NAME_BIT and flags) != 0

        val groupCount = packet.readFormatSmart(formatType)
        val groupIds = IntArray(groupCount)
        val groupNames = if (hashedNames) IntArray(groupCount) else null
        val groupCrcs = IntArray(groupCount)
        val groupVersions = IntArray(groupCount)
        val groupFileCounts = IntArray(groupCount)
        val groupFileIds = mutableMapOf<Int, IntArray>()
        val groupFileNames = mutableMapOf<Int, IntArray>()

        var increment = 0
        for (i in 0 until groupCount) {
            val delta = packet.readFormatSmart(formatType)
            increment += delta
            groupIds[i] = increment
        }

        if (groupNames != null) {
            for (i in 0 until groupCount) {
                groupNames[i] = packet.g4
            }
        }

        for (i in 0 until groupCount) {
            groupCrcs[i] = packet.g4
        }

        for (i in 0 until groupCount) {
            groupVersions[i] = packet.g4
        }

        for (i in 0 until groupCount) {
            val fileCount = packet.readFormatSmart(formatType)
            groupFileCounts[i] = fileCount
        }

        for (i in 0 until groupCount) {
            val fileCount = groupFileCounts[i]
            val fileIds = groupFileIds.computeIfAbsent(i) { IntArray(fileCount) }

            increment = 0
            for (j in 0 until fileCount) {
                val delta = packet.readFormatSmart(formatType)
                increment += delta
                fileIds[j] = increment
            }
        }

        if (hashedNames) {
            for (i in 0 until groupCount) {
                val fileCount = groupFileCounts[i]
                val fileNames = groupFileNames.computeIfAbsent(i) { IntArray(fileCount) }

                for (j in 0 until fileCount) {
                    fileNames[j] = packet.g4
                }
            }
        }

        val groups = mutableListOf<Group>()
        for (i in 0 until groupCount) {
            val groupId = groupIds[i]
            val groupName = groupNames?.get(i)
            val groupCrc = groupCrcs[i]
            val groupVersion = groupVersions[i]
            val groupFileCount = groupFileCounts[i]

            val fileIds = groupFileIds[i]!!
            val fileNames = groupFileNames[i]

            val files: List<GroupFile> = if (fileNames != null) {
                fileIds.mapIndexed { index, id -> NamedGroupFile(id, fileNames[index]) }
            } else {
                fileIds.map { GroupFile(it) }
            }

            val group = if (groupName != null) {
                NamedGroup(groupId, groupCrc, groupVersion, files.toTypedArray(), groupName)
            } else {
                Group(groupId, groupCrc, groupVersion, files.toTypedArray())
            }

            groups.add(group)
        }

        return Index(format, flags, groups.toTypedArray())
    }

    fun encode(packet: WriteOnlyPacket) {
        TODO()
    }

    private fun ReadOnlyPacket.readFormatSmart(format: Int): Int = if (format == FormatType.SMART) {
        gSmart2Or4
    } else {
        g2
    }
}

internal object GroupCodec {

    fun decode(
        dataFile: FileSystemFile,
        idxFile: FileSystemFile,
        archive: Int,
        group: Int,
        extended: Boolean,
        idxBlockLength: Int,
        dataHeaderLength: Int,
        dataBlockLength: Int,
        tmpIdxBuf: ByteArray,
        tmpDataBuf: ByteArray
    ): Result<ByteArray, DomainMessage> {
        idxFile.seek(group.toLong() * idxBlockLength)
        val read = idxFile.read(tmpIdxBuf)

        // If the amount of bytes read from the idx file does not match the
        // given index byte length, we provide an error.
        if (read != idxBlockLength) {
            return Err(MalformedIndexRead)
        }

        val pointerRes = DataBlockPointerCodec.decode(ReadOnlyPacket.of(tmpIdxBuf))
        // TODO: please god sku help
        if (pointerRes.getError() != null) {
            return Err(pointerRes.getError()!!)
        }

        val dataPointer = pointerRes.get()!!
        val dataOffset = dataPointer.offset
        val dataLength = dataPointer.length

        val dataBlock = DataCodec.decode(
            dataFile = dataFile, archive = archive, group = group,
            extended = extended, offset = dataOffset,
            headerLength = dataHeaderLength, blockLength = dataBlockLength,
            totalLength = dataLength, tmpDataBuf = tmpDataBuf
        )

        // TODO: please god sku help
        if (dataBlock.getError() != null) {
            return Err(dataBlock.getError()!!)
        }

        return Ok(dataBlock.get()!!.data)
    }

    fun encode(packet: WriteOnlyPacket) {
        TODO()
    }
}

internal object GroupFileCodec {

    fun decode(packet: ReadOnlyPacket, fileCount: Int): Array<ByteArray> {
        val bufLength = packet.readableBytes

        // Read the last byte of the packet, which has the total amount of blocks.
        packet.position = bufLength - Byte.SIZE_BYTES
        val totalBlocks = packet.g1

        // Start reading from the position that contains the block sizes.
        packet.position = bufLength - Byte.SIZE_BYTES - (totalBlocks * (fileCount * Int.SIZE_BYTES))

        val blockLengths = Array(fileCount) { IntArray(totalBlocks) }
        val fileLengths = IntArray(fileCount)

        for (block in 0 until totalBlocks) {
            var blockLength = 0
            for (id in 0 until fileCount) {
                val delta = packet.g4
                blockLength += delta

                blockLengths[id][block] = blockLength
                fileLengths[id] += blockLength
            }
        }

        // Reset the packet position back to the beginning to read the actual
        // data.
        packet.reset()

        val fileData = Array(fileCount) { ByteArray(fileLengths[it]) }
        val fileOffsets = IntArray(fileCount)

        for (block in 0 until totalBlocks) {
            for (id in 0 until fileCount) {
                val blockLength = blockLengths[id][block]
                packet.g1Array(fileData[id], fileOffsets[id], blockLength)
                fileOffsets[id] += blockLength
            }
        }

        return fileData
    }
}