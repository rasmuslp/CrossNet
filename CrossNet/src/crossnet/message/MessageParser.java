package crossnet.message;

import crossnet.util.ByteArrayReader;

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

	/**
	 * Tries to construct a Message by parsing data.
	 * <p>
	 * May return null if there was not enough data or an error occurred.
	 * 
	 * @param payload
	 *            The data source to parse from.
	 * @return A freshly parsed Message.
	 */
	public Message parseData( ByteArrayReader payload );

}
