package crossnet.message;

/**
 * Abstract implementation that handles support for a single tiered MessageParser.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class AbstractMessageParser implements MessageParser {

	/**
	 * The tiered MessageParser, if any.
	 */
	protected MessageParser tieredMessageParser;

	@Override
	public abstract Message parseData( byte[] data );

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
}
