package gg.rsmod.cache.archive

import com.github.michaelbull.result.getError
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.WriteOnlyPacket
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndexTests {

    @Test
    fun `encode and decode without names`() {
        val files = arrayOf(
            arrayOf(GroupFile(0), GroupFile(1), GroupFile(2)),
            arrayOf(GroupFile(0), GroupFile(1), GroupFile(2), GroupFile(3))
        )
        val groups: Array<Group> = files.mapIndexed { index, it -> Group(id = index, crc = 0, version = 0, files = it) }.toTypedArray()
        val index = Index(crc = 0, formatType = 0, format = 0, flags = 0, groups = groups.associateBy { it.id } as MutableMap<Int, Group>)

        val data = ByteArray(1024)

        val encode = IndexCodec.encode(WriteOnlyPacket.of(data), index)
        assertNull(encode.getError())

        val decode = IndexCodec.decode(packet = ReadOnlyPacket.of(data), crc = 0)
        assertSame(index.formatType, decode.formatType)
        assertSame(index.format, decode.format)
        assertSame(index.flags, decode.flags)

        index.groups.values.forEachIndexed { i, group ->
            val otherGroup = decode.groups[i]
            assertNotNull(otherGroup)
            assertSame(group.id, (otherGroup?.id ?: Unit))

            group.files.forEachIndexed { j, file ->
                val otherFile = decode.groups[i]?.files?.get(j)
                assertNotNull(otherFile)
                assertSame(file.id, (otherFile?.id ?: Unit))
            }
        }
    }

    @Test
    fun `encode and decode with names`() {
        val files = arrayOf(
            arrayOf(NamedGroupFile(0, 10), NamedGroupFile(1, 10), NamedGroupFile(2, 10)),
            arrayOf(NamedGroupFile(0, 10), NamedGroupFile(1, 10), NamedGroupFile(2, 10), NamedGroupFile(3, 10))
        )
        val groups: Array<NamedGroup> = files.mapIndexed { index, it -> NamedGroup(id = index, crc = 0, version = 0, files = it, name = 10) }.toTypedArray()
        val index = Index(crc = 0, formatType = 0, format = 0, flags = 1, groups = groups.associateBy { it.id } as MutableMap<Int, NamedGroup>)

        val data = ByteArray(1024)

        val encode = IndexCodec.encode(WriteOnlyPacket.of(data), index)
        assertNull(encode.getError())

        val decode = IndexCodec.decode(ReadOnlyPacket.of(data), crc = 0)
        assertSame(index.formatType, decode.formatType)
        assertSame(index.format, decode.format)
        assertSame(index.flags, decode.flags)

        index.groups.values.forEachIndexed { i, g ->
            val group1 = g as? NamedGroup
            val group2 = decode.groups[i] as? NamedGroup

            assertNotNull(group1)
            assertNotNull(group2)

            assertSame(group1?.id, (group2?.id ?: Unit))

            group1?.files?.forEachIndexed { j, f ->
                val file1 = f as? NamedGroupFile
                val file2 = decode.groups[i]?.files?.get(j) as? NamedGroupFile

                assertNotNull(file1)
                assertNotNull(file2)

                assertSame(file1?.id, (file2?.id ?: Unit))
                assertSame(file1?.name, (file2?.name ?: Unit))
            }
        }
    }
}