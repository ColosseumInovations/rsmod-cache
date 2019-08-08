package gg.rsmod.cache.io

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyPacketTests {

    @Test
    fun `construct packet`() {
        val packet = ReadOnlyPacket(PACKET_SIZE)
        assertSame(0, packet.position)
        assertSame(PACKET_SIZE, packet.readableBytes)
        assertTrue(packet.isReadable)
    }

    @Test
    fun `underflow packet`() {
        val packet = ReadOnlyPacket(PACKET_SIZE)
        assertSame(0, packet.g1)
        assertThrows<ArrayIndexOutOfBoundsException> { packet.g1 }
    }

    @Test
    fun `read from predefined array-backed packet`() {
        val value = 18
        val packet = ReadOnlyPacket.of(ByteArray(1) {
            value.toByte()
        })
        assertSame(value, packet.g1)
    }

    companion object {
        private const val PACKET_SIZE = 1
    }
}