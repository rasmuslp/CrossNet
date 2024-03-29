package crossnet.packet.lflf;

import java.nio.ByteBuffer;

import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;

/**
 * Creates {@link LFLFPacket}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class LFLFPacketFactory implements PacketFactory {

	@Override
	public int getMaxPayloadSize() {
		return LFLFPacket.MAX_PAYLOAD_SIZE;
	}

	@Override
	public int getMaxPacketSize() {
		return LFLFPacket.MAX_PACKET_SIZE;
	}

	@Override
	public Packet newPacket( final byte[] data ) {
		return new LFLFPacket( data );
	}

	@Override
	public Packet parseData( ByteBuffer byteBuffer ) {
		// Prepare buffer for reading.
		byteBuffer.flip();

		boolean prevIsLF = false;
		ByteBuffer parseBuffer = ByteBuffer.allocate( byteBuffer.capacity() );
		while ( byteBuffer.hasRemaining() ) {
			byte b = byteBuffer.get();
			parseBuffer.put( b );
			if ( b == '\n' ) {
				if ( prevIsLF ) {
					// Found Packet terminator.

					// Prepare buffer for read.
					parseBuffer.flip();
					parseBuffer.limit( parseBuffer.limit() - 2 );

					// Read Packet and empty parseBuffer.
					byte[] packetData = new byte[parseBuffer.remaining()];
					parseBuffer.get( packetData );
					parseBuffer.clear();

					// Compact the buffer and return Packet.
					byteBuffer.compact();

					return new LFLFPacket( packetData );
				}

				// Current char was LF.
				prevIsLF = true;
			} else {
				// Current char was not LF.
				prevIsLF = false;
			}
		}

		// Return buffer in writable state (as received).
		byteBuffer.position( byteBuffer.limit() );
		byteBuffer.limit( byteBuffer.capacity() );

		return null;
	}

}
