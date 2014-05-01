package crossnet.message.crossnet;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.util.ByteArrayWriter;

/**
 * Abstract Message that is used internally to maintain state.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class CrossNetMessage implements Message {

	/**
	 * The type of Message.
	 */
	protected final CrossNetMessageType crossNetMessageType;

	public CrossNetMessage( final CrossNetMessageType messageType ) {
		this.crossNetMessageType = messageType;
	}

	@Override
	public byte[] getBytes() {
		try {
			ByteArrayWriter out = new ByteArrayWriter();

			// Write header
			out.writeByte( this.crossNetMessageType.ordinal() );

			// Write payload
			this.serializeCrossNetPayload( out );

			return out.toByteArray();
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error serializing Message:", e );
		}

		return null;
	}

	/**
	 * Serialises the payload of the CrossNetMessage.
	 * 
	 * @param to
	 *            The destination of the serialisation.
	 * @throws IOException
	 *             If a serialisation error occurs.
	 */
	protected abstract void serializeCrossNetPayload( ByteArrayWriter to ) throws IOException;
}
