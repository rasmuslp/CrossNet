package crossnet.listener;

import crossnet.Connection;
import crossnet.message.Message;

/**
 * Adapter class with empty implementations of the methods.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ConnectionListenerAdapter implements ConnectionListener {

	@Override
	public void connected( Connection connection ) {
		// Override this if necessary.
	}

	@Override
	public void disconnected( Connection connection ) {
		// Override this if necessary.
	}

	@Override
	public void received( Connection connection, Message message ) {
		// Override this if necessary.
	}

	@Override
	public void idle( Connection connection ) {
		// Override this if necessary.
	}

}
