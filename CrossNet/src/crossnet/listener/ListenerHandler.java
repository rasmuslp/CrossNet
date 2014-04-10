package crossnet.listener;

import java.util.ArrayList;
import java.util.List;

import crossnet.Connection;
import crossnet.Server;
import crossnet.message.Message;

/**
 * Redistributes events.
 * 
 * Used internally in {@link Server} and {@link Connection}.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ListenerHandler implements ConnectionListener {

	/**
	 * Lock for adding and removing listeners.
	 */
	private final Object lock = new Object();

	/**
	 * The listeners registered with this.
	 */
	protected final List< ConnectionListener > connectionListeners = new ArrayList<>();

	/**
	 * Adds a listener. A listener cannot be added multiple times.
	 * 
	 * @param connectionListener
	 *            The listener to add.
	 */
	public void addConnectionListener( ConnectionListener connectionListener ) {
		if ( connectionListener == null ) {
			throw new IllegalArgumentException( "ConnectionListener cannot be null." );
		}

		synchronized ( this.lock ) {
			if ( this.connectionListeners.contains( connectionListener ) ) {
				return;
			}

			this.connectionListeners.add( connectionListener );
		}
	}

	/**
	 * Removes a listener.
	 * 
	 * @param connectionListener
	 *            The listener to remove.
	 */
	public void removeConnectionListener( ConnectionListener connectionListener ) {
		if ( connectionListener == null ) {
			throw new IllegalArgumentException( "ConnectionListener cannot be null." );
		}

		synchronized ( this.lock ) {
			if ( !this.connectionListeners.contains( connectionListener ) ) {
				return;
			}

			this.connectionListeners.remove( connectionListener );
		}
	}

	@Override
	public void connected( Connection connection ) {
		for ( ConnectionListener connectionListener : this.connectionListeners ) {
			connectionListener.connected( connection );
		}
	}

	@Override
	public void disconnected( Connection connection ) {
		for ( ConnectionListener connectionListener : this.connectionListeners ) {
			connectionListener.disconnected( connection );
		}
	}

	@Override
	public void received( Connection connection, Message message ) {
		for ( ConnectionListener connectionListener : this.connectionListeners ) {
			connectionListener.received( connection, message );
		}
	}

	@Override
	public void idle( Connection connection ) {
		for ( ConnectionListener connectionListener : this.connectionListeners ) {
			connectionListener.idle( connection );
		}
	}

}
