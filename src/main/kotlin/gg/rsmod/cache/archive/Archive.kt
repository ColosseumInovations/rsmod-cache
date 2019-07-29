package gg.rsmod.cache.archive

/**
 * An archive contains [Group]s and their byte data.
 */
data class Archive(
    val id: Int, val groupData: MutableMap<Group, Array<ByteArray>>
)

/**
 * A group contains [GroupFile]s.
 */
open class Group(
    val id: Int, val crc: Int, val version: Int, val files: Array<GroupFile>
)

/**
 * A [Group] which was encoded with a hashed name.
 */
class NamedGroup(
    id: Int, crc: Int, version: Int, files: Array<GroupFile>, val name: Int
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
    val format: Int, val flags: Int, val groups: Array<Group>
) {
    companion object {
        const val HASHED_NAME_BIT = 0x01
    }
}