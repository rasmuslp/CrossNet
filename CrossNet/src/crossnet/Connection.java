package crossnet;

import java.io.IOException;

import crossnet.listener.Listener;
import crossnet.listener.ListenerHandler;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.messages.DataMessage;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.PingMessage;

/**
 * A Connection between a {@link Client} and a {@link Server}.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class Connection {

	/**
	 * A unique ID.
	 */
	private int id = -1;

	/**
	 * A human readable name.
	 */
	private String name;

	/**
	 * The TransportLayer utilised.
	 */
	private TransportLayer transportLayer;

	/**
	 * {@code True} iff connected.
	 */
	private volatile boolean isConnected;

	/**
	 * The Connection listener. Forwards all events.
	 */
	private ListenerHandler listenerHandler = new ListenerHandler();

	@SuppressWarnings( "hiding" )
	final void initialize( final TransportLayer transportLayer ) {
		this.transportLayer = transportLayer;
	}

	/**
	 * Sets the unique ID.
	 * 
	 * @param id
	 *            The new unique ID.
	 */
	void setID( final int id ) {
		this.id = id;
	}

	/**
	 * Gets the unique ID.
	 * 
	 * @return The unique ID.
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Set a human readable name.
	 * 
	 * @param name
	 *            The new name.
	 */
	public void setName( String name ) {
		this.name = name;
	}

	@Override
	public String toString() {
		if ( this.name == null ) {
			return "Connection " + this.id;
		}

		return this.name;
	}

	/**
	 * Get the TransportLayer utilised.
	 * 
	 * @return The TransportLayer utilised.
	 */
	public TransportLayer getTransportLayer() {
		return this.transportLayer;
	}

	/**
	 * Will return {@code true} iff the Connection is considered fully connected.
	 * <p>
	 * For the {@link Client}, this means that the registration procedure is completed.
	 * 
	 * @return {@code True} iff the Connection is considered fully connected.
	 */
	boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * Set the connected status.
	 * 
	 * @param isConnected
	 *            {@link true} iff considered fully connected.
	 */
	void setConnected( boolean isConnected ) {
		this.isConnected = isConnected;
	}

	/**
	 * Adds a Listener. A Listener cannot be added multiple times.
	 * 
	 * @param listener
	 *            The Listener to add.
	 */
	void addListener( Listener listener ) {
		this.listenerHandler.addListener( listener );
	}

	/**
	 * Removes a Listener.
	 * 
	 * @param listener
	 *            The Listener to remove.
	 */
	void removeListener( Listener listener ) {
		this.listenerHandler.removeListener( listener );
	}

	/**
	 * Notify the {@link Listener}s of this, that it is now connected.
	 */
	void notifyConnected() {
		if ( Log.INFO ) {
			if ( this.transportLayer.getRemoteAddress() != null ) {
				Log.info( "CrossNet", this + " connected: " + this.transportLayer.getRemoteAddress() );
			}
		}

		this.listenerHandler.connected( this );
	}

	/**
	 * Notify the {@link Listener}s of this, that it is now disconnected.
	 */
	void notifyDisconnected() {
		this.listenerHandler.disconnected( this );
	}

	/**
	 * Notify the {@link Listener}s of this, that it is now idle.
	 */
	void notifyIdle() {
		this.listenerHandler.idle( this );
	}

	/**
	 * Notify the {@link Listener}s of this, that it received a Message.
	 * <p>
	 * KeepAlive messages are filtered.
	 * <p>
	 * PingMessages are announced if they update the RTT of this.
	 * 
	 * @param message
	 *            The Message received.
	 */
	void notifyReceived( Message message ) {
		// Log
		if ( Log.DEBUG ) {
			String objectString = message.getClass().getSimpleName();
			if ( !( message instanceof FrameworkMessage ) ) {
				Log.debug( "CrossNet", this + " received: " + objectString );
			} else if ( Log.TRACE ) {
				Log.trace( "CrossNet", this + " received: " + objectString );
			}
		}

		// Handle
		if ( message instanceof KeepAliveMessage ) {
			// Ignore
			return;
		} else if ( message instanceof PingMessage ) {
			this.transportLayer.gotPingMessage( (PingMessage) message );
		}

		this.listenerHandler.received( this, message );
	}

	/**
	 * Send a Message through this Connection.
	 * <p>
	 * This Message will be wrapped in a {@link DataMessage} for transportation.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 */
	public int send( Message message ) {
		DataMessage dataMessage = new DataMessage( message.getBytes() );
		return this.sendInternal( dataMessage );
	}

	/**
	 * Send a Message through this Connection.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 */
	int sendInternal( Message message ) {
		if ( message == null ) {
			throw new IllegalArgumentException( "Cannot send null." );
		}

		try {
			int length = this.transportLayer.send( message );
			if ( length == 0 ) {
				Log.trace( "CrossNet", this + " had nothing to send." );
			} else if ( Log.DEBUG ) {
				String messageString = message.getClass().getSimpleName();
				if ( !( message instanceof FrameworkMessage ) ) {
					Log.debug( "CrossNet", this + " sent: " + messageString + " (" + length + ")" );
				} else if ( Log.TRACE ) {
					Log.trace( "CrossNet", this + " sent: " + messageString + " (" + length + ")" );
				}
			}
			return length;
		} catch ( IOException ex ) {
			Log.debug( "CrossNet", "Unable to send with connection: " + this, ex );
			close();
			return 0;
		}
	}

	/**
	 * Close the {@link Connection}.
	 */
	public void close() {
		boolean wasConnected = this.isConnected;
		this.setConnected( false );
		this.transportLayer.close();
		if ( wasConnected ) {
			this.notifyDisconnected();
			Log.info( "CrossNet", this + " disconnected." );
		}
	}
}
