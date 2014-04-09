package crossnet;

import java.io.IOException;
import java.nio.ByteBuffer;

import crossnet.listener.Listener;
import crossnet.message.Message;
import crossnet.message.MessageParser;
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

	protected volatile long lastReadTime;
	protected volatile long lastWriteTime;

	public TransportLayer( final Connection connection, final PacketFactory packetFactory, final MessageParser messageParser ) {
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

	public abstract Message read() throws IOException;

	public abstract void write() throws IOException;

	public abstract void close();

}
