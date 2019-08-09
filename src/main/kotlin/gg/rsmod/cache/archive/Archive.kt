package gg.rsmod.cache.archive

/**
 * An archive contains [Group]s and their byte data.
 *
 * @param id the archive id.
 * @param groupData a map with group ids and their corresponding array of data
 * for each file inside of them.
 */
data class Archive(
    val id: Int, val groupData: MutableMap<Int, Array<ByteArray>>
)

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
    val formatType: Int, val format: Int, val flags: Int, val groups: MutableMap<Int, out Group>
) {

    val hasHashedNames: Boolean
        get() = (HASHED_NAME_BIT and flags) != 0

    companion object {
        /**
         * The bit used to identify that the groups inside the archive
         * that this index represents has names that have been hashed.
         */
        const val HASHED_NAME_BIT = 0x01
    }
}