package crossnet.message.framework;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.AbstractMessageParser;
import crossnet.message.Message;
import crossnet.message.framework.messages.DataMessage;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.PingMessage;
import crossnet.message.framework.messages.RegisterMessage;
import crossnet.util.ByteArrayReader;

/**
 * MessagParser for framework messages.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class FrameworkMessageParser extends AbstractMessageParser {

	/**
	 * The possible types.
	 */
	private final FrameworkMessageType[] frameworkMessageTypes = FrameworkMessageType.values();

	@Override
	public Message parseData( byte[] data ) {
		if ( data.length == 0 ) {
			Log.warn( "CrossNet", "Data length was zero, nothing to parse." );
			return null;
		}

		ByteArrayReader dataReader = new ByteArrayReader( data );

		int type = -1;
		try {
			type = dataReader.readUnsignedByte();
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Could not read type: Cannot parse.", e );
			return null;
		}

		FrameworkMessageType frameworkMessageType;
		try {
			frameworkMessageType = this.frameworkMessageTypes[type];
		} catch ( ArrayIndexOutOfBoundsException e ) {
			Log.error( "CrossNet", "Type not recognized: " + type + " Cannot parse." );
			return null;
		}

		Message message = null;

		switch ( frameworkMessageType ) {
			case REGISTER:
				message = RegisterMessage.parse( dataReader );
				break;
			case KEEPALIVE:
				message = KeepAliveMessage.parse();
				break;
			case PING:
				message = PingMessage.parse( dataReader );
				break;
			case DATA:
				DataMessage dataMessage = DataMessage.parse( dataReader );
				if ( this.tieredMessageParser != null ) {
					message = this.tieredMessageParser.parseData( dataMessage.getData() );
				} else {
					message = dataMessage;
					Log.warn( "CrossNet", "No tiered parser: Cannot parse content of DataMessage." );
				}
				break;
			default:
				Log.error( "CrossNet", "Unknown FrameworkMessageType, cannot parse: " + frameworkMessageType );
				break;
		}

		try {
			if ( dataReader.bytesAvailable() > 0 ) {
				Log.error( "CrossNet", "Not all data was consumed when parsing Message" );
			}
		} catch ( IOException e ) {
			// Ignored
		}

		return message;
	}
}
