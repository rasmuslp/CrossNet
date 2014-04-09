package crossnet.message;


public interface MessageParser {

	public Message parseData( byte[] data );

	public MessageParser setTieredMessageParser( MessageParser tieredMessageParser );

	public MessageParser getTieredMessageParser();

}
