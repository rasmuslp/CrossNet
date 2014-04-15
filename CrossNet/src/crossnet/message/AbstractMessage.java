package crossnet.message;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.util.ByteArrayWriter;

/**
 * An abstract Message that handles up to 256 different Message types.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 * @param <E>
 *            The enum describing the possible Message types.
 */
public abstract class AbstractMessage< E extends Enum< E > > implements Message {

	/**
	 * The type of Message.
	 */
	protected final E messageType;

	public AbstractMessage( final E messageType ) {
		this.messageType = messageType;
	}

	@Override
	public byte[] getBytes() {
		try {
			ByteArrayWriter out = new ByteArrayWriter();

			// Write header
			out.writeByte( this.messageType.ordinal() );

			// Write payload
			this.serializePayload( out );

			return out.toByteArray();
		} catch ( IOException e ) {
			Log.error( "GLHF", "Error serializing Message:", e );
		}

		return null;
	}

	/**
	 * Serialises the payload of the AbstractMessage.
	 * 
	 * @param to
	 *            The destination of the serialisation.
	 * @throws IOException
	 *             If a serialisation error occurs.
	 */
	protected abstract void serializePayload( ByteArrayWriter to ) throws IOException;
}
