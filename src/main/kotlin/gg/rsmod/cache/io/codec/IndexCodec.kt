package gg.rsmod.cache.io.codec

import gg.rsmod.cache.index.Index
import gg.rsmod.cache.index.IndexFormat
import gg.rsmod.cache.io.*

/**
 * @author Tom <rspsmods@gmail.com>
 */
internal object IndexCodec : CacheCodec<Index> {

    override fun decode(buf: CacheBuf): Index {
        val formatOpcode = buf.readUnsignedByte()
        val formatType = IndexFormat.values.firstOrNull { it.opcode == formatOpcode } ?: throw IllegalArgumentException("Illegal format opcode: $formatOpcode")

        val format = if (formatType == IndexFormat.NONE) 0 else buf.readInt()
        val flags = buf.readUnsignedByte()
        val groupCount = buf.readFormatSmart(formatType)

        val index = Index(format, flags, groupCount)

        var increment = 0
        for (i in 0 until groupCount) {
            val delta = buf.readFormatSmart(formatType)
            increment += delta
            index.groupIds[i] = increment
        }

        if (index.groupNames != null) {
            for (i in 0 until groupCount) {
                index.groupNames[i] = buf.readInt()
            }
        }

        for (i in 0 until groupCount) {
            index.groupCrcs[i] = buf.readInt()
        }

        for (i in 0 until groupCount) {
            index.groupVersions[i] = buf.readInt()
        }

        for (i in 0 until groupCount) {
            val fileCount = buf.readInt()
            index.groupFileCounts[i] = fileCount
            index.groupFileIds[i] = IntArray(fileCount)
            index.groupFileNames[i] = if (index.containsNames) IntArray(fileCount) else IntArray(0)
        }

        for (i in 0 until groupCount) {
            val fileCount = index.groupFileCounts[i]
            val fileIds = index.groupFileIds[i]

            increment = 0
            for (j in 0 until fileCount) {
                val delta = buf.readFormatSmart(formatType)
                increment += delta
                fileIds[j] = increment
            }
        }

        if (index.containsNames) {
            for (i in 0 until groupCount) {
                val fileCount = index.groupFileCounts[i]
                val fileNames = index.groupFileNames[i]

                for (j in 0 until fileCount) {
                    fileNames[j] = buf.readInt()
                }
            }
        }

        return index
    }

    override fun encode(buf: CacheBuf, value: Index) {
        TODO()
    }
}