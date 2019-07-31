package gg.rsmod.cache.domain

sealed class DomainMessage

/**
 * The directory for the [gg.rsmod.cache.FileSystem] was not set.
 */
object FileSystemDirectoryNotSet : DomainMessage()

/**
 * The main data file was not found in the file system directory.
 */
object DataFileNotFound : DomainMessage()

/**
 * The Master Index file was not found in the file system directory.
 */
object MasterIndexFileNotFound : DomainMessage()

/**
 * No index files were found in the file system directory.
 */
object NoIndexFilesFound : DomainMessage()

/**
 * The master index has not been loaded and archives cannot be loaded onto
 * memory.
 */
object MasterIndexNotLoaded : DomainMessage()

/**
 * The archives have not been loaded.
 */
object ArchivesNotLoaded : DomainMessage()

/**
 * The archive being read from a data block does not match the archive
 * specified by the user.
 */
object DataArchiveMismatch : DomainMessage()

/**
 * The group being read from a data block does not match the group
 * specified by the user.
 */
object DataGroupMismatch : DomainMessage()

/**
 * The block being read from a data block is not accurate to how many blocks
 * have been read from a data block so far.
 */
object DataBlockMismatch : DomainMessage()

/**
 * The length of a compressed block of data was out of range of the given
 * bounds.
 */
object CompressedLengthOutOfBounds : DomainMessage()

/**
 * The given compression type is invalid.
 */
object IllegalCompressionType : DomainMessage()

/**
 * The data being decompressed has an illegal version value.
 */
object IllegalVersion : DomainMessage()

/**
 * The compressed length read from the data does not match the length
 * of the data that was actually decompressed.
 */
object CompressionLengthMismatch : DomainMessage()

/**
 * The index file for an archive was read, but the amount of bytes read differed
 * from the amount of bytes specified per index data block.
 */
object MalformedIndexRead : DomainMessage()

/**
 * The archive, or its index, being requested does not exist.
 */
object ArchiveDoesNotExist : DomainMessage()

/**
 * The length of a data block that was read from an index file was an illegal
 * value.
 */
object DataBlockPointerInvalidLength : DomainMessage()

/**
 * The byte offset of a data block that was read from an index file was an
 * illegal value.
 */
object DataBlockPointerInvalidOffset : DomainMessage()

/**
 * The data block was not read to the extent it was asked.
 */
object DataBlockReadMalformation : DomainMessage()