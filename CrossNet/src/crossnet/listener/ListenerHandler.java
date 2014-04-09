package crossnet.listener;

import java.util.ArrayList;
import java.util.List;

import crossnet.Connection;
import crossnet.log.Log;
import crossnet.message.Message;

/**
 * Redistributes events.
 * 
 * Used internally in Server and Connection.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ListenerHandler implements Listener {

	private final Object lock = new Object();

	protected final List< Listener > listeners = new ArrayList<>();

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

		Log.trace( "CrossNet", "Server listener added: " + listener.getClass().getName() );
	}

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

		Log.trace( "CrossNet", "Server listener removed: " + listener.getClass().getName() );
	}

	@Override
	public void connected( Connection connection ) {
		for ( Listener listener : this.listeners ) {
			listener.connected( connection );
		}
	}

	@Override
	public void disconnected( Connection connection ) {
		//TODO What to do about missing "removeConnection"
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
