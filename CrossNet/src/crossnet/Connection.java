package crossnet;

import java.io.IOException;

import crossnet.listener.ConnectionListener;
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
	 * Adds a ConnectionListener. A ConnectionListener cannot be added multiple times.
	 * 
	 * @param connectionListener
	 *            The ConnectionListener to add.
	 */
	void addConnectionListener( ConnectionListener connectionListener ) {
		this.listenerHandler.addConnectionListener( connectionListener );
	}

	/**
	 * Removes a ConnectionListener.
	 * 
	 * @param connectionListener
	 *            The ConnectionListener to remove.
	 */
	void removeConnectionListener( ConnectionListener connectionListener ) {
		this.listenerHandler.removeConnectionListener( connectionListener );
	}

	/**
	 * Notify the {@link ConnectionListener}s of this, that it is now connected.
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
	 * Notify the {@link ConnectionListener}s of this, that it is now disconnected.
	 */
	void notifyDisconnected() {
		this.listenerHandler.disconnected( this );
	}

	/**
	 * Notify the {@link ConnectionListener}s of this, that it is now idle.
	 */
	void notifyIdle() {
		this.listenerHandler.idle( this );
	}

	/**
	 * Notify the {@link ConnectionListener}s of this, that it received a Message.
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
			String messageClass = message.getClass().getSimpleName();
			if ( !( message instanceof FrameworkMessage ) ) {
				Log.debug( "CrossNet", this + " received: " + messageClass );
			} else if ( Log.TRACE ) {
				Log.trace( "CrossNet", this + " received: " + messageClass );
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
	 * If the Message is not a {@link FrameworkMessage}, it will be wrapped in a {@link DataMessage} for transportation.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 */
	public int send( Message message ) {
		if ( message == null ) {
			throw new IllegalArgumentException( "Cannot send null." );
		}

		String messageClass = message.getClass().getSimpleName();
		boolean wrapped = false;
		if ( !( message instanceof FrameworkMessage ) ) {
			// Wrap message in DataMessage
			byte[] messageData = message.getBytes();
			message = new DataMessage( messageData );
			wrapped = true;
		}

		try {
			int length = this.transportLayer.send( message );
			if ( length == 0 ) {
				Log.trace( "CrossNet", this + " had nothing to send." );
			} else if ( Log.DEBUG ) {
				if ( wrapped ) {
					Log.debug( "CrossNet", this + " sent: " + messageClass + " (" + length + ")" );
				} else if ( Log.TRACE ) {
					Log.trace( "CrossNet", this + " sent: " + messageClass + " (" + length + ")" );
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
