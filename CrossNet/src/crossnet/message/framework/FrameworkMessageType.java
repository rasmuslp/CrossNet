package crossnet.message.framework;

import crossnet.Connection;

/**
 * The various types of {@link FrameworkMessage}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public enum FrameworkMessageType {

	/**
	 * {@link Server} sends this to {@link Client} when establishing a {@link Connection}.
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
	 * For sending raw data.
	 */
	DATA
}
