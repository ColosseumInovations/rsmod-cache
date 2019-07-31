package gg.rsmod.cache.io

/**
 * @author Tom
 */
class WriteOnlyPacket(private val buffer: ByteArray) {

    var position = 0

    val array: ByteArray
        get() = buffer

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