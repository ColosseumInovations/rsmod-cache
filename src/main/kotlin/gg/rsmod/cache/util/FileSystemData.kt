package gg.rsmod.cache.util

/**
 * Represents metadata that points to a block of data that you want to read.
 * Can be thought of as a memory address, except it also keeps track of the
 * length of said memory.
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
 * Represents a single block of data that is limited by the data byte length
 * specified in [gg.rsmod.cache.FileSystem.dataBlockLength]. If the collective
 * data that has to be read exceeds said length, the data will read more than one
 * of this type of [FileSystemDataBlock] until exhausted.
 *
 * @param archive the archive id that this data block was initially encoded into.
 * @param group the group id that this data block was initially encoded into.
 * @param currBlockIndex the current 'index' this data block was encoded to.
 * if the total byte data did not exceed [gg.rsmod.cache.FileSystem.dataBlockLength]
 * then this value will always be 0, otherwise it will be equal to its
 * iteration index when all of the data was being encoded.
 * @param nextBlock the block offset where the next block of data was encoded
 * to.
 *
 * @author Tom
 */
internal data class FileSystemDataBlock(
    val archive: Int, val group: Int, val currBlockIndex: Int, val nextBlock: Int
)

/**
 * Represents a big blob of data inside a [gg.rsmod.cache.archive.Group].
 *
 * @param archive the archive id that this blob belongs to.
 * @param group the group id that this blob belongs to.
 * @param data all of the data this blob represents.
 *
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