package gg.rsmod.cache.io

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
     * Writes one byte containing the given byte value into this packet
     * in the current position, and then increments the position by one.
     */
    fun p1(value: Int) {
        buffer[position++] = value.toByte()
    }

    /**
     * Writes two bytes containing the given short value into this packet
     * in the current position, and then increments the position by two.
     */
    fun p2(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Writes three bytes containing the given medium value into this packet
     * in the current position, and then increments the position by three.
     */
    fun p3(value: Int) {
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    /**
     * Writes four bytes containing the given int value into this packet
     * in the current position, and then increments the position by four.
     */
    fun p4(value: Int) {
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
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