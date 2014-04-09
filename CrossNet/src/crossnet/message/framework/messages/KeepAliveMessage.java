package crossnet.message.framework.messages;

import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;

public class KeepAliveMessage extends FrameworkMessage {

	public KeepAliveMessage() {
		super( FrameworkMessageType.KEEPALIVE );
	}

	@Override
	protected byte[] serializePayload() {
		return new byte[0];
	}

	public static KeepAliveMessage parse() {
		return new KeepAliveMessage();
	}

}
