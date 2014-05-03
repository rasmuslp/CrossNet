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
	 * @return A Class name of this Message-
	 */
	public String getMessageClass();

	/**
	 * @return The bytes that constitutes this Message.
	 */
	public byte[] getBytes();

}
