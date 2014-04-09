package crossnet.message.framework.messages;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

public class RegisterMessage extends FrameworkMessage {

	private final int connectionId;

	public RegisterMessage( final int connectionId ) {
		super( FrameworkMessageType.REGISTER );
		this.connectionId = connectionId;
	}

	public int getConnectionID() {
		return this.connectionId;
	}

	@Override
	protected byte[] serializePayload() throws IOException {
		ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
		byteArrayWriter.writeInt( this.connectionId );
		return byteArrayWriter.toByteArray();
	}

	public static RegisterMessage parse( byte[] payload ) {
		try {
			ByteArrayReader byteArrayReader = new ByteArrayReader( payload );
			int connectionId = byteArrayReader.readInt();
			return new RegisterMessage( connectionId );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing RegisterMessage:", e );
		}

		return null;
	}

}
