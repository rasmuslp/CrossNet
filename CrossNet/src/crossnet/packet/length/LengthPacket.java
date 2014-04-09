package crossnet.packet.length;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.util.ByteArrayWriter;

public class LengthPacket extends Packet {

	public final static int MAX_DATA = 65535;
	public final static int MAX_LENGTH = 2 + LengthPacket.MAX_DATA;

	protected LengthPacket( byte[] data ) {
		super( data );

		if ( data == null ) {
			throw new IllegalArgumentException( "Data cannot be null" );
		}

		if ( data.length > LengthPacket.MAX_DATA ) {
			throw new IllegalArgumentException( "Data too large. Is " + data.length + "B, but maximum is: " + LengthPacket.MAX_DATA + "B." );
		}
	}

	@Override
	public byte[] toBytes() {
		ByteArrayWriter byteArrayWriter = new ByteArrayWriter();

		try {
			// 2B header that describes the length of the payload
			byteArrayWriter.writeShort( this.data.length );

			// Payload
			byteArrayWriter.writeByteArray( this.data );

			return byteArrayWriter.toByteArray();
		} catch ( IOException e ) {
			// Should never happen.
			Log.error( "CrossNet", "Error serializing Packet:", e );
		}

		return null;
	}

}
