package gg.rsmod.cache.archive

/**
 * An archive contains [Group]s and their byte data.
 *
 * @param id the archive id.
 * @param groupData a map with group ids and their corresponding array of data
 * for each file inside of them.
 */
data class Archive(
    val id: Int,
    var rawData: ByteArray,
    val groupData: MutableMap<Int, Array<ByteArray>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Archive

        if (id != other.id) return false
        if (!rawData.contentEquals(other.rawData)) return false
        if (groupData != other.groupData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + groupData.hashCode()
        return result
    }
}

/**
 * A group contains [GroupFile]s.
 */
open class Group(
    val id: Int, val crc: Int, val version: Int, val files: Array<out GroupFile>
)

/**
 * A [Group] which was encoded with a hashed name.
 */
class NamedGroup(
    id: Int, crc: Int, version: Int, files: Array<out GroupFile>, val name: Int
) : Group(id, crc, version, files)

/**
 * A file contains data.
 */
open class GroupFile(val id: Int)

/**
 * A [GroupFile] which was encoded with a hashed name.
 */
class NamedGroupFile(
    id: Int, val name: Int
) : GroupFile(id)

/**
 * An [Index] contains metadata for an [Archive].
 */
open class Index(
    val crc: Int,
    val formatType: Int,
    val format: Int,
    val flags: Int,
    val groups: MutableMap<Int, out Group>,
    val rawData: ByteArray
) {

    val hasHashedNames: Boolean
        get() = (NAME_HASH_BIT and flags) != 0

    companion object {
        /**
         * The bit used to identify that the groups inside the archive
         * that this index represents has names that have been hashed.
         */
        const val NAME_HASH_BIT = 0x01

        const val WHIRLPOOL_BIT = 0x02

        const val SIZE_BIT = 0x04
    }
}