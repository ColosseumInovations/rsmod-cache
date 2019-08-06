package gg.rsmod.cache.io

import java.io.File
import java.io.RandomAccessFile

/**
 * An alias for [RandomAccessFile].
 */
typealias FileSystemFile = RandomAccessFile

/**
 * Convert the [File] into a [FileSystemFile].
 *
 * @param accessType the type of [FileSystemAccessType] that will be
 * used for this file.
 */
internal fun File.toFileSystemFile(
    accessType: FileSystemAccessType = FileSystemAccessType.Read
): FileSystemFile = FileSystemFile(this, accessType.mode)

/**
 * The type of file access for a [FileSystemFile].
 *
 * @author Tom
 */
internal sealed class FileSystemAccessType(val mode: String) {

    /**
     * The file system will have read-only access.
     */
    object Read : FileSystemAccessType("r")

    /**
     * The file system will have write-only access.
     */
    object Write : FileSystemAccessType("w")

    /**
     * The file system will have read-write access.
     */
    object ReadWrite : FileSystemAccessType("rw")
}