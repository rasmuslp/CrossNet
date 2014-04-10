package crossnet.message.framework.messages;

import java.io.IOException;

import crossnet.Connection;
import crossnet.log.Log;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

/**
 * {@link Server} sends this to {@link Client} when establishing a {@link Connection}.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class RegisterMessage extends FrameworkMessage {

	/**
	 * The ID of the connection.
	 */
	private final int connectionId;

	/**
	 * Create new RegisterMessage with connection ID.
	 * 
	 * @param connectionId
	 *            The ID.
	 */
	public RegisterMessage( final int connectionId ) {
		super( FrameworkMessageType.REGISTER );
		this.connectionId = connectionId;
	}

	/**
	 * Gets the connection ID.
	 * 
	 * @return The connection ID.
	 */
	public int getConnectionID() {
		return this.connectionId;
	}

	@Override
	protected byte[] serializePayload() throws IOException {
		ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
		byteArrayWriter.writeInt( this.connectionId );
		return byteArrayWriter.toByteArray();
	}

	/**
	 * Construct a RegisterMessage from the provided payload.
	 * 
	 * @param payload
	 *            The payload from which to determine the content of this.
	 * @return A freshly parsed RegisterMessage.
	 */
	public static RegisterMessage parse( ByteArrayReader payload ) {
		try {
			int connectionId = payload.readInt();
			return new RegisterMessage( connectionId );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing RegisterMessage:", e );
		}

		return null;
	}

}
