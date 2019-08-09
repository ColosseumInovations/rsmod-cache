package gg.rsmod.cache.domain

sealed class DomainMessage(private val reason: String) {
    override fun toString(): String = reason
}

/**
 * The packet can't store any more data due to the capacity it was constructed
 * with.
 */
object PacketOverflow
    : DomainMessage("The packet cannot store any more data due to its initial capacity")

/**
 * The packet can't read any more data due to the capacity it was constructed
 * with.
 */
object PacketNotEnoughData
    : DomainMessage("The packet does not have any more data to read")

/**
 * The directory for the [gg.rsmod.cache.FileSystem] was not set.
 */
object FileSystemDirectoryNotSet : DomainMessage("File system directory was not set")

/**
 * The main data file was not found in the file system directory.
 */
object DataFileNotFound
    : DomainMessage("Data file was not found in the file system directory")

/**
 * The Master Index file was not found in the file system directory.
 */
object MasterIndexFileNotFound
    : DomainMessage("Master index file was not found in the file system directory")

/**
 * No index files were found in the file system directory.
 */
object NoIndexFilesFound
    : DomainMessage("No idx files were not found in the file system directory")

/**
 * The master index has not been loaded and archives cannot be loaded onto
 * memory.
 */
object MasterIndexNotLoaded
    : DomainMessage("Master index has not been initialised")

/**
 * The archives have not been loaded.
 */
object ArchivesNotLoaded : DomainMessage("Archives have not been initialised")

/**
 * The index files have already been set on the file system.
 */
object IndexesAlreadySet : DomainMessage("Indexes have already been set")

/**
 * The archive files have already been set on the file system.
 */
object ArchivesAlreadySet : DomainMessage("Archives have already been set.")

/**
 * The archive being read from a data block does not match the archive
 * specified by the user.
 */
object DataArchiveMismatch
    : DomainMessage("Data block contains the incorrect archive id")

/**
 * The group being read from a data block does not match the group
 * specified by the user.
 */
object DataGroupMismatch
    : DomainMessage("Data block contains the incorrect group id")

/**
 * The block being read from a data block is not accurate to how many blocks
 * have been read from a data block so far.
 */
object DataBlockMismatch
    : DomainMessage("Data block index does not match the current iteration")

/**
 * The length of a compressed block of data was out of range of the given
 * bounds.
 */
object CompressedLengthOutOfBounds
    : DomainMessage("Packed compression length is out of bounds")

/**
 * The input streams threw an exception when trying to decompress data.
 */
object DecompressionError
    : DomainMessage("There was an exception when decompressing the data (most likely incorrect XTEA key)")

/**
 * The given compression type is invalid.
 */
object IllegalCompressionType : DomainMessage("Packed compression type is invalid")

/**
 * The data being decompressed has an illegal version value.
 */
object IllegalVersion : DomainMessage("Packed compressed version is invalid")

/**
 * The compressed length read from the data does not match the length
 * of the data that was actually decompressed.
 */
object CompressionLengthMismatch
    : DomainMessage("Packed decompression length mismatch with decompressed data length")

/**
 * The index file for an archive was read, but the amount of bytes read differed
 * from the amount of bytes specified per index data block.
 */
object MalformedIndexRead : DomainMessage("Unexpected end of file for index")

/**
 * The archive, or its index, being requested does not exist.
 */
object ArchiveDoesNotExist : DomainMessage("Archive or its index does not exist")

/**
 * The group does not exist in the archive.
 */
object GroupDoesNotExist : DomainMessage("Group does not exist")

/**
 * The length of a data block that was read from an index file was an illegal
 * value.
 */
object DataBlockPointerInvalidLength : DomainMessage("Invalid packed length in index")

/**
 * The byte offset of a data block that was read from an index file was an
 * illegal value.
 */
object DataBlockPointerInvalidOffset : DomainMessage("Invalid packed offset in index")

/**
 * The data block was not read to the extent it was asked.
 */
object DataBlockReadMalformation : DomainMessage("Data block could not be fully read")