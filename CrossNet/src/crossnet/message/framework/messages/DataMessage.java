package crossnet.message.framework.messages;

import crossnet.message.Message;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;

public class DataMessage extends FrameworkMessage {

	private final byte[] data;

	public DataMessage( final byte[] data ) {
		super( FrameworkMessageType.DATA );
		this.data = data;
	}

	/**
	 * Do _not_ put FrameworkMessages in here unless you really want them wrapped in a DataMessage.
	 * 
	 * @param message
	 */
	public DataMessage( Message message ) {
		this( message.getBytes() );
	}

	public byte[] getData() {
		return this.data;
	}

	@Override
	protected byte[] serializePayload() {
		return this.data;
	}

	public static DataMessage parse( byte[] payload ) {
		return new DataMessage( payload );
	}

}
