package crossnet.message.crossnet.messages;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.crossnet.CrossNetMessage;
import crossnet.message.crossnet.CrossNetMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

/**
 * For determining round trip time.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class PingMessage extends CrossNetMessage {

	/**
	 * Signals if this is a request or a response.
	 */
	private boolean isReply = false;

	/**
	 * The ping ID.
	 */
	private final int id;

	/**
	 * Create new PingMessage with ping ID.
	 * 
	 * @param id
	 *            The ID.
	 */
	public PingMessage( final int id ) {
		super( CrossNetMessageType.PING );
		this.id = id;
	}

	/**
	 * Internal constructor used when parsing byte[].
	 * 
	 * @param isReply
	 * @param id
	 */
	private PingMessage( boolean isReply, final int id ) {
		super( CrossNetMessageType.PING );
		this.isReply = isReply;
		this.id = id;
	}

	/**
	 * Determine if this is a reply.
	 * 
	 * @return {@code True} iff this is a reply.
	 */
	public boolean isReply() {
		return this.isReply;
	}

	/**
	 * Marks this as a reply.
	 */
	public void setReply() {
		this.isReply = true;
	}

	/**
	 * Gets the ping ID.
	 * 
	 * @return The ping ID.
	 */
	public long getId() {
		return this.id;
	}

	@Override
	protected void serializePayload( ByteArrayWriter to ) throws IOException {
		to.writeBoolean( this.isReply );
		to.writeInt( this.id );
	}

	/**
	 * Construct a PingMessage from the provided payload.
	 * 
	 * @param payload
	 *            The payload from which to determine the content of this.
	 * @return A freshly parsed PingMessage
	 */
	public static PingMessage parse( ByteArrayReader payload ) {
		try {
			boolean isReply = payload.readBoolean();
			int id = payload.readInt();
			return new PingMessage( isReply, id );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing PingMessage:", e );
		}

		return null;
	}

}
