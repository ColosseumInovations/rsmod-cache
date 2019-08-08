package gg.rsmod.cache.archive

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.WriteOnlyPacket
import gg.rsmod.cache.util.FileSystemDataBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataBlockTests {

    @Test
    fun `decode packet`() {
        val packet = ReadOnlyPacket(PACKET_SIZE)
        assertNull(DataBlockCodec.decode(packet).getError())
        assertEquals(PACKET_SIZE, packet.position)
    }

    @Test
    fun `encode packet`() {
        val packet = WriteOnlyPacket(PACKET_SIZE)
        val value = FileSystemDataBlock(archive = 0, group = 0, currBlockIndex = 0, nextBlock = 0)
        assertNull(DataBlockCodec.encode(packet, value).getError())
        assertEquals(PACKET_SIZE, packet.position)
    }

    @Test
    fun `encode and decode packet`() {
        val writer = WriteOnlyPacket(PACKET_SIZE)
        val value = FileSystemDataBlock(archive = 8, group = 8, currBlockIndex = 0, nextBlock = 1)

        assertNull(DataBlockCodec.encode(writer, value).getError())

        val reader = ReadOnlyPacket.of(writer.array)
        val decoded = DataBlockCodec.decode(reader)
        assertNull(decoded.getError())
        assertSame((decoded.get()?.archive ?: Unit), value.archive)
        assertSame((decoded.get()?.group ?: Unit), value.group)
        assertSame((decoded.get()?.currBlockIndex ?: Unit), value.currBlockIndex)
        assertSame((decoded.get()?.nextBlock ?: Unit), value.nextBlock)
    }

    @Test
    fun `decode extended packet`() {
        val packet = ReadOnlyPacket(PACKET_SIZE_EXTENDED)
        assertNull(DataBlockCodec.decodeExtended(packet).getError())
        assertEquals(PACKET_SIZE_EXTENDED, packet.position)
    }

    @Test
    fun `encode extended packet`() {
        val packet = WriteOnlyPacket(PACKET_SIZE_EXTENDED)
        val value = FileSystemDataBlock(archive = 0, group = 0, currBlockIndex = 0, nextBlock = 0)
        assertNull(DataBlockCodec.encodeExtended(packet, value).getError())
        assertEquals(PACKET_SIZE_EXTENDED, packet.position)
    }

    @Test
    fun `encode and decode extended packet`() {
        val writer = WriteOnlyPacket(PACKET_SIZE_EXTENDED)
        val value = FileSystemDataBlock(archive = 8, group = 8, currBlockIndex = 0, nextBlock = 1)

        assertNull(DataBlockCodec.encodeExtended(writer, value).getError())

        val reader = ReadOnlyPacket.of(writer.array)
        val decoded = DataBlockCodec.decodeExtended(reader)
        assertNull(decoded.getError())
        assertSame((decoded.get()?.archive ?: Unit), value.archive)
        assertSame((decoded.get()?.group ?: Unit), value.group)
        assertSame((decoded.get()?.currBlockIndex ?: Unit), value.currBlockIndex)
        assertSame((decoded.get()?.nextBlock ?: Unit), value.nextBlock)
    }

    companion object {
        private const val PACKET_SIZE = 8
        private const val PACKET_SIZE_EXTENDED = 10
    }
}