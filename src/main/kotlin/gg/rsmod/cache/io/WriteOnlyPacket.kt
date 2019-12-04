package gg.rsmod.cache.io

import gg.rsmod.cache.util.CharacterUtil

/**
 * A write-only packet is a buffer containing an array of bytes and a pointer
 * of the last position in said array that it has written data to.
 *
 * This buffer can only be used to write data and not read from it.
 * If you need to read from a buffer, consider using [ReadOnlyPacket]
 * or [ReadWritePacket].
 *
 * @author Tom
 */
class WriteOnlyPacket(private val buffer: ByteArray) {

    /**
     * The last position in [array] that was accessed.
     */
    var position = 0

    /**
     * Create a [WriteOnlyPacket] with a backing-array the size of [capacity].
     */
    constructor(capacity: Int) : this(ByteArray(capacity))

    /**
     * Get the [Byte] located in position [index] on the backing array.
     */
    operator fun get(index: Int): Byte = buffer[index]

    /**
     * The backing array for this packet.
     */
    val array: ByteArray
        get() = buffer

    /**
     * The capacity of this packet.
     */
    val capacity: Int
        get() = array.size

    /**
     * The amount of bytes that can be written to this packet taking the
     * current [position] into account.
     */
    val writableBytes: Int
        get() = buffer.size - position

    /**
     * Check if this packet has any more [writableBytes].
     */
    val isWritable: Boolean
        get() = writableBytes > 0

    /**
     * Reset the write position of this packet.
     */
    fun reset(): WriteOnlyPacket {
        position = 0
        return this
    }

    /**
     * Set [value] in our buffer and increment our [position] by one.
     */
    fun p1(value: Int) {
        buffer[position++] = value.toByte()
    }

    /**
     * Set [value] in our buffer as [value] plus 128, and increment
     * our [position] by one.
     */
    fun p1_altA(value: Int) {
        buffer[position++] = (value + 128).toByte()
    }

    /**
     * Set [value] in our buffer as negated [value], and increment
     * our [position] by one.
     */
    fun p1_altC(value: Int) {
        buffer[position++] = (0 - value).toByte()
    }

    /**
     * Set [value] in our buffer as 128 minus [value], and increment
     * our [position] by one.
     */
    fun p1_altS(value: Int) {
        buffer[position++] = (128 - value).toByte()
    }

    /**
     * Set [value] in our buffer as two bytes, and increment
     * our [position] by one.
     */
    fun p2(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Set [value] in our buffer as two bytes, the second byte being set
     * to [value] plus 128, and increment our [position] by two.
     */
    fun p2_altA(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = (value + 128).toByte()
    }

    /**
     * Set [value] in our buffer as two bytes in little-endian order,
     * and increment our [position] by two.
     */
    fun p2LE(value: Int) {
        buffer[position++] = value.toByte()
        buffer[position++] = (value shr 8).toByte()
    }

    /**
     * Set [value] in our buffer as two bytes in little-endian order,
     * the second byte being set to [value] plus 128, and increment
     * our [position] by two.
     */
    fun p2LE_altA(value: Int) {
        buffer[position++] = (value + 128).toByte()
        buffer[position++] = (value shr 8).toByte()
    }

    /**
     * Set [value] in our buffer as three bytes, and increment
     * our [position] by three.
     */
    fun p3(value: Int) {
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Set [value] in our buffer as four bytes, and increment
     * our [position] by four.
     */
    fun p4(value: Int) {
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Set [value] in our buffer as four bytes in little-endian order,
     * and increment our [position] by four.
     */
    fun p4_alt1(value: Int) {
        buffer[position++] = value.toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 24).toByte()
    }

    /**
     * Set [value] in our buffer as four bytes in "v1" or "middle"
     * order, and increment our [position] by four.
     */
    fun p4_alt2(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
    }

    /**
     * Set [value] in our buffer as four bytes in "v2" or "inverse middle"
     * order, and increment our [position] by four.
     */
    fun p4_alt3(value: Int) {
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = value.toByte()
        buffer[position++] = (value shr 8).toByte()
    }

    /**
     * Set [value] in our buffer as five bytes, and increment
     * our [position] by five.
     */
    fun p5(value: Long) {
        buffer[position++] = (value shr 32).toByte()
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Set [value] in our buffer as eight bytes, and increment
     * our [position] by eight.
     */
    fun p8(value: Long) {
        buffer[position++] = (value shr 56).toByte()
        buffer[position++] = (value shr 48).toByte()
        buffer[position++] = (value shr 40).toByte()
        buffer[position++] = (value shr 32).toByte()
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Writes a byte for each character in the given string value in
     * the current position and terminates the string with a 0 written
     * as a byte, all while incrementing the position on each write by
     * one.
     */
    fun pjstr(value: String) {
        value.forEach { character ->
            val char = if (character.toInt() == 63) {
                128
            } else {
                CharacterUtil.VALID_CHARACTERS.indexOf(character) + 128
            }
            p1(char)
        }
        p1(0)
    }

    /**
     * Set [value] in our buffer as one or two bytes depending on the
     * [value], and increment our [position] by one or two.
     */
    fun psmart1or2(value: Int) {
        if (value <= Byte.MAX_VALUE) {
            p1(value)
        } else {
            p2(value)
        }
    }

    /**
     * Set [value] in our buffer as two or four bytes depending on the
     * [value], and increment our [position] by two or four.
     */
    fun psmart2or4(value: Int) {
        if (value <= Short.MAX_VALUE) {
            p2(value)
        } else {
            p4(value)
        }
    }

    /**
     * Transfers the data from the specified source data into this packet
     * starting from the current [position], reading the data until the amount
     * of bytes transferred equals [length].
     *
     * If [src] length is less than [length] - [srcOffset], an [ArrayIndexOutOfBoundsException]
     * will be thrown.
     *
     * @param src the array of bytes to transfer into this packet.
     * @param srcOffset the offset to begin reading from the source data.
     * @param length the amount of bytes to transfer from the source data.
     */
    fun pdata(src: ByteArray, srcOffset: Int, length: Int) {
        for (i in 0 until length) {
            p1(src[srcOffset + i].toInt())
        }
    }

    /**
     * Transfers the data from the specified source data into this packet
     * starting from the current [position], fully reading the data until
     * the amount of bytes transferred equals to the size of the source
     * data.
     *
     * @param src the array of bytes to transfer into this packet.
     */
    fun pdata(src: ByteArray) = pdata(src, 0, src.size)

    companion object {

        /**
         * Create a [WriteOnlyPacket] with [data] as its backing array.
         */
        fun of(data: ByteArray): WriteOnlyPacket = WriteOnlyPacket(data)
    }
}