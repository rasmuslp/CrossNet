package crossnet.message;

import crossnet.Connection;

/**
 * A Message to be sent through a {@link Connection}.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface Message {

	/**
	 * Get the bytes that constitutes this Message.
	 * 
	 * @return The bytes that constitutes this Message.
	 */
	public byte[] getBytes();

}
