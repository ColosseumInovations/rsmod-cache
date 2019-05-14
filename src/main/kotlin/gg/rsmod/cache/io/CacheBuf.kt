package gg.rsmod.cache.io

import gg.rsmod.cache.index.IndexFormat
import java.nio.ByteBuffer

typealias CacheBuf = ByteBuffer

internal fun CacheBuf.readUnsignedByte() = get().toInt() and 0xFF

internal fun CacheBuf.readUnsignedShort() = getShort(position()).toInt() and 0xFF

internal fun CacheBuf.readInt() = getInt(position())

internal fun CacheBuf.readLargeSmart(): Int = if (get(position()) >= 0) {
    readUnsignedShort()
} else {
    readInt()
}

internal fun CacheBuf.readFormatSmart(format: IndexFormat): Int = if (format == IndexFormat.SMART) {
    readLargeSmart()
} else {
    readUnsignedShort()
}