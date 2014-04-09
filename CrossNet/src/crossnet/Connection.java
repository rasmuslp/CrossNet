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

public class Connection {

	private int id = -1;

	private String name;

	private TransportLayer transportLayer;

	private volatile boolean isConnected;

	protected int pingMillis = 1000;
	protected int pingId = 0;
	protected long pingSendTime = 0;
	protected int pingRoundTripTime = 0;

	private ListenerHandler listenerHandler = new ListenerHandler();

	@SuppressWarnings( "hiding" )
	final void initialize( final TransportLayer transportLayer ) {
		this.transportLayer = transportLayer;
	}

	/**
	 * Sets the ID.
	 * 
	 * @param id
	 *            The new ID.
	 */
	void setID( final int id ) {
		this.id = id;
	}

	/**
	 * Gets the ID.
	 * 
	 * @return The ID.
	 */
	public int getID() {
		return this.id;
	}

	public int send( Message message ) {
		DataMessage dataMessage = new DataMessage( message.getBytes() );
		return this.sendInternal( dataMessage );
	}

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

	public void close() {
		boolean wasConnected = this.isConnected;
		this.setConnected( false );
		this.transportLayer.close();
		if ( wasConnected ) {
			this.notifyDisconnected();
			Log.info( "CrossNet", this + " disconnected." );
		}
	}

	public boolean needsPing( long time ) {
		if ( !this.isConnected() ) {
			return false;
		}

		if ( this.pingMillis > 0 && time - this.pingSendTime > this.pingMillis ) {
			return true;
		}

		return false;
	}

	/**
	 * Default is 1000. Set to 0 to disable.
	 * 
	 * @param pingMillis
	 */
	public void setPingMillis( int pingMillis ) {
		this.pingMillis = pingMillis;
	}

	public void updatePingRoundTripTime() {
		Message message = new PingMessage( this.pingId++ );
		this.pingSendTime = System.currentTimeMillis();
		this.sendInternal( message );
	}

	public int getPingRoundTripTime() {
		return this.pingRoundTripTime;
	}

	public TransportLayer getTransportLayer() {
		return this.transportLayer;
	}

	boolean isConnected() {
		return this.isConnected;
	}

	void setConnected( boolean isConnected ) {
		this.isConnected = isConnected;
	}

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

	void addListener( Listener listener ) {
		this.listenerHandler.addListener( listener );
	}

	void removeListener( Listener listener ) {
		this.listenerHandler.removeListener( listener );
	}

	void notifyConnected() {
		if ( Log.INFO ) {
			if ( this.transportLayer.getRemoteAddress() != null ) {
				Log.info( "CrossNet", this + " connected: " + this.transportLayer.getRemoteAddress() );
			}
		}

		this.listenerHandler.connected( this );
	}

	void notifyDisconnected() {
		this.listenerHandler.disconnected( this );
	}

	void notifyIdle() {
		this.listenerHandler.idle( this );
	}

	/**
	 * KeepAlive messages are filtered.
	 * 
	 * PingMessages are announced if they update the RTT of this.
	 * 
	 * @param message
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
			PingMessage pingMessage = (PingMessage) message;
			if ( pingMessage.isReply() ) {
				if ( pingMessage.getId() == ( this.pingId - 1 ) ) {
					// Update RTT
					this.pingRoundTripTime = (int) ( System.currentTimeMillis() - this.pingSendTime );
					Log.trace( "CrossNet", this + " round trip time: " + this.pingRoundTripTime );
				} else {
					// Ping is old, ignore
					return;
				}
			} else {
				// Otherwise, return to sender
				pingMessage.setReply();
				this.sendInternal( pingMessage );
				return;
			}
		}

		this.listenerHandler.received( this, message );
	}
}
