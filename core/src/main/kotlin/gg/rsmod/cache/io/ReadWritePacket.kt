package gg.rsmod.cache.io

/**
 * A read-write packet is a buffer containing an array of bytes and two distinct
 * pointers to the backing array; one for reading and one for writing.
 *
 * @author Tom
 */
class ReadWritePacket(buffer: ByteArray) {

    /**
     * The [WriteOnlyPacket] used to write to this packet.
     */
    private val writer = WriteOnlyPacket(buffer)

    /**
     * The [ReadOnlyPacket] used to read from this packet.
     */
    private val reader = ReadOnlyPacket(buffer)

    /**
     * Create a [ReadWritePacket] with a backing-array the size of [capacity].
     */
    constructor(capacity: Int) : this(ByteArray(capacity))

    /**
     * Alias for this packet's writer [WriteOnlyPacket.array].
     */
    val writerArray: ByteArray
        get() = writer.array

    /**
     * Alias for this packet's writer [WriteOnlyPacket.position].
     */
    val writerPosition: Int
        get() = writer.position

    /**
     * Alias for this packet's reader [ReadOnlyPacket.array].
     */
    val readerArray: ByteArray
        get() = writer.array

    /**
     * Alias for this packet's reader [ReadOnlyPacket.position].
     */
    val readerPosition: Int
        get() = reader.position

    /**
     * Reset the position for both the reader and writer packet.
     */
    fun reset(): ReadWritePacket {
        writer.reset()
        reader.reset()
        return this
    }

    /**
     * Alias for this packet's writer [WriteOnlyPacket.position]
     * setter.
     */
    fun setWriterPosition(position: Int) {
        writer.position = position
    }

    /**
     * Alias for this packet's reader [ReadOnlyPacket.position]
     * setter.
     */
    fun setReaderPosition(position: Int) {
        reader.position = position
    }

    /**
     * @see [ReadOnlyPacket.g1s]
     */
    val g1s: Int
        get() = reader.g1s

    /**
     * @see [ReadOnlyPacket.g1]
     */
    val g1: Int
        get() = reader.g1

    /**
     * @see [ReadOnlyPacket.g2]
     */
    val g2: Int
        get() = reader.g2

    /**
     * @see [ReadOnlyPacket.g2s]
     */
    val g2s: Int
        get() = reader.g2s

    /**
     * @see [ReadOnlyPacket.g3]
     */
    val g3: Int
        get() = reader.g3

    /**
     * @see [ReadOnlyPacket.g4]
     */
    val g4: Int
        get() = reader.g4

    /**
     * @see [ReadOnlyPacket.gSmart2Or4]
     */
    val gSmart2Or4: Int
        get() = reader.gSmart2Or4

    /**
     * @see [ReadOnlyPacket.g8]
     */
    val g8: Long
        get() = reader.g8

    /**
     * @see [ReadOnlyPacket.gjstr]
     */
    val gjstr: String
        get() = reader.gjstr

    /**
     * @see [ReadOnlyPacket.gdata]
     */
    fun gdata(dst: ByteArray, offset: Int, length: Int) = reader.gdata(dst, offset, length)

    /**
     * @see [ReadOnlyPacket.gdata]
     */
    fun gdata(dst: ByteArray) = reader.gdata(dst)

    /**
     * @see [WriteOnlyPacket.p1]
     */
    fun p1(value: Int) = writer.p1(value)

    /**
     * @see [WriteOnlyPacket.p2]
     */
    fun p2(value: Int) = writer.p2(value)

    /**
     * @see [WriteOnlyPacket.p3]
     */
    fun p3(value: Int) = writer.p3(value)

    /**
     * @see [WriteOnlyPacket.p4]
     */
    fun p4(value: Int) = writer.p4(value)

    /**
     * @see [WriteOnlyPacket.pdata]
     */
    fun pdata(src: ByteArray, srcOffset: Int, length: Int) = writer.pdata(src, srcOffset, length)

    /**
     * @see [WriteOnlyPacket.pdata]
     */
    fun pdata(src: ByteArray) = writer.pdata(src)

    companion object {

        /**
         * Create a [ReadWritePacket] with [data] as its data for both
         * the backing [WriteOnlyPacket] and [ReadOnlyPacket].
         */
        fun of(data: ByteArray): ReadWritePacket = ReadWritePacket(data)
    }
}