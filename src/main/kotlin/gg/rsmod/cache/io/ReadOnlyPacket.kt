package gg.rsmod.cache.io

/**
 * @author Tom
 */
class ReadOnlyPacket(private val buffer: ByteArray) {

    var position = 0

    constructor(capacity: Int) : this(ByteArray(capacity))

    operator fun get(index: Int): Byte = buffer[index]

    val array: ByteArray
        get() = buffer

    val readableBytes: Int
        get() = buffer.size - position

    val isReadable: Boolean
        get() = readableBytes > 0

    fun reset(): ReadOnlyPacket {
        position = 0
        return this
    }

    /**
     * Get next value as a signed byte.
     */
    val g1s: Int
        get() = this[position++].toInt()

    /**
     * Get next value as an unsigned byte.
     */
    val g1: Int
        get() = g1s and 0xFF

    /**
     * Get next values as an unsigned short.
     */
    val g2: Int
        get() = (g1 shl 8) or g1

    /**
     * Get next values as a signed short.
     */
    val g2s: Int
        get() = {
            val value = g2
            if (value > Short.MAX_VALUE) {
                value - 0x10000
            } else {
                value
            }
        }()

    /**
     * Get next values as a medium.
     */
    val g3: Int
        get() = ((g1 shl 16) or (g1 shl 8) or g1)

    /**
     * Get next values as an int.
     */
    val g4: Int
        get() = ((g1 shl 24) or (g1 shl 16) or (g1 shl 8) or g1)

    /**
     * Get next values as a short or an int.
     */
    val gSmart2Or4: Int
        get() = {
            if (this[position] >= 0) {
                g2
            } else {
                g4 and Int.MAX_VALUE
            }
        }()

    /**
     * Get next values as a long.
     */
    val g8: Long
        get() = {
            val l = g4.toLong() and 4294967295L
            val r = g4.toLong() and 4294967295L
            (l shl 32) or r
        }()

    /**
     * Get the next values as a string until terminated.
     */
    val gjstr: String
        get() {
            val builder = StringBuilder(readableBytes)
            while (isReadable) {
                val next = g1
                if (next == 0) {
                    break
                }
                val character: Char =
                    if (next in 128 until 160) {
                        var cleansed = VALID_CHARACTERS[next - 128]
                        if (cleansed == 0.toChar()) {
                            cleansed = '?'
                        }
                        cleansed
                    } else {
                        '?'
                    }
                builder.append(character)
            }
            return builder.toString()
        }

    fun gdata(dst: ByteArray, offset: Int, length: Int) {
        for (i in 0 until length) {
            dst[offset + i] = g1.toByte()
        }
    }

    fun gdata(dst: ByteArray) = gdata(dst, 0, dst.size)

    companion object {

        private val VALID_CHARACTERS = charArrayOf(
            '\u20ac',
            '\u0000',
            '\u201a',
            '\u0192',
            '\u201e',
            '\u2026',
            '\u2020',
            '\u2021',
            '\u02c6',
            '\u2030',
            '\u0160',
            '\u2039',
            '\u0152',
            '\u0000',
            '\u017d',
            '\u0000',
            '\u0000',
            '\u2018',
            '\u2019',
            '\u201c',
            '\u201d',
            '\u2022',
            '\u2013',
            '\u2014',
            '\u02dc',
            '\u2122',
            '\u0161',
            '\u203a',
            '\u0153',
            '\u0000',
            '\u017e',
            '\u0178'
        )

        fun of(data: ByteArray): ReadOnlyPacket = ReadOnlyPacket(data)
    }
}