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
    val writer = WriteOnlyPacket(buffer)

    /**
     * The [ReadOnlyPacket] used to read from this packet.
     */
    val reader = ReadOnlyPacket(buffer)

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
     * Alias for this packet's reader [ReadOnlyPacket.array].
     */
    val readerArray: ByteArray
        get() = writer.array

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
    val g1s = reader.g1s

    /**
     * @see [ReadOnlyPacket.g1s_altA]
     */
    val g1s_altA = reader.g1s_altA

    /**
     * @see [ReadOnlyPacket.g1s_altC]
     */
    val g1s_altC = reader.g1s_altC

    /**
     * @see [ReadOnlyPacket.g1s_altS]
     */
    val g1s_altS = reader.g1s_altS

    /**
     * @see [ReadOnlyPacket.g1]
     */
    val g1 = reader.g1

    /**
     * @see [ReadOnlyPacket.g1_altA]
     */
    val g1_altA = reader.g1_altA

    /**
     * @see [ReadOnlyPacket.g1_altC]
     */
    val g1_altC = reader.g1_altC

    /**
     * @see [ReadOnlyPacket.g1_altS]
     */
    val g1_altS = reader.g1_altS

    /**
     * @see [ReadOnlyPacket.g2s]
     */
    val g2s = reader.g2s

    /**
     * @see [ReadOnlyPacket.g2sLE]
     */
    val g2sLE = reader.g2sLE

    /**
     * @see [ReadOnlyPacket.g2]
     */
    val g2 = reader.g2

    /**
     * @see [ReadOnlyPacket.g2_altA]
     */
    val g2_altA = reader.g2_altA

    /**
     * @see [ReadOnlyPacket.g2LE]
     */
    val g2LE = reader.g2LE

    /**
     * @see [ReadOnlyPacket.g2LE_altA]
     */
    val g2LE_altA = reader.g2LE_altA

    /**
     * @see [ReadOnlyPacket.g3s]
     */
    val g3s = reader.g3s

    /**
     * @see [ReadOnlyPacket.g3]
     */
    val g3 = reader.g3

    /**
     * @see [ReadOnlyPacket.g4]
     */
    val g4 = reader.g4

    /**
     * @see [ReadOnlyPacket.g4_alt1]
     */
    val g4_alt1 = reader.g4_alt1

    /**
     * @see [ReadOnlyPacket.g4_alt2]
     */
    val g4_alt2 = reader.g4_alt2

    /**
     * @see [ReadOnlyPacket.g4_alt3]
     */
    val g4_alt3 = reader.g4_alt3

    /**
     * @see [ReadOnlyPacket.gsmart1or2]
     */
    val gsmart1or2 = reader.gsmart1or2

    /**
     * @see [ReadOnlyPacket.gsmart2or4]
     */
    val gsmart2or4 = reader.gsmart2or4

    /**
     * @see [ReadOnlyPacket.g8]
     */
    val g8 = reader.g8

    /**
     * @see [ReadOnlyPacket.gfloat]
     */
    val gfloat = reader.gfloat

    /**
     * @see [ReadOnlyPacket.gjstr]
     */
    val gjstr = reader.gjstr

    /**
     * @see [ReadOnlyPacket.gdata]
     */
    fun gdata(dst: ByteArray, position: Int, length: Int) = reader.gdata(dst, position, length)

    /**
     * @see [ReadOnlyPacket.gdata]
     */
    fun gdata(dst: ByteArray) = reader.gdata(dst)

    /**
     * @see [ReadOnlyPacket.gdataLE_altA]
     */
    fun gdataLE_altA(dst: ByteArray, position: Int, length: Int) = reader.gdataLE_altA(dst, position, length)

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