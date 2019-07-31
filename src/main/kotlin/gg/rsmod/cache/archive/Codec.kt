package gg.rsmod.cache.archive

import com.github.michaelbull.result.*
import gg.rsmod.cache.FileSystem
import gg.rsmod.cache.domain.*
import gg.rsmod.cache.io.FileSystemFile
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.ReadWritePacket
import gg.rsmod.cache.io.WriteOnlyPacket
import gg.rsmod.cache.util.*
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
        packet.p3(pointer.length)
        packet.p3(pointer.offset)
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
        packet.p2(dataBlock.group)
        packet.p2(dataBlock.currBlock)
        packet.p3(dataBlock.nextBlock)
        packet.p1(dataBlock.archive)
    }

    fun decodeExtended(packet: ReadOnlyPacket): FileSystemDataBlock {
        val group = packet.g4
        val currBlock = packet.g2
        val nextBlock = packet.g3
        val archive = packet.g1
        return FileSystemDataBlock(archive = archive, group = group, currBlock = currBlock, nextBlock = nextBlock)
    }

    fun encodeExtended(dataBlock: FileSystemDataBlock, packet: WriteOnlyPacket) {
        packet.p4(dataBlock.group)
        packet.p2(dataBlock.currBlock)
        packet.p3(dataBlock.nextBlock)
        packet.p1(dataBlock.archive)
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
                return Err(DataArchiveMismatch)
            }

            if (block.group != group) {
                return Err(DataGroupMismatch)
            }

            if (block.currBlock != currBlock) {
                return Err(DataBlockMismatch)
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
        packet.gdata(encryptedData)
        crc.update(encryptedData)

        val decryptedData = Xtea.decipher(encryptedData, keys)

        if (packet.readableBytes >= Short.SIZE_BYTES) {
            val version = packet.g2s
            if (version == -1) {
                return Err(IllegalVersion)
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
            else -> return Err(IllegalCompressionType)
        }

        if (decompressedData.size != decompressedLength) {
            return Err(CompressionLengthMismatch)
        }

        return Ok(decompressedData)
    }

    fun encode(
        data: ByteArray,
        compression: Int,
        version: Int?,
        keys: IntArray,
        packet: ReadWritePacket
    ): Result<Unit, DomainMessage> {
        val compressed = when (compression) {
            Compression.NONE -> data
            Compression.GZIP -> GZip.compress(data)
            Compression.BZIP2 -> BZip2.compress(data)
            else -> return Err(IllegalCompressionType)
        }

        packet.p1(compression)
        packet.p4(compressed.size)
        packet.pdata(compressed)

        if (version != null) {
            packet.p2(version)
        }

        if (!keys.contentEquals(Xtea.EMPTY_KEY_SET)) {
            Xtea.encipher(packet, 5, compressed.size + (if (compression == Compression.NONE) 5 else 9), keys)
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

            val decodeRes = decodeIndex(
                crc = CRC32(),
                indexFile = indexFile,
                masterIndex = masterIndex,
                dataFile = dataFile,
                indexBuf = ReadOnlyPacket.of(indexBuf),
                dataBuf = dataBuf,
                headerLength = headerLength,
                dataLength = dataLength
            )

            // TODO: starts getting a bit sus here with err...
            //  let's try to improve this
            val decodeErr = decodeRes.getError()
            if (decodeErr != null) {
                return Err(decodeErr)
            }

            indexes[indexFile] = IndexCodec.decode(ReadOnlyPacket.of(decodeRes.get()!!))
        }

        return Ok(indexes)
    }

    fun encode(packet: WriteOnlyPacket) {
        TODO()
    }

    private fun decodeIndex(
        crc: CRC32,
        indexFile: Int,
        masterIndex: Int,
        dataFile: FileSystemFile,
        indexBuf: ReadOnlyPacket,
        dataBuf: ByteArray,
        headerLength: Int,
        dataLength: Int
    ): Result<ByteArray, DomainMessage> {
        return DataBlockPointerCodec.decode(indexBuf)
            .andThen { metadata ->
                DataCodec.decode(
                    dataFile = dataFile, archive = masterIndex, group = indexFile,
                    extended = false, offset = metadata.offset, headerLength = headerLength,
                    blockLength = dataLength, totalLength = metadata.length, tmpDataBuf = dataBuf
                    )
            }.andThen { dataBlock ->
                CompressionCodec.decode(
                    ReadOnlyPacket.of(dataBlock.data), crc,
                    Xtea.EMPTY_KEY_SET, FileSystem.VALID_COMPRESSION_LENGTH
                )
            }
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

        return DataBlockPointerCodec.decode(ReadOnlyPacket.of(tmpIdxBuf))
            .andThen { dataPointer ->
                DataCodec.decode(
                    dataFile = dataFile, archive = archive, group = group,
                    extended = extended, offset = dataPointer.offset,
                    headerLength = dataHeaderLength, blockLength = dataBlockLength,
                    totalLength = dataPointer.length, tmpDataBuf = tmpDataBuf
                )
            }.andThen {
                Ok(it.data)
            }
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
                packet.gdata(fileData[id], fileOffsets[id], blockLength)
                fileOffsets[id] += blockLength
            }
        }

        return fileData
    }
}