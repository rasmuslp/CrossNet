package crossnet.message.crossnet.messages;

import java.io.IOException;

import crossnet.Connection;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.crossnet.CrossNetMessage;
import crossnet.message.crossnet.CrossNetMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

/**
 * This is for sending raw data. Users {@link Message}s sent through a {@link Connection} is wrapped in this.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class TieredCrossNetMessage extends CrossNetMessage {

	/**
	 * The payload.
	 */
	private final byte[] data;

	/**
	 * Create a new TieredCrossNetMessage with a payload.
	 * 
	 * @param data
	 *            The payload.
	 */
	public TieredCrossNetMessage( final byte[] data ) {
		super( CrossNetMessageType.TIERED );
		if ( data == null ) {
			throw new IllegalArgumentException( "Data cannot be null." );
		}
		this.data = data;
	}

	/**
	 * Gets the payload.
	 * 
	 * @return The payload.
	 */
	public byte[] getData() {
		return this.data;
	}

	@Override
	protected void serializePayload( ByteArrayWriter to ) throws IOException {
		to.writeByteArray( this.data );
	}

	/**
	 * Construct a TieredCrossNetMessage from the provided payload.
	 * 
	 * @param payload
	 *            The payload from which to determine the content of this.
	 * @return A freshly parsed TieredCrossNetMessage.
	 */
	public static TieredCrossNetMessage parse( ByteArrayReader payload ) {
		try {
			int bytes = payload.bytesAvailable();
			byte[] data = new byte[bytes];
			payload.readByteArray( data );
			return new TieredCrossNetMessage( data );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing TieredCrossNetMessage:", e );
		}

		return null;
	}
}
