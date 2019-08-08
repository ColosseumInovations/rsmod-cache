package gg.rsmod.cache.util

import gg.rsmod.cache.io.WriteOnlyPacket
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XteaTests {

    @Test
    fun `cipher data`() {
        val enciphered = Xtea.encipher(data, 0, data.size, key)

        val decipherIncorrectKey = Xtea.decipher(enciphered.copyOf(), intArrayOf(1, 2, 4, 3))
        assert(!decipherIncorrectKey.contentEquals(data))

        val decipherCorrectKey = Xtea.decipher(enciphered.copyOf(), key)
        assert(decipherCorrectKey.contentEquals(data))
    }

    companion object {

        private val data = WriteOnlyPacket(capacity = Int.SIZE_BYTES * 10).apply {
            for (i in 0 until capacity / Int.SIZE_BYTES) {
                p4(i * 8)
            }
        }.array

        private val key = intArrayOf(
            1, 2, 3, 4
        )
    }
}