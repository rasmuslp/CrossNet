package crossnet.message.framework;

import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.framework.messages.DataMessage;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.PingMessage;
import crossnet.message.framework.messages.RegisterMessage;
import crossnet.packet.Packet;

public class FrameworkMessageParser implements MessageParser {

	private static final FrameworkMessageType[] frameworkMessageTypes = FrameworkMessageType.values();

	private MessageParser tieredMessageParser;

	public Message parsePacket( Packet packet ) {
		return this.parseData( packet.getData() );
	}

	@Override
	public Message parseData( byte[] data ) {
		if ( data.length == 0 ) {
			//TODO LOG
			System.out.println( "Data length was zero" );
			return null;
		}

		byte[] payload = new byte[data.length - 1];
		System.arraycopy( data, 1, payload, 0, data.length - 1 );

		FrameworkMessageType frameworkMessageType;
		try {
			frameworkMessageType = FrameworkMessageParser.frameworkMessageTypes[data[0]];
		} catch ( ArrayIndexOutOfBoundsException e ) {
			//TODO LOG
			System.out.println( "Type not recognized: " + data[0] );
			return null;
		}

		Message message = null;

		switch ( frameworkMessageType ) {
			case REGISTER:
				message = RegisterMessage.parse( payload );
				break;
			case KEEPALIVE:
				message = KeepAliveMessage.parse();
				break;
			case PING:
				message = PingMessage.parse( payload );
				break;
			case DATA:
				if ( this.tieredMessageParser != null ) {
					DataMessage dataMessage = DataMessage.parse( payload );
					message = this.tieredMessageParser.parseData( dataMessage.getData() );
				} else {
					//TODO: LOG
				}
				break;
			default:
				//TODO: LOG
		}

		return message;
	}

	@Override
	public MessageParser setTieredMessageParser( MessageParser tieredMessageParser ) {
		MessageParser removedTieredMessageParser = this.tieredMessageParser;
		this.tieredMessageParser = tieredMessageParser;
		return removedTieredMessageParser;
	}

	@Override
	public MessageParser getTieredMessageParser() {
		return this.tieredMessageParser;
	}

}
