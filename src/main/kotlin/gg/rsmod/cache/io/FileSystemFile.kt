package gg.rsmod.cache.io

import gg.rsmod.cache.util.FileSystemAccessMode
import java.io.File
import java.io.RandomAccessFile

typealias FileSystemFile = RandomAccessFile

/**
 * Convert the [File] into a [FileSystemFile].
 *
 * @param accessMode the type of [FileSystemAccessMode] that will be used for
 * this file.
 */
internal fun File.toFileSystemFile(
    accessMode: FileSystemAccessMode = FileSystemAccessMode.READ
): FileSystemFile = FileSystemFile(this, accessMode.id)