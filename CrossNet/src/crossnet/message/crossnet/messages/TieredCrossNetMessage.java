package crossnet.message.crossnet.messages;

import crossnet.message.MessageParser;
import crossnet.message.crossnet.CrossNetMessage;
import crossnet.message.crossnet.CrossNetMessageType;

/**
 * This Message is for use together with a tiered {@link MessageParser}.
 * <p>
 * Subclass this such that it looks like {@link CrossNetMessage} without the {@link CrossNetMessage#getBytes()} method.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class TieredCrossNetMessage extends CrossNetMessage {

	/**
	 * Create a new TieredCrossNetMessage.
	 */
	public TieredCrossNetMessage() {
		super( CrossNetMessageType.TIERED );
	}

}
