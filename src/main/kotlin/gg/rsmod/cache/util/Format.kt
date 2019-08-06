package gg.rsmod.cache.util

/**
 * Contains information on the type of format that can be used to identify
 * a way of reading values from a part of the file system.
 *
 * @author Tom
 */
internal object Format {

    /**
     * The piece of data does not have a format.
     */
    const val NONE = 5

    /**
     * The piece of data is encoded, and should be decoded, as a short.
     */
    const val SHORT = 6

    /**
     * The piece of data is encoded, and should be decoded, as a 'smart'.
     */
    const val SMART = 7
}