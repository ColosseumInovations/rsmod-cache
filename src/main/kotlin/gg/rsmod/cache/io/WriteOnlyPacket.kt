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

    fun p1(value: Int) {
        buffer[position++] = value.toByte()
    }

    fun p2(value: Int) {
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun p3(value: Int) {
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun p4(value: Int) {
        buffer[position++] = (value shr 24).toByte()
        buffer[position++] = (value shr 16).toByte()
        buffer[position++] = (value shr 8).toByte()
        buffer[position++] = value.toByte()
    }

    fun pdata(src: ByteArray, srcOffset: Int, length: Int) {
        for (i in 0 until length) {
            p1(src[srcOffset + i].toInt())
        }
    }

    fun pdata(src: ByteArray) = pdata(src, 0, src.size)

    companion object {

        fun of(data: ByteArray): WriteOnlyPacket = WriteOnlyPacket(data)
    }
}