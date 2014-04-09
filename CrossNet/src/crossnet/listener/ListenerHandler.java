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
public class ListenerHandler implements Listener {

	/**
	 * Lock for adding and removing listeners.
	 */
	private final Object lock = new Object();

	/**
	 * The listeners registered with this.
	 */
	protected final List< Listener > listeners = new ArrayList<>();

	/**
	 * Adds a listener. A Listener cannot be added multiple times.
	 * 
	 * @param listener
	 *            The Listener to add.
	 */
	public void addListener( Listener listener ) {
		if ( listener == null ) {
			throw new IllegalArgumentException( "Listener cannot be null." );
		}

		synchronized ( this.lock ) {
			if ( this.listeners.contains( listener ) ) {
				return;
			}

			this.listeners.add( listener );
		}
	}

	/**
	 * Removes a Listener.
	 * 
	 * @param listener
	 *            The Listener to remove.
	 */
	public void removeListener( Listener listener ) {
		if ( listener == null ) {
			throw new IllegalArgumentException( "Listener cannot be null." );
		}

		synchronized ( this.lock ) {
			if ( !this.listeners.contains( listener ) ) {
				return;
			}

			this.listeners.remove( listener );
		}
	}

	@Override
	public void connected( Connection connection ) {
		for ( Listener listener : this.listeners ) {
			listener.connected( connection );
		}
	}

	@Override
	public void disconnected( Connection connection ) {
		for ( Listener listener : this.listeners ) {
			listener.disconnected( connection );
		}
	}

	@Override
	public void received( Connection connection, Message message ) {
		for ( Listener listener : this.listeners ) {
			listener.received( connection, message );
		}
	}

	@Override
	public void idle( Connection connection ) {
		for ( Listener listener : this.listeners ) {
			listener.idle( connection );
		}
	}

}
