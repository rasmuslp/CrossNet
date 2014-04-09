package crossnet;

import java.io.IOException;
import java.nio.ByteBuffer;

import crossnet.listener.Listener;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.framework.messages.PingMessage;
import crossnet.packet.PacketFactory;

public abstract class TransportLayer {

	protected final Connection connection;
	protected final PacketFactory packetFactory;
	protected final MessageParser messageParser;

	protected final Object writeLock = new Object();

	protected final ByteBuffer readBuffer;
	protected final ByteBuffer writeBuffer;

	protected float idleThreshold = 0.1f;
	protected int keepAliveMillis = 10000;
	protected int timeoutMillis = 15000;

	protected int pingMillis = 1000;
	protected int pingId = 0;
	protected long pingSendTime = 0;
	protected int pingRoundTripTime = 0;

	protected volatile long lastReadTime;
	protected volatile long lastWriteTime;

	TransportLayer( final Connection connection, final PacketFactory packetFactory, final MessageParser messageParser ) {
		this.connection = connection;
		this.packetFactory = packetFactory;
		this.messageParser = messageParser;
		this.readBuffer = ByteBuffer.allocate( this.packetFactory.getMaxLength() );
		this.writeBuffer = ByteBuffer.allocate( this.packetFactory.getMaxLength() );
	}

	/** @see #setIdleThreshold(float) */
	public boolean isIdle() {
		return this.writeBuffer.position() / (float) this.writeBuffer.capacity() < this.idleThreshold;
	}

	/**
	 * If the percent of the TCP write buffer that is filled is less than the specified threshold,
	 * {@link Listener#idle(Connection)} will be called for each network thread update. Default is 0.1.
	 */
	public void setIdleThreshold( float idleThreshold ) {
		this.idleThreshold = idleThreshold;
	}

	public abstract boolean needsKeepAlive( long time );

	/**
	 * Default is 10000. Set to 0 to disable.
	 * 
	 * @param keepAliveMillis
	 */
	public void setKeepAlive( int keepAliveMillis ) {
		this.keepAliveMillis = keepAliveMillis;
	}

	public abstract boolean isTimedOut( long time );

	/**
	 * Default is 15000. Set to 0 to disable.
	 * 
	 * @param timeoutMillis
	 */
	public void setTimeout( int timeoutMillis ) {
		this.timeoutMillis = timeoutMillis;
	}

	public abstract boolean needsPing( long time );

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
		this.connection.sendInternal( message );
	}

	public void updatePingRTT( PingMessage pingMessage ) {
		if ( pingMessage.isReply() ) {
			if ( pingMessage.getId() == ( this.pingId - 1 ) ) {
				// Update RTT
				this.pingRoundTripTime = (int) ( System.currentTimeMillis() - this.pingSendTime );
				Log.trace( "CrossNet", this.connection + " round trip time: " + this.pingRoundTripTime );
			} else {
				// Ping is old, ignore
				return;
			}
		} else {
			// Otherwise, return to sender
			pingMessage.setReply();
			this.connection.sendInternal( pingMessage );
			return;
		}
	}

	public int getPingRoundTripTime() {
		return this.pingRoundTripTime;
	}

	/**
	 * May return null.
	 * 
	 * @return
	 */
	public abstract String getRemoteAddress();

	/**
	 * 
	 * @param message
	 * @return The number of bytes added to the send (write) buffer.
	 * @throws IOException
	 */
	public abstract int send( Message message ) throws IOException;

	abstract Message read() throws IOException;

	abstract void write() throws IOException;

	abstract void close();

}
