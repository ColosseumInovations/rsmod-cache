package gg.rsmod.cache.util

/**
 * Contains metadata that points to a block of data.
 *
 * @param offset the offset where the block begins, in bytes.
 * @param length the total length of the block of data, in bytes.
 *
 * @author Tom
 */
internal data class FileSystemDataBlockPointer(
    val offset: Int, val length: Int
)

/**
 * @author Tom
 */
internal data class FileSystemDataBlock(
    val archive: Int, val group: Int, val currBlock: Int, val nextBlock: Int
)

/**
 * @author Tom
 */
internal data class FileSystemData(val archive: Int, val group: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSystemData

        if (archive != other.archive) return false
        if (group != other.group) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = archive
        result = 31 * result + group
        result = 31 * result + data.contentHashCode()
        return result
    }

}