package crossnet.message.crossnet.messages;

import crossnet.Connection;
import crossnet.message.crossnet.CrossNetMessage;
import crossnet.message.crossnet.CrossNetMessageType;
import crossnet.util.ByteArrayWriter;

/**
 * For hindering the {@link Connection} from timing out.
 * <p>
 * Has no payload.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class KeepAliveMessage extends CrossNetMessage {

	public KeepAliveMessage() {
		super( CrossNetMessageType.KEEPALIVE );
	}

	@Override
	protected void serializeCrossNetPayload( ByteArrayWriter to ) {
		// No payload to serialise.
	}

	/**
	 * Constructs a KeepAliveMessage from the provided data. (None)
	 * 
	 * @return A freshly parsed KeepAliveMessage
	 */
	public static KeepAliveMessage parse() {
		return new KeepAliveMessage();
	}

}
