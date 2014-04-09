package crossnet.message.framework.messages;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.FrameworkMessageType;
import crossnet.util.ByteArrayReader;
import crossnet.util.ByteArrayWriter;

public class PingMessage extends FrameworkMessage {

	private boolean isReply = false;
	private final int id;

	public PingMessage( final int id ) {
		super( FrameworkMessageType.PING );
		this.id = id;
	}

	private PingMessage( boolean isReply, final int id ) {
		super( FrameworkMessageType.PING );
		this.isReply = isReply;
		this.id = id;
	}

	public boolean isReply() {
		return this.isReply;
	}

	public void setReply() {
		this.isReply = true;
	}

	public long getId() {
		return this.id;
	}

	@Override
	protected byte[] serializePayload() throws IOException {
		ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
		byteArrayWriter.writeBoolean( this.isReply );
		byteArrayWriter.writeInt( this.id );
		return byteArrayWriter.toByteArray();
	}

	public static PingMessage parse( byte[] payload ) {
		try {
			ByteArrayReader byteArrayReader = new ByteArrayReader( payload );
			boolean isReply = byteArrayReader.readBoolean();
			int id = byteArrayReader.readInt();
			return new PingMessage( isReply, id );
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error deserializing PingMessage:", e );
		}

		return null;
	}

}
