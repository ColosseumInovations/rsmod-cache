package gg.rsmod.cache.archive

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import gg.rsmod.cache.domain.DecompressionError
import gg.rsmod.cache.domain.DomainMessage
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.util.Compression
import gg.rsmod.cache.util.Xtea
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.zip.CRC32

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressionTests {

    @Test
    fun `encode and decode without xtea cipher`() {
        val key = Xtea.EMPTY_KEY_SET
        val compression = Compression.NONE

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `encode and decode with xtea cipher`() {
        val key = intArrayOf(1, 2, 3, 4)
        val compression = Compression.NONE

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `encode with xtea and decode with incorrect key`() {
        val correctKey = intArrayOf(1, 2, 3, 4)
        val incorrectKey = intArrayOf(1, 2, 4, 3)
        val compression = Compression.NONE

        val compressed = compress(data, correctKey, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, incorrectKey)
        assertNotNull(decompressed.get())
        assertFalse(decompressed.get()!!.contentEquals(data))
    }

    @Test
    fun `bzip2 and unbzip2 without xtea cipher`() {
        val key = Xtea.EMPTY_KEY_SET
        val compression = Compression.BZIP2

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `bzip2 and unbzip2 with xtea cipher`() {
        val key = intArrayOf(1, 2, 3, 4)
        val compression = Compression.BZIP2

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `bzip2 with xtea and unbzip2 with incorrect key`() {
        val correctKey = intArrayOf(1, 2, 3, 4)
        val incorrectKey = intArrayOf(1, 2, 4, 3)
        val compression = Compression.BZIP2

        val compressed = compress(data, correctKey, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, incorrectKey)
        assertEquals(DecompressionError, decompressed.getError())
    }

    @Test
    fun `gzip and ungzip without xtea cipher`() {
        val key = Xtea.EMPTY_KEY_SET
        val compression = Compression.GZIP

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `gzip and ungzip with xtea cipher`() {
        val key = intArrayOf(1, 2, 3, 4)
        val compression = Compression.GZIP

        val compressed = compress(data, key, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, key)
        assertNull(decompressed.getError())
        assert(decompressed.get()?.contentEquals(data) ?: false)
    }

    @Test
    fun `gzip with xtea and ungzip with incorrect key`() {
        val correctKey = intArrayOf(1, 2, 3, 4)
        val incorrectKey = intArrayOf(1, 2, 4, 3)
        val compression = Compression.GZIP

        val compressed = compress(data, correctKey, compression)
        assertNull(compressed.getError())

        val decompressed = decompress(compressed.get()!!, incorrectKey)
        assertEquals(DecompressionError, decompressed.getError())
    }

    companion object {
        private val data = byteArrayOf(
            0, 8, 16, 24, 32, 40, 48, 56, 64,
            72, 80, 88, 96, 104, 112, 120
        )

        private fun compress(data: ByteArray, key: IntArray, compression: Int): Result<ByteArray, DomainMessage> =
            CompressionCodec.encode(
                data,
                compression,
                null,
                key
            )

        private fun decompress(data: ByteArray, key: IntArray): Result<ByteArray, DomainMessage> =
            CompressionCodec.decode(
                ReadOnlyPacket.of(data),
                key,
                1_000_000,
                CRC32()
            )
    }
}