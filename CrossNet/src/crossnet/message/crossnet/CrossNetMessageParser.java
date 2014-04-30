package crossnet.message.crossnet;

import crossnet.log.Log;
import crossnet.message.AbstractMessageParser;
import crossnet.message.Message;
import crossnet.message.crossnet.messages.KeepAliveMessage;
import crossnet.message.crossnet.messages.PingMessage;
import crossnet.message.crossnet.messages.RegisterMessage;
import crossnet.util.ByteArrayReader;

/**
 * MessagParser for CrossNet messages.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class CrossNetMessageParser extends AbstractMessageParser< CrossNetMessageType > {

	public CrossNetMessageParser() {
		super( CrossNetMessageType.class );
	}

	@Override
	protected Message parseType( CrossNetMessageType messageType, ByteArrayReader payload ) {
		Message message = null;

		switch ( messageType ) {
			case REGISTER:
				message = RegisterMessage.parse( payload );
				break;
			case KEEPALIVE:
				message = KeepAliveMessage.parse();
				break;
			case PING:
				message = PingMessage.parse( payload );
				break;
			case TIERED:
				if ( this.tieredMessageParser != null ) {
					message = this.tieredMessageParser.parseData( payload );
				} else {
					Log.warn( "CrossNet", "No tiered parser: Cannot parse content of TieredCrossNetMessage." );
				}
				break;
			default:
				Log.error( "CrossNet", "Unknown CrossNetMessageType, cannot parse: " + messageType );
				break;
		}

		return message;
	}

}
