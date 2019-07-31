package gg.rsmod.cache.io

/**
 * @author Tom
 */
class ReadWritePacket(private val buffer: ByteArray) {

    private val writer = WriteOnlyPacket(buffer)

    private val reader = ReadOnlyPacket(buffer)

    constructor(capacity: Int) : this(ByteArray(capacity))

    val writerArray: ByteArray
        get() = writer.array

    val readerArray: ByteArray
        get() = writer.array

    val writerPosition: Int
        get() = writer.position

    val readerPosition: Int
        get() = reader.position

    fun reset(): ReadWritePacket {
        writer.reset()
        reader.reset()
        return this
    }

    fun setWriterPosition(position: Int) {
        writer.position = position
    }

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