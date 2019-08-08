package gg.rsmod.cache.archive

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import gg.rsmod.cache.domain.PacketNotEnoughData
import gg.rsmod.cache.domain.PacketOverflow
import gg.rsmod.cache.io.ReadOnlyPacket
import gg.rsmod.cache.io.WriteOnlyPacket
import gg.rsmod.cache.util.FileSystemDataBlockPointer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataBlockPointerTests {

    @Test
    fun `decode packet`() {
        val packet = ReadOnlyPacket(PACKET_SIZE)
        assertNotEquals(PacketOverflow, DataBlockPointerCodec.decode(packet).getError())
        assertEquals(PACKET_SIZE, packet.position)
    }

    @Test
    fun `decode packet to underflow`() {
        val packet = ReadOnlyPacket(PACKET_SIZE - 1)
        assertEquals(PacketNotEnoughData, DataBlockPointerCodec.decode(packet).getError())
        assertEquals(0, packet.position)
    }

    @Test
    fun `encode packet`() {
        val packet = WriteOnlyPacket(PACKET_SIZE)
        val value = FileSystemDataBlockPointer(offset = 0, length = 0)
        assertNull(DataBlockPointerCodec.encode(value, packet).getError())
        assertEquals(PACKET_SIZE, packet.position)
    }

    @Test
    fun `encode packet to overflow`() {
        val packet = WriteOnlyPacket(PACKET_SIZE - 1)
        val value = FileSystemDataBlockPointer(offset = 0, length = 0)
        assertEquals(PacketOverflow, DataBlockPointerCodec.encode(value, packet).getError())
        assertEquals(0, packet.position)
    }

    @Test
    fun `encode and decode packet`() {
        val writer = WriteOnlyPacket(PACKET_SIZE)
        val value = FileSystemDataBlockPointer(offset = 8, length = 8)

        assertNotEquals(PacketOverflow, DataBlockPointerCodec.encode(value, writer).getError())

        val reader = ReadOnlyPacket.of(writer.array)
        val decoded = DataBlockPointerCodec.decode(reader)
        assertNull(decoded.getError())
        assertSame((decoded.get()?.offset ?: Unit), value.offset)
        assertSame((decoded.get()?.length ?: Unit), value.length)
    }

    companion object {
        private const val PACKET_SIZE = 6
    }
}