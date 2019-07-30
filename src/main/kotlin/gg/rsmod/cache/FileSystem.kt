package gg.rsmod.cache

import com.github.michaelbull.result.*
import gg.rsmod.cache.archive.*
import gg.rsmod.cache.domain.*
import gg.rsmod.cache.io.FileSystemFile
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.toFileSystemFile
import gg.rsmod.cache.util.Xtea
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.CRC32

/**
 * @author Tom
 */
open class FileSystem(
    internal val indexBlockLength: Int,
    internal val dataBlockLength: Int,
    private val dataFile: FileSystemFile,
    private val masterIndex: Int,
    private val masterIndexFile: FileSystemFile,
    private val indexFiles: Map<Int, FileSystemFile>,
    private val indexes: MutableMap<Int, Index>,
    private val archives: MutableMap<Int, Archive>
) : AutoCloseable {

    fun getIndex(id: Int): Index? = indexes[id]

    fun getArchive(id: Int): Archive? = archives[id]

    fun load(): Result<Any, DomainMessage> = loadMasterIndex().andThen { loadArchives() }

    fun loadMasterIndex(): Result<Any, DomainMessage> =
        MasterIndexCodec.decode(
            dataFile, masterIndexFile, masterIndex,
            indexFiles.keys.toIntArray(), DATA_HEADER_LENGTH,
            indexBlockLength, dataBlockLength
        ).map { indexes.putAll(it) }

    fun loadArchives(): Result<Unit, DomainMessage> {
        if (indexes.isEmpty()) {
            return Err(MasterIndexNotLoaded)
        }
        archives.putAll(indexes.keys.associateWith { Archive(it, mutableMapOf()) })
        return Ok(Unit)
    }

    fun loadGroupFiles(archive: Archive, group: Group, data: ByteArray): Result<Array<ByteArray>, DomainMessage> =
        CompressionCodec.decode(
            ReadOnlyPacket.of(data),
            CRC32(),
            Xtea.EMPTY_KEY_SET,
            0..1_000_000
        ).andThen { decompressedData ->
            val fileCount = group.files.size
            val files: Array<ByteArray>
            if (fileCount == 1) {
                files = arrayOf(decompressedData)
                archive.groupData[group] = files
            } else {
                files = GroupFileCodec.decode(ReadOnlyPacket.of(decompressedData), fileCount)
                archive.groupData[group] = files
            }
            Ok(files)
        }

    fun getGroupData(
        archive: Int, group: Int,
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

    override fun close() {
        dataFile.close()
        masterIndexFile.close()
        indexFiles.values.forEach { it.close() }
    }

    companion object {

        internal val VALID_COMPRESSION_LENGTH = 0..1_000_000

        internal const val DEFAULT_INDEX_FILE_PREFIX = "main_file_cache"

        internal const val DEFAULT_INDEX_FILE_SUFFIX = ".idx"

        internal const val DEFAULT_DATA_FILE_NAME = "main_file_cache.dat2"

        internal const val DEFAULT_MASTER_INDEX = 255

        internal const val DEFAULT_INDEX_BLOCK_LENGTH = 6

        internal const val DEFAULT_DATA_BLOCK_LENGTH = 520

        internal const val DATA_HEADER_LENGTH = 8

        internal const val EXTENDED_DATA_HEADER_LENGTH = 10

        fun of(init: Builder.() -> Unit): Builder {
            val builder = Builder()
            init(builder)
            return builder
        }

        @DslMarker
        annotation class FileSystemDslMarker

        @FileSystemDslMarker
        class Builder {

            lateinit var directory: String

            var indexFilePrefix = DEFAULT_INDEX_FILE_PREFIX

            var indexFileSuffix = DEFAULT_INDEX_FILE_SUFFIX

            var dataFileName = DEFAULT_DATA_FILE_NAME

            var masterIndexId = DEFAULT_MASTER_INDEX

            var indexBlockBytes = DEFAULT_INDEX_BLOCK_LENGTH

            var dataBlockBytes = DEFAULT_DATA_BLOCK_LENGTH

            fun build(): Result<FileSystem, DomainMessage> {
                if (!::directory.isInitialized) {
                    return Err(FileSystemDirectoryNotSet)
                }

                val fileStorePath = Paths.get(directory)

                var dataFile: FileSystemFile? = null
                var masterIdxFile: FileSystemFile? = null
                val idxFiles = mutableMapOf<Int, FileSystemFile>()

                Files.walk(fileStorePath).forEach { path ->
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
                    else -> {
                        val fileSystem = FileSystem(
                            indexBlockBytes, dataBlockBytes, dataFile!!, masterIndexId,
                            masterIdxFile!!, idxFiles, mutableMapOf(), mutableMapOf()
                        )
                        Ok(fileSystem)
                    }
                }
            }
        }
    }
}