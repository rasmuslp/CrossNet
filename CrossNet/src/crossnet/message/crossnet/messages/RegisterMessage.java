package crossnet.message.crossnet.messages;

import java.io.IOException;

import crossnet.Connection;
import crossnet.log.Log;
import crossnet.message.crossnet.CrossNetMessage;
import crossnet.message.crossnet.CrossNetMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

/**
 * {@link Server} sends this to {@link Client} when establishing a {@link Connection}.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class RegisterMessage extends CrossNetMessage {

	/**
	 * The ID of the connection.
	 */
	private final int id;

	/**
	 * Create new RegisterMessage with a connection ID.
	 * 
	 * @param id
	 *            The ID.
	 */
	public RegisterMessage( final int id ) {
		super( CrossNetMessageType.REGISTER );
		this.id = id;
	}

	/**
	 * Gets the connection ID.
	 * 
	 * @return The connection ID.
	 */
	public int getId() {
		return this.id;
	}

	@Override
	protected void serializePayload( ByteArrayWriter to ) throws IOException {
		to.writeInt( this.id );
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
			int id = payload.readInt();
			return new RegisterMessage( id );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing RegisterMessage:", e );
		}

		return null;
	}

}
