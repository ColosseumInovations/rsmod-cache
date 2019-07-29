package gg.rsmod.cache.io

/**
 * @author Tom
 */
internal class WriteOnlyPacket(private val buffer: ByteArray) {

    var position = 0

    val array: ByteArray
        get() = buffer

    fun reset(): WriteOnlyPacket {
        position = 0
        return this
    }

    fun writeByte(value: Int) {
        buffer[position++] = value.toByte()
    }

    fun writeShort(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun writeMedium(value: Int) {
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun writeInt(value: Int) {
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun writeBytes(src: ByteArray, srcOffset: Int, length: Int) {
        for (i in 0 until length) {
            writeByte(src[srcOffset + i].toInt())
        }
    }

    fun writeBytes(src: ByteArray) = writeBytes(src, 0, src.size)

    companion object {

        fun of(data: ByteArray): WriteOnlyPacket = WriteOnlyPacket(data)
    }
}