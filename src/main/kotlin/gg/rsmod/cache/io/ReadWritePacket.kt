package gg.rsmod.cache.io

/**
 * @author Tom
 */
internal class ReadWritePacket(private val buffer: ByteArray) {

    private val writer = WriteOnlyPacket(buffer)

    private val reader = ReadOnlyPacket(buffer)

    constructor(capacity: Int) : this(ByteArray(capacity))

    val writerArray: ByteArray
        get() = writer.array

    val readerArray: ByteArray
        get() = writer.array

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
    internal val g1s: Int
        get() = reader.g1s

    /**
     * @see [ReadOnlyPacket.g1]
     */
    internal val g1: Int
        get() = reader.g1

    /**
     * @see [ReadOnlyPacket.g2]
     */
    internal val g2: Int
        get() = reader.g2

    /**
     * @see [ReadOnlyPacket.g2s]
     */
    internal val g2s: Int
        get() = reader.g2s

    /**
     * @see [ReadOnlyPacket.g3]
     */
    internal val g3: Int
        get() = reader.g3

    /**
     * @see [ReadOnlyPacket.g4]
     */
    internal val g4: Int
        get() = reader.g4

    /**
     * @see [ReadOnlyPacket.gSmart2Or4]
     */
    internal val gSmart2Or4: Int
        get() = reader.gSmart2Or4

    /**
     * @see [WriteOnlyPacket.writeByte]
     */
    fun writeByte(value: Int) {
        writer.writeByte(value)
    }

    /**
     * @see [WriteOnlyPacket.writeShort]
     */
    fun writeShort(value: Int) {
        writer.writeShort(value)
    }

    /**
     * @see [WriteOnlyPacket.writeMedium]
     */
    fun writeMedium(value: Int) {
        writer.writeMedium(value)
    }

    /**
     * @see [WriteOnlyPacket.writeInt]
     */
    fun writeInt(value: Int) {
        writer.writeInt(value)
    }

    companion object {

        fun of(data: ByteArray): ReadWritePacket = ReadWritePacket(data)
    }
}