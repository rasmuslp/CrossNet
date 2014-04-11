package crossnet.packet.length;

import java.io.IOException;
import java.nio.ByteBuffer;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;
import crossnet.util.ByteArrayReader;

/**
 * Creates {@link LengthPacket}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class LengthPacketFactory implements PacketFactory {

	@Override
	public int getMaxPayloadSize() {
		return LengthPacket.MAX_PAYLOAD_SIZE;
	}

	@Override
	public int getMaxPacketSize() {
		return LengthPacket.MAX_PACKET_SIZE;
	}

	@Override
	public Packet newPacket( final byte[] data ) {
		return new LengthPacket( data );
	}

	@Override
	public Packet parseData( ByteBuffer byteBuffer ) {
		// Prepare buffer for reading.
		byteBuffer.flip();

		try {
			if ( byteBuffer.remaining() >= 2 ) {
				// Has the 2B header, lets start by reading that.
				byte[] header = new byte[2];
				byteBuffer.get( header );
				ByteArrayReader headerReader = new ByteArrayReader( header );
				int payloadLength = headerReader.readUnsignedShort();

				if ( byteBuffer.remaining() >= payloadLength ) {
					// Should also have the payload.
					byte[] payload = new byte[payloadLength];
					byteBuffer.get( payload );

					// Compact buffer and return Packet.
					byteBuffer.compact();

					return new LengthPacket( payload );
				}
			}
		} catch ( IOException e ) {
			// Should never happen.
			Log.error( "CrossNet", "Error deserializing Packet:", e );
		}

		// Return buffer in writable state (as received).
		byteBuffer.position( byteBuffer.limit() );
		byteBuffer.limit( byteBuffer.capacity() );

		return null;
	}

}
