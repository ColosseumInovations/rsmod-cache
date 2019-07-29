package gg.rsmod.cache.util

import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

object Xtea {

    val EMPTY_KEY_SET = intArrayOf(0, 0, 0, 0)

    /**
     * The golden ratio XTEA uses.
     */
    private const val GOLDEN_RATIO = -1640531527

    /**
     * The number of rounds XTEA uses.
     */
    private const val ROUNDS = 32

    fun decrypt(data: ByteArray, keys: IntArray): ByteArray {
        if (keys.contentEquals(EMPTY_KEY_SET)) {
            return data
        }
        return decode(keys, data, 0, data.size)
    }

    fun decode(key: IntArray, data: ByteArray, start: Int, end: Int): ByteArray {
        val numBlocks = (end - start) / 8

        val buffer = ByteBuffer.wrap(data)
        buffer.position(start)

        for (i in 0 until numBlocks) {
            var y = buffer.int
            var z = buffer.int
            @Suppress("INTEGER_OVERFLOW")
            var sum = GOLDEN_RATIO * ROUNDS
            val delta = GOLDEN_RATIO
            for (j in ROUNDS downTo 1) {
                z -= (y.ushr(5) xor (y shl 4)) + y xor sum + key[sum.ushr(11) and 0x56c00003]
                sum -= delta
                y -= (z.ushr(5) xor (z shl 4)) - -z xor sum + key[sum and 0x3]
            }
            buffer.position(buffer.position() - 8)
            buffer.putInt(y)
            buffer.putInt(z)
        }
        return buffer.array()
    }

    fun encode(buffer: ByteBuf, start: Int, end: Int, key: IntArray) {
        val numQuads = (end - start) / 8
        for (i in 0 until numQuads) {
            var sum = 0
            var v0 = buffer.getInt(start + i * 8)
            var v1 = buffer.getInt(start + i * 8 + 4)
            for (j in 0 until ROUNDS) {
                v0 += (v1 shl 4 xor v1.ushr(5)) + v1 xor sum + key[sum and 3]
                sum += GOLDEN_RATIO
                v1 += (v0 shl 4 xor v0.ushr(5)) + v0 xor sum + key[sum.ushr(11) and 3]
            }
            buffer.setInt(start + i * 8, v0)
            buffer.setInt(start + i * 8 + 4, v1)
        }
    }
}