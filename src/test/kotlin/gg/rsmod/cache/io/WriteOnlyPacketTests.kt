package gg.rsmod.cache.io

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WriteOnlyPacketTests {

    @Test
    fun `construct packet`() {
        val packet = WriteOnlyPacket(PACKET_SIZE)
        assertSame(0, packet.position)
        assertSame(PACKET_SIZE, packet.writableBytes)
        assertTrue(packet.isWritable)
    }

    @Test
    fun `overflow packet`() {
        val packet = WriteOnlyPacket(PACKET_SIZE)
        repeat(PACKET_SIZE) {
            packet.p1(0)
        }
        assertThrows<ArrayIndexOutOfBoundsException> { packet.p1(0) }
    }

    @Test
    fun `write to packet and read from backed-array`() {
        val value = 17
        val packet = WriteOnlyPacket(PACKET_SIZE)
        packet.p1(value)
        assertSame(value, packet.array[0].toInt())
    }

    companion object {
        private const val PACKET_SIZE = 1
    }
}