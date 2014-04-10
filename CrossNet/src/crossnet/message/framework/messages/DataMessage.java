package crossnet.message.framework.messages;

import java.io.IOException;

import crossnet.Connection;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;
import crossnet.util.ByteArrayReader;

/**
 * This is for sending raw data. Users {@link Message}s sent through a {@link Connection} is wrapped in this.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class DataMessage extends FrameworkMessage {

	/**
	 * The payload.
	 */
	private final byte[] data;

	/**
	 * Create a new DataMessage with a payload.
	 * 
	 * @param data
	 *            The payload.
	 */
	public DataMessage( final byte[] data ) {
		super( FrameworkMessageType.DATA );
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
	protected byte[] serializePayload() {
		return this.data;
	}

	/**
	 * Construct a DataMessage from the provided payload.
	 * 
	 * @param payload
	 *            The payload from which to determine the content of this.
	 * @return A freshly parsed DataMessage.
	 */
	public static DataMessage parse( ByteArrayReader payload ) {
		try {
			int bytes = payload.bytesAvailable();
			byte[] data = new byte[bytes];
			payload.readByteArray( data );
			return new DataMessage( data );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing RegisterMessage:", e );
		}

		return null;
	}
}
