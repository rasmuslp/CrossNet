package crossnet.message;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.util.ByteArrayReader;

/**
 * Abstract implementation that handles support for a single tiered MessageParser.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class AbstractMessageParser< E extends Enum< E > > implements MessageParser {

	private final E[] messageTypes;

	/**
	 * The tiered MessageParser, if any.
	 */
	protected MessageParser tieredMessageParser;

	public AbstractMessageParser( Class< E > messageTypeClass ) {
		this.messageTypes = messageTypeClass.getEnumConstants();
	}

	@Override
	public final MessageParser setTieredMessageParser( MessageParser tieredMessageParser ) {
		MessageParser removedTieredMessageParser = this.tieredMessageParser;
		this.tieredMessageParser = tieredMessageParser;
		return removedTieredMessageParser;
	}

	@Override
	public final MessageParser getTieredMessageParser() {
		return this.tieredMessageParser;
	}

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

		E messageType;
		try {
			messageType = this.messageTypes[type];
		} catch ( ArrayIndexOutOfBoundsException e ) {
			Log.error( "CrossNet", "Type not recognized: " + type + " Cannot parse." );
			return null;
		}

		Message message = this.parseType( messageType, dataReader );

		if ( message == null ) {
			Log.error( "CrossNet", "Parsed Message was null. Type was: " + messageType );
		}

		try {
			int bytesRemaining = dataReader.bytesAvailable();
			if ( bytesRemaining > 0 ) {
				Log.error( "CrossNet", "Not all data was consumed when parsing type: " + messageType + ". Bytes remaining: " + bytesRemaining );
			}
		} catch ( IOException e ) {
			// Ignored
		}

		return message;
	}

	/**
	 * When the type has been determined, this will parse to a Message according to the type.
	 * 
	 * @param messageType
	 *            The type of Message.
	 * @param payload
	 *            The payload to parse.
	 * @return A freshly parsed Message.
	 */
	protected abstract Message parseType( E messageType, ByteArrayReader payload );
}
