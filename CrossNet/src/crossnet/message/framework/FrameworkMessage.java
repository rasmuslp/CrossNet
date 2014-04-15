package crossnet.message.framework;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.util.ByteArrayWriter;

/**
 * These are used internally to maintain state and wrap external {@link Message}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class FrameworkMessage implements Message {

	/**
	 * The FrameworkMessage type.
	 */
	protected final FrameworkMessageType frameworkMessageType;

	protected FrameworkMessage( FrameworkMessageType frameworkMessageType ) {
		this.frameworkMessageType = frameworkMessageType;
	}

	@Override
	public byte[] getBytes() {
		try {
			ByteArrayWriter out = new ByteArrayWriter();

			// Write header
			out.writeByte( this.frameworkMessageType.ordinal() );

			// Write payload
			out.writeByteArray( this.serializePayload() );

			return out.toByteArray();
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error serializing Message:", e );
		}

		return null;
	}

	/**
	 * Serialises the payload of a FrameworkMessage.
	 * 
	 * @return The serialisation.
	 * @throws IOException
	 *             If a serialisation error occurs.
	 */
	protected abstract byte[] serializePayload() throws IOException;

}
