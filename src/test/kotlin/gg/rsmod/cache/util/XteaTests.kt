package gg.rsmod.cache.util

import gg.rsmod.cache.io.WriteOnlyPacket
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XteaTests {

    @Test
    fun `cipher data`() {
        val dataWriter = WriteOnlyPacket(capacity = Int.SIZE_BYTES * 10)
        for (i in 0 until dataWriter.array.size / Int.SIZE_BYTES) {
            dataWriter.p4(i * 8)
        }

        val data = dataWriter.array
        val enciphered = Xtea.encipher(data, 0, data.size, key)

        val decipherIncorrectKey = Xtea.decipher(enciphered, intArrayOf(1, 2, 4, 3))
        assert(!decipherIncorrectKey.contentEquals(data))

        val decipherCorrectKey = Xtea.decipher(enciphered, key)
        assert(decipherCorrectKey.contentEquals(data))
    }

    companion object {
        private val key = intArrayOf(
            1, 2, 3, 4
        )
    }
}