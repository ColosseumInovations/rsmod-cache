package gg.rsmod.cache.index

/**
 * @author Tom <rspsmods@gmail.com>
 */
internal class Index(val format: Int, val flags: Int, groupCount: Int) {

    val containsNames: Boolean
        get() = (IDENTIFIER_FLAG and flags != 0)

    val groupIds = IntArray(groupCount)

    val groupNames = if (containsNames) IntArray(groupCount) else null

    val groupCrcs = IntArray(groupCount)

    val groupVersions = IntArray(groupCount)

    val groupFileCounts = IntArray(groupCount)

    lateinit var groupFileIds: Array<IntArray>

    lateinit var groupFileNames: Array<IntArray>

    companion object {
        internal const val IDENTIFIER_FLAG = 0x01
        internal const val WHIRPOOL_FLAG = 0x02
        internal const val LENGTH_FLAG = 0x04
        internal const val HASHED_FLAG = 0x08
    }
}