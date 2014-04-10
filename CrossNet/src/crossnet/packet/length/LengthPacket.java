package crossnet.packet.length;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.util.ByteArrayWriter;

/**
 * The LengthPacket is a Packet that has a 2B header, which denotes the number of bytes following the header, that is
 * this Packet's payload.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class LengthPacket extends Packet {

	/**
	 * Maximum payload size.
	 */
	public final static int MAX_PAYLOAD_SIZE = 65535;

	/**
	 * Maximum packet size.
	 */
	public final static int MAX_PACKET_SIZE = 2 + LengthPacket.MAX_PAYLOAD_SIZE;

	/**
	 * Create a Packet with payload.
	 * 
	 * @param payload
	 *            The payload.
	 */
	protected LengthPacket( final byte[] payload ) {
		super( payload );

		if ( payload == null ) {
			throw new IllegalArgumentException( "Data cannot be null" );
		}

		if ( payload.length > LengthPacket.MAX_PAYLOAD_SIZE ) {
			throw new IllegalArgumentException( "Data too large. Is " + payload.length + "B, but maximum is: " + LengthPacket.MAX_PAYLOAD_SIZE + "B." );
		}
	}

	@Override
	public byte[] toBytes() {
		ByteArrayWriter byteArrayWriter = new ByteArrayWriter();

		try {
			// 2B header that describes the length of the payload
			byteArrayWriter.writeShort( this.payload.length );

			// Payload
			byteArrayWriter.writeByteArray( this.payload );

			return byteArrayWriter.toByteArray();
		} catch ( IOException e ) {
			// Should never happen.
			Log.error( "CrossNet", "Error serializing Packet:", e );
		}

		return null;
	}

}
