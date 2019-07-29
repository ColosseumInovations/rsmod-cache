package gg.rsmod.cache.util

/**
 * The type of access-mode we want a [gg.rsmod.cache.io.FileSystemFile]
 * to have.
 *
 * @author Tom
 */
internal enum class FileSystemAccessMode(val id: String) {
    READ("r"),
    WRITE("w"),
    READ_WRITE("rw")
}