package crossnet.packet.lflf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
		Packet packet = null;

		// Prepare buffer for reading
		byteBuffer.flip();

		boolean prevIsLF = false;
		ByteBuffer parseBuffer = ByteBuffer.allocate( byteBuffer.capacity() );
		while ( byteBuffer.hasRemaining() ) {
			byte b = byteBuffer.get();
			parseBuffer.put( b );
			if ( b == '\n' ) {
				if ( prevIsLF ) {
					// Found Packet terminator

					// Prepare buffer for read
					parseBuffer.flip();
					parseBuffer.limit( parseBuffer.limit() - 2 );

					// Read Packet and empty parseBuffer
					byte[] packetData = new byte[parseBuffer.remaining()];
					parseBuffer.get( packetData );
					parseBuffer.clear();

					// Create
					packet = new LFLFPacket( packetData );

					// Compact the buffer and prepare it for reading another Packet
					int remaining = byteBuffer.remaining();
					byteBuffer.compact();
					byteBuffer.position( 0 );
					byteBuffer.limit( remaining );

					break;
				}

				// Current char was LF
				prevIsLF = true;
			} else {
				// Current char was not LF
				prevIsLF = false;
			}
		}

		// Return buffer in writable state (as received)
		byteBuffer.position( byteBuffer.limit() );
		byteBuffer.limit( byteBuffer.capacity() );

		return packet;
	}

	@Override
	public List< Packet > parseDataList( ByteBuffer byteBuffer ) {
		List< Packet > packets = new ArrayList<>();

		// Prepare buffer for reading
		byteBuffer.flip();

		boolean prevIsLF = false;
		ByteBuffer parseBuffer = ByteBuffer.allocate( byteBuffer.capacity() );
		while ( byteBuffer.hasRemaining() ) {
			byte b = byteBuffer.get();
			parseBuffer.put( b );
			if ( b == '\n' ) {
				if ( prevIsLF ) {
					// Found Packet terminator

					// Prepare buffer for read
					parseBuffer.flip();
					parseBuffer.limit( parseBuffer.limit() - 2 );

					// Read Packet and empty parseBuffer
					byte[] packetData = new byte[parseBuffer.remaining()];
					parseBuffer.get( packetData );
					parseBuffer.clear();

					// Create and add
					Packet packet = new LFLFPacket( packetData );
					packets.add( packet );

					// Compact the buffer and prepare it for reading another Packet
					int remaining = byteBuffer.remaining();
					byteBuffer.compact();
					byteBuffer.position( 0 );
					byteBuffer.limit( remaining );
				} else {
					// Current char was LF
					prevIsLF = true;
				}
			} else {
				// Current char was not LF
				prevIsLF = false;
			}
		}

		// Return buffer in writable state (as received)
		byteBuffer.position( byteBuffer.limit() );
		byteBuffer.limit( byteBuffer.capacity() );

		return packets;
	}

}
