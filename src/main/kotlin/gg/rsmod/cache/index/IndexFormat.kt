package gg.rsmod.cache.index

/**
 * @author Tom <rspsmods@gmail.com>
 */
internal enum class IndexFormat(val opcode: Int) {
    NONE(5),
    SHORT(6),
    SMART(7);

    companion object {
        val values = enumValues<IndexFormat>()
    }
}