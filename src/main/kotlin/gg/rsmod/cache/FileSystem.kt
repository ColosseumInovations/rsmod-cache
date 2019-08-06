package gg.rsmod.cache

import com.github.michaelbull.result.*
import gg.rsmod.cache.archive.*
import gg.rsmod.cache.domain.*
import gg.rsmod.cache.io.FileSystemFile
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.toFileSystemFile
import gg.rsmod.cache.util.Xtea
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import java.util.zip.CRC32

/**
 * The file system is used as an abstraction layer to read, and/or write,
 * to the game resource files.
 *
 * @param indexBlockLength the length, in bytes, of each data block in an index sector;
 * usually 6 bytes.
 * @param dataBlockLength the length, in bytes, of each data block in a data sector;
 * usually 520 bytes.
 * @param cipheredArchives an array of archive ids that are ciphered through XTEA
 * and require keys in order to be read/written properly.
 * @param masterIndex the id of the 'master index', which contains metadata pointing
 * to the index of each archive.
 * @param dataFile the file that contains all the actual data for each archive, their
 * groups and the corresponding group files.
 * @param indexFiles idx files paired with their corresponding id. This does not include
 * the master index file.
 * @param indexes the virtual [Index] files paired with their corresponding id.
 * @param archives the virtual [Archive] files paired with their corresponding id.
 *
 * @author Tom
 */
open class FileSystem(
    internal val indexBlockLength: Int,
    internal val dataBlockLength: Int,
    private val cipheredArchives: IntArray,
    private val masterIndex: Int,
    private val masterIndexFile: FileSystemFile,
    private val dataFile: FileSystemFile,
    private val indexFiles: Map<Int, FileSystemFile>,
    private val indexes: MutableMap<Int, Index>,
    private val archives: MutableMap<Int, Archive>
) : AutoCloseable {

    /**
     * Load all components needed to read every archive and index
     * in the file system.
     */
    fun loadFully(): Result<Any, DomainMessage> =
        getIndexes()
            .andThen { putIndexes(it) }
            .andThen { getArchives() }
            .andThen { putArchives(it) }
            .andThen { loadGroups() }

    /**
     * Get the available indexes from the master index.
     *
     * @return a [Result] with the success value being a map of the [Index]
     * paired to its corresponding id.
     */
    fun getIndexes(): Result<Map<Int, Index>, DomainMessage> =
        MasterIndexCodec.decode(
            dataFile, masterIndexFile, masterIndex,
            indexFiles.keys.toIntArray(), DATA_HEADER_LENGTH,
            indexBlockLength, dataBlockLength
        )

    /**
     * Get the archives by using the [indexes] as a guide of what
     * archive ids should be available.
     *
     * @return a [Result] with the success value being a map of the newly
     * created [Archive]s paired to their corresponding id.
     */
    fun getArchives(): Result<Map<Int, Archive>, DomainMessage> =
        if (indexes.isEmpty()) {
            Err(MasterIndexNotLoaded)
        } else {
            val archives = indexes.keys.associateWith { Archive(it, mutableMapOf()) }
            Ok(archives)
        }

    /**
     * Load and set the group data corresponding to each archive.
     */
    fun loadGroups(): Result<Unit, DomainMessage> {
        if (indexes.isEmpty()) {
            return Err(MasterIndexNotLoaded)
        } else if (archives.isEmpty()) {
            return Err(ArchivesNotLoaded)
        }

        val idxBuf = ByteArray(indexBlockLength)
        val dataBuf = ByteArray(dataBlockLength)
        val validIndexes = indexes.filterKeys { !cipheredArchives.contains(it) }
        for (entry in validIndexes) {
            val id = entry.key
            val index = entry.value
            val archive = archives[id] ?: return Err(ArchiveDoesNotExist)

            for (group in index.groups.values) {
                val result = loadGroup(archive, group, Xtea.EMPTY_KEY_SET, idxBuf, dataBuf)

                // TODO: starts getting a bit sus here with err...
                //  let's try to improve this
                val err = result.getError()
                if (err != null) {
                    return Err(err)
                }
            }
        }
        return Ok(Unit)
    }

    /**
     * Load and set the data for [group] in [archive].
     */
    fun loadGroup(
        archive: Archive,
        group: Group,
        key: IntArray,
        tmpIdxBuf: ByteArray = ByteArray(indexBlockLength),
        tmpDataBuf: ByteArray = ByteArray(dataBlockLength)
    ): Result<Array<ByteArray>, DomainMessage> =
        getGroupData(archive.id, group.id, tmpIdxBuf, tmpDataBuf)
            .andThen { compressedData ->
                CompressionCodec.decode(
                    ReadOnlyPacket.of(compressedData),
                    key,
                    VALID_COMPRESSION_LENGTH,
                    CRC32()
                )
            }.andThen { decompressedData -> putGroupData(archive, group, decompressedData) }

    /**
     * Get the data block in a group.
     */
    fun getGroupData(
        archive: Int,
        group: Int,
        tmpIdxBuf: ByteArray,
        tmpDataBuf: ByteArray
    ): Result<ByteArray, DomainMessage> {
        val idxFile = indexFiles[archive] ?: return Err(ArchiveDoesNotExist)

        val extended = group > 0xFFFF
        val dataHeaderLength = if (extended) EXTENDED_DATA_HEADER_LENGTH else DATA_HEADER_LENGTH

        return GroupCodec.decode(
            dataFile = dataFile, idxFile = idxFile, archive = archive,
            group = group, extended = extended, idxBlockLength = indexBlockLength,
            dataHeaderLength = dataHeaderLength, dataBlockLength = dataBlockLength,
            tmpIdxBuf = tmpIdxBuf, tmpDataBuf = tmpDataBuf
        )
    }

    /**
     * Put the group data ([decompressedData]) in [archive] paired to
     * [group].
     */
    fun putGroupData(
        archive: Archive,
        group: Group,
        decompressedData: ByteArray
    ): Result<Array<ByteArray>, DomainMessage> {
        val fileCount = group.files.size
        val files: Array<ByteArray>
        if (fileCount == 1) {
            files = arrayOf(decompressedData)
            archive.groupData[group.id] = files
        } else {
            files = GroupFileCodec.decode(ReadOnlyPacket.of(decompressedData), fileCount)
            archive.groupData[group.id] = files
        }
        return Ok(files)
    }

    /**
     * Put the [newIndexes] into the [indexes] map.
     */
    fun putIndexes(newIndexes: Map<Int, Index>): Result<Unit, DomainMessage> =
        if (indexes.isEmpty()) {
            indexes.putAll(newIndexes)
            Ok(Unit)
        } else {
            Err(IndexesAlreadySet)
        }

    /**
     * Put the [newArchives] into the [archives] map.
     */
    fun putArchives(newArchives: Map<Int, Archive>): Result<Unit, DomainMessage> =
        if (archives.isEmpty()) {
            archives.putAll(newArchives)
            Ok(Unit)
        } else {
            Err(ArchivesAlreadySet)
        }

    fun getIndex(id: Int): Index? = indexes[id]

    fun getArchive(id: Int): Archive? = archives[id]

    override fun close() {
        dataFile.close()
        masterIndexFile.close()
        indexFiles.values.forEach { it.close() }
    }

    companion object {

        /**
         * The valid length, in bytes, that we allow for data decompression.
         */
        internal val VALID_COMPRESSION_LENGTH = 0..1_000_000

        /**
         * The default file names for index files in the file system
         * directory.
         */
        internal const val DEFAULT_INDEX_FILE_PREFIX = "main_file_cache"

        /**
         * The default file extension for index files in the file system
         * directory. The files only have to contain this word to be
         * considered as an index file.
         */
        internal const val DEFAULT_INDEX_FILE_SUFFIX = ".idx"

        /**
         * The default file name for the main data file in the file system
         * directory.
         */
        internal const val DEFAULT_DATA_FILE_NAME = "main_file_cache.dat2"

        /**
         * The default index id for the master index file.
         */
        internal const val DEFAULT_MASTER_INDEX = 255

        /**
         * The default length, in bytes, of a single block of data in an index
         * sector.
         */
        internal const val DEFAULT_INDEX_BLOCK_LENGTH = 6

        /**
         * The default length, in bytes, of a single block of data in a data
         * sector.
         */
        internal const val DEFAULT_DATA_BLOCK_LENGTH = 520

        /**
         * The default length, in bytes, of the header of a data sector.
         */
        internal const val DATA_HEADER_LENGTH = 8

        /**
         * The default length, in bytes, of an extended header of a data
         * sector. The difference is usually found in the 'id' of the block
         * being read/written as a short vs an int.
         */
        internal const val EXTENDED_DATA_HEADER_LENGTH = 10

        fun of(init: Builder.() -> Unit): Builder {
            val builder = Builder()
            init(builder)
            return builder
        }

        fun of(fsDirectory: String): Builder = of { directory = fsDirectory }

        @DslMarker
        annotation class FileSystemDslMarker

        @FileSystemDslMarker
        class CipheredArchiveBuilder(private val archives: MutableSet<Int>) {

            /**
             * Add the [archive] as an ciphered archive.
             */
            infix fun set(archive: Int) = archives.add(archive)
        }

        @FileSystemDslMarker
        class Builder {

            /**
             * The system directory that contains the game resource files
             * to be used in the file system.
             */
            lateinit var directory: String

            /**
             * A set of ids that belong to archives that are ciphered
             * with XTEA.
             */
            private var cipheredArchives = mutableSetOf<Int>()

            /**
             * The prefix for idx files in the file system.
             */
            var indexFilePrefix = DEFAULT_INDEX_FILE_PREFIX

            /**
             * The extension for idx files in the file system.
             */
            var indexFileSuffix = DEFAULT_INDEX_FILE_SUFFIX

            /**
             * The name of the main data file in the file system.
             */
            var dataFileName = DEFAULT_DATA_FILE_NAME

            /**
             * The id of the master idx file in the file system.
             */
            var masterIndexId = DEFAULT_MASTER_INDEX

            /**
             * The length, in bytes, of a single index data sector.
             */
            var indexBlockLength = DEFAULT_INDEX_BLOCK_LENGTH

            /**
             * The length, in bytes, of a single data sector.
             */
            var dataBlockLength = DEFAULT_DATA_BLOCK_LENGTH

            /**
             * Construct a [Result] with the success state being a [FileSystem].
             */
            fun build(): Result<FileSystem, DomainMessage> {
                if (!::directory.isInitialized) {
                    return Err(FileSystemDirectoryNotSet)
                }

                val fileStorePath = Paths.get(directory)

                return findFiles(Files.walk(fileStorePath))
                    .andThen { files ->
                        val fileSystem = FileSystem(
                            indexBlockLength, dataBlockLength, cipheredArchives.toIntArray(),
                            masterIndexId, files.masterIdxFile, files.dataFile, files.idxFiles,
                            mutableMapOf(), mutableMapOf()
                        )
                        Ok(fileSystem)
                    }
            }

            /**
             * Construct an [CipheredArchiveBuilder] that specifies which
             * archives the cache enciphers with XTEA and require keys to
             * be deciphered successfully.
             */
            fun cipheredArchives(init: CipheredArchiveBuilder.() -> Unit) {
                init(CipheredArchiveBuilder(cipheredArchives))
            }

            /**
             * Find the files required to start up the file system.
             */
            private fun findFiles(paths: Stream<Path>): Result<FsFiles, DomainMessage> {
                var dataFile: FileSystemFile? = null
                var masterIdxFile: FileSystemFile? = null
                val idxFiles = mutableMapOf<Int, FileSystemFile>()

                paths.forEach { path ->
                    val name = path.fileName.toString()
                    if (name == dataFileName) {
                        dataFile = path.toFile().toFileSystemFile()
                    } else if (name.startsWith(indexFilePrefix) && name.contains(indexFileSuffix)) {
                        val suffixIndex = name.indexOf(indexFileSuffix)
                        val idx = name.substring(suffixIndex + indexFileSuffix.length).toInt()
                        if (idx == masterIndexId) {
                            masterIdxFile = path.toFile().toFileSystemFile()
                        } else {
                            idxFiles[idx] = path.toFile().toFileSystemFile()
                        }
                    }
                }

                return when {
                    masterIdxFile == null -> Err(MasterIndexFileNotFound)
                    dataFile == null -> Err(DataFileNotFound)
                    idxFiles.isEmpty() -> Err(NoIndexFilesFound)
                    else -> Ok(FsFiles(dataFile!!, masterIdxFile!!, idxFiles))
                }
            }

            /**
             * Contains the files required to successfully read, or write,
             * to the file system.
             */
            private data class FsFiles(
                internal val dataFile: FileSystemFile,
                internal val masterIdxFile: FileSystemFile,
                internal val idxFiles: MutableMap<Int, FileSystemFile>
            )
        }
    }
}