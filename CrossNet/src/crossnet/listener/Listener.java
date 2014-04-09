package crossnet.listener;

import crossnet.Connection;
import crossnet.message.Message;

/**
 * Listener for events from {@link Connection}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface Listener {

	/**
	 * New connection connected.
	 * 
	 * @param connection
	 *            The new Connection.
	 */
	public void connected( Connection connection );

	/**
	 * Connection disconnected.
	 * 
	 * @param connection
	 *            The connection that disconnected.
	 */
	public void disconnected( Connection connection );

	/**
	 * Connection received a message.
	 * 
	 * @param connection
	 *            The connection that received.
	 * @param message
	 *            The message that was received.
	 */
	public void received( Connection connection, Message message );

	/**
	 * Connection is idle.
	 * 
	 * @param connection
	 *            The idle connection.
	 */
	public void idle( Connection connection );

}
