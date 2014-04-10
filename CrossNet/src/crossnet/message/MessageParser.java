package crossnet.message;


/**
 * Parses byte arrays to {@link Message}s.
 * <p>
 * Parsing can be tiered.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface MessageParser {

	/**
	 * Parse data and tries to construct a Message.
	 * <p>
	 * May return null if there was not enough data or an error occurred.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return A freshly parsed Message.
	 */
	public Message parseData( byte[] data );

	/**
	 * Provide a tiered MessageParser. Useful for layered parsing.
	 * 
	 * @param tieredMessageParser
	 *            Set to null to disable.
	 * @return The previously set tiered MessageParser, null otherwise.
	 */
	public MessageParser setTieredMessageParser( MessageParser tieredMessageParser );

	/**
	 * Gets the current tiered MessageParser.
	 * 
	 * @return The current tiered MessageParser.
	 */
	public MessageParser getTieredMessageParser();

}
