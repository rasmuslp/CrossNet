package crossnet.packet.length;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;
import crossnet.util.ByteArrayReader;

public class LengthPacketFactory implements PacketFactory {

	@Override
	public int getMaxData() {
		return LengthPacket.MAX_DATA;
	}

	@Override
	public int getMaxLength() {
		return LengthPacket.MAX_LENGTH;
	}

	@Override
	public Packet newPacket( final byte[] data ) {
		return new LengthPacket( data );
	}

	@Override
	public Packet parseData( ByteBuffer byteBuffer ) {
		// Prepare buffer for reading
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

					Packet packet = new LengthPacket( payload );

					// Compact buffer and return Packet
					byteBuffer.compact();

					return packet;
				}
			}
		} catch ( IOException e ) {
			// Should never happen.
			Log.error( "CrossNet", "Error deserializing Packet:", e );
		}

		// Return buffer in writable state (as received)
		byteBuffer.position( byteBuffer.limit() );
		byteBuffer.limit( byteBuffer.capacity() );

		return null;
	}

	@Override
	public List< Packet > parseDataList( ByteBuffer byteBuffer ) {
		// TODO Auto-generated method stub
		return null;
	}

	public static String bytesToHex( byte[] in ) {
		final StringBuilder builder = new StringBuilder();
		for ( byte b : in ) {
			builder.append( String.format( "%02x", b ) );
		}
		return builder.toString();
	}

}
