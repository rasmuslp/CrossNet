package crossnet.message.framework.messages;

import crossnet.Connection;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;

/**
 * For hindering the {@link Connection} from timing out.
 * <p>
 * Has no payload.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class KeepAliveMessage extends FrameworkMessage {

	public KeepAliveMessage() {
		super( FrameworkMessageType.KEEPALIVE );
	}

	@Override
	protected byte[] serializePayload() {
		return new byte[0];
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
