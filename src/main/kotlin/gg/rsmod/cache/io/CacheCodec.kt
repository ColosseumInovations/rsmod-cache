package gg.rsmod.cache.io

/**
 * @author Tom <rspsmods@gmail.com>
 */
internal interface CacheCodec<T> {

    fun decode(buf: CacheBuf): T

    fun encode(buf: CacheBuf, value: T)
}