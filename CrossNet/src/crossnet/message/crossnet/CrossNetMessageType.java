package crossnet.message.crossnet;

import crossnet.Connection;
import crossnet.CrossNetClient;
import crossnet.CrossNetServer;

/**
 * The various types of {@link CrossNetMessage}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public enum CrossNetMessageType {

	/**
	 * {@link CrossNetServer} sends this to {@link CrossNetClient} when establishing a {@link Connection}.
	 */
	REGISTER,

	/**
	 * For hindering the {@link Connection} from timing out.
	 */
	KEEPALIVE,

	/**
	 * For determining round trip time.
	 */
	PING,

	/**
	 * For sending tiered Messages.
	 */
	TIERED
}
