package gg.rsmod.cache.util

import com.github.michaelbull.result.Result
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The different type of compression methods that can be used within the
 * file system.
 *
 * @author Tom
 */
internal object Compression {

    /**
     * The data is not compressed.
     */
    const val NONE = 0

    /**
     * The data is compressed with Bzip2 (https://en.wikipedia.org/wiki/Bzip2).
     */
    const val BZIP2 = 1

    /**
     * The data is compressed with Gzip (https://en.wikipedia.org/wiki/Gzip).
     */
    const val GZIP = 2
}

internal object BZip2 {

    private val HEADER = ByteArray(4).apply {
        this[0] = 'B'.toByte()
        this[1] = 'Z'.toByte()
        this[2] = 'h'.toByte()
        this[3] = '1'.toByte()
    }

    fun compress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ByteArrayInputStream(data).use { input ->
            BZip2CompressorOutputStream(output, 1).use { output ->
                IOUtils.copy(input, output)
            }
        }
        return output.toByteArray()
    }

    fun decompress(data: ByteArray, length: Int): Result<ByteArray, Exception> {
        return Result.of {
            val formatData = ByteArray(length + HEADER.size)
            System.arraycopy(HEADER, 0, formatData, 0, HEADER.size)

            System.arraycopy(data, 0, formatData, HEADER.size, length)

            val output = ByteArrayOutputStream()
            BZip2CompressorInputStream(ByteArrayInputStream(formatData)).use { input ->
                output.use { output ->
                    IOUtils.copy(input, output)
                }
            }
            return@of output.toByteArray()
        }
    }
}

internal object GZip {

    fun compress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ByteArrayInputStream(data).use { input ->
            GZIPOutputStream(output).use { output ->
                IOUtils.copy(input, output)
            }
        }
        return output.toByteArray()
    }

    fun decompress(data: ByteArray, length: Int): Result<ByteArray, Exception> {
        return Result.of {
            val output = ByteArrayOutputStream()
            GZIPInputStream(ByteArrayInputStream(data, 0, length)).use { input ->
                output.use { output ->
                    IOUtils.copy(input, output)
                }
            }
            return@of output.toByteArray()
        }
    }
}