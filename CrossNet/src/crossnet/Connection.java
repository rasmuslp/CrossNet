package crossnet;

import java.io.IOException;

import crossnet.listener.ConnectionListener;
import crossnet.listener.ConnectionListenerHandler;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.crossnet.messages.KeepAliveMessage;
import crossnet.message.crossnet.messages.PingMessage;
import crossnet.message.crossnet.messages.TieredCrossNetMessage;

/**
 * A Connection between a {@link CrossNetClient} and a {@link CrossNetServer}.
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
	private ConnectionListenerHandler connectionListenerHandler = new ConnectionListenerHandler();

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

	@Override
	public String toString() {
		return "Connection " + this.id;
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
	 * For the {@link CrossNetClient}, this means that the registration procedure is completed.
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
	 *            {@code true} iff considered fully connected.
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
		this.connectionListenerHandler.addConnectionListener( connectionListener );
	}

	/**
	 * Removes a ConnectionListener.
	 * 
	 * @param connectionListener
	 *            The ConnectionListener to remove.
	 */
	void removeConnectionListener( ConnectionListener connectionListener ) {
		this.connectionListenerHandler.removeConnectionListener( connectionListener );
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

		this.connectionListenerHandler.connected( this );
	}

	/**
	 * Notify the {@link ConnectionListener}s of this, that it is now disconnected.
	 */
	void notifyDisconnected() {
		this.connectionListenerHandler.disconnected( this );
	}

	/**
	 * Notify the {@link ConnectionListener}s of this, that it is now idle.
	 */
	void notifyIdle() {
		this.connectionListenerHandler.idle( this );
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
		if ( message instanceof TieredCrossNetMessage ) {
			Log.debug( "CrossNet", this + " received: " + message.getMessageClass() );
		} else {
			Log.trace( "CrossNet", this + " received: " + message.getMessageClass() );
		}

		// Handle
		if ( message instanceof KeepAliveMessage ) {
			// Ignore
			return;
		} else if ( message instanceof PingMessage ) {
			if ( !this.transportLayer.gotPingMessage( (PingMessage) message ) ) {
				return;
			}
		}

		this.connectionListenerHandler.received( this, message );
	}

	/**
	 * Send a Message through this Connection.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 */
	public int send( Message message ) {
		if ( message == null ) {
			throw new IllegalArgumentException( "Cannot send null." );
		}

		try {
			int length = this.transportLayer.send( message );
			if ( length == 0 ) {
				Log.trace( "CrossNet", this + " had nothing to send." );
			} else if ( Log.DEBUG ) {
				if ( message instanceof TieredCrossNetMessage ) {
					Log.debug( "CrossNet", this + " sent: " + message.getMessageClass() + " (" + length + ")" );
				} else if ( Log.TRACE ) {
					Log.trace( "CrossNet", this + " sent: " + message.getMessageClass() + " (" + length + ")" );
				}
			}
			return length;
		} catch ( IOException ex ) {
			Log.debug( "CrossNet", "Unable to send with connection: " + this, ex );
			this.close();
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
