package crossnet;

import java.io.IOException;
import java.nio.ByteBuffer;

import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.crossnet.messages.KeepAliveMessage;
import crossnet.message.crossnet.messages.PingMessage;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;

/**
 * The part of a {@link Connection} that handles the actual transport of data.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class TransportLayer {

	/**
	 * The Connection this is a TransportLayer for.
	 */
	protected final Connection connection;

	/**
	 * The PacketFactory used to parse to and from {@link Packet}s.
	 */
	protected final PacketFactory packetFactory;

	/**
	 * The MessageParser used to parse byte arrays to {@link Message}s.
	 */
	protected final MessageParser messageParser;

	/**
	 * The lock used to guard the {@link #writeBuffer}.
	 */
	protected final Object writeLock = new Object();

	/**
	 * The read / receive buffer.
	 */
	protected final ByteBuffer readBuffer;

	/**
	 * The write / send buffer.
	 */
	protected final ByteBuffer writeBuffer;

	/**
	 * Time stamp from the last read.
	 */
	protected volatile long lastReadTime;

	/**
	 * Time stamp from the last write.
	 */
	protected volatile long lastWriteTime;

	/**
	 * {@link Connection} is considered idle when the {@link #writeBuffer} utilisation is below this threshold.
	 * <p>
	 * Default is 10%.
	 */
	protected float idleThreshold = 0.1f;

	/**
	 * Maximum time between {@link KeepAliveMessage}s should be sent.
	 * <p>
	 * Default is 10000 milliseconds.
	 */
	protected int keepAliveMillis = 10000;

	/**
	 * {@link Connection} is considered timed out when there hasn't been any reads within this time limit.
	 * <p>
	 * Default is 15000 milliseconds.
	 */
	protected int timeoutMillis = 15000;

	/**
	 * Maximum time between {@link PingMessage}s should be sent.
	 * <p>
	 * Default is 1000 milliseconds.
	 */
	protected int pingMillis = 1000;

	/**
	 * ID of last {@link PingMessage} sent.
	 */
	protected int pingId = 0;

	/**
	 * Time stamp from when the last {@link PingMessage} was sent.
	 */
	protected long pingSendTime = 0;

	/**
	 * Last recorded round trip time.
	 */
	protected int pingRoundTripTime = 0;

	TransportLayer( final Connection connection, final PacketFactory packetFactory, final MessageParser messageParser ) {
		this.connection = connection;
		this.packetFactory = packetFactory;
		this.messageParser = messageParser;
		this.readBuffer = ByteBuffer.allocate( this.packetFactory.getMaxPacketSize() );
		this.writeBuffer = ByteBuffer.allocate( this.packetFactory.getMaxPacketSize() );
	}

	/**
	 * Determine if {@link Connection} is idle.
	 * 
	 * @see #idleThreshold
	 * @return {@code True} iff the Connection is idle.
	 */
	public boolean isIdle() {
		return this.writeBuffer.position() / (float) this.writeBuffer.capacity() < this.idleThreshold;
	}

	/**
	 * Sets the threshold of when to consider the {@link Connection} idle.
	 * 
	 * @see #idleThreshold
	 * @param idleThreshold
	 *            The new threshold.
	 */
	public void setIdleThreshold( float idleThreshold ) {
		this.idleThreshold = idleThreshold;
	}

	/**
	 * Determine if {@link Connection} needs a {@link KeepAliveMessage} sent.
	 * 
	 * @param timestamp
	 *            The time stamp to compare to. Should be now.
	 * @return {@code True} iff keep alive is needed.
	 */
	public abstract boolean needsKeepAlive( long timestamp );

	/**
	 * Sets the maximum time between {@link KeepAliveMessage}s should be sent.
	 * <p>
	 * Set to 0 to disable.
	 * 
	 * @see #keepAliveMillis
	 * @param keepAliveMillis
	 *            The new time limit.
	 */
	public void setKeepAlive( int keepAliveMillis ) {
		this.keepAliveMillis = keepAliveMillis;
	}

	/**
	 * Determine if {@link Connection} is timed out.
	 * 
	 * @param timestamp
	 *            The time stamp to compare to. Should be now.
	 * @return {@code True} iff timed out.
	 */
	public abstract boolean isTimedOut( long timestamp );

	/**
	 * Sets the time out limit.
	 * <p>
	 * Set to 0 to disable.
	 * 
	 * @see #timeoutMillis
	 * @param timeoutMillis
	 *            The new time limit.
	 */
	public void setTimeout( int timeoutMillis ) {
		this.timeoutMillis = timeoutMillis;
	}

	/**
	 * Determine if {@link Connection} needs a {@link PingMessage} sent.
	 * 
	 * @param timestamp
	 *            The time stamp to compare to. Should be now.
	 * @return {@code True} iff needs ping.
	 */
	public abstract boolean needsPing( long timestamp );

	/**
	 * Sets the maximum time between {@link PingMessage}s should be sent.
	 * <p>
	 * Set to 0 to disable.
	 * 
	 * @see #pingMillis
	 * @param pingMillis
	 *            The new time limit.
	 */
	public void setPing( int pingMillis ) {
		this.pingMillis = pingMillis;
	}

	/**
	 * Requests an update of the ping by sending a new {@link PingMessage}.
	 */
	void requestPingRoundTripTimeUpdate() {
		Message message = new PingMessage( this.pingId++ );
		this.pingSendTime = System.currentTimeMillis();
		this.connection.send( message );
	}

	/**
	 * If this is the answer to a request, the {@link #pingRoundTripTime} is updated. Otherwise, return to sender.
	 * 
	 * @param pingMessage
	 *            The PingMessage received.
	 */
	void gotPingMessage( PingMessage pingMessage ) {
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
			this.connection.send( pingMessage );
			return;
		}
	}

	/**
	 * Gets the last recorded {@link #pingRoundTripTime}.
	 * 
	 * @return the last recorded ping round trip time.
	 */
	public int getPingRoundTripTime() {
		return this.pingRoundTripTime;
	}

	/**
	 * Gets the address of what this is connected to.
	 * 
	 * @return The address of what this is connected to. May return {@code null} if not connected or an error occurs.
	 */
	public abstract String getRemoteAddress();

	/**
	 * Send a Message.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 * @throws IOException
	 *             If unable to send.
	 */
	abstract int send( Message message ) throws IOException;

	/**
	 * Reads a Message.
	 * 
	 * @return A new Message or null if not enough available data.
	 * @throws IOException
	 *             If unable to read.
	 */
	abstract Message read() throws IOException;

	/**
	 * Writes data from the {@link #writeBuffer}.
	 * 
	 * @throws IOException
	 *             If unable to write.
	 */
	abstract void write() throws IOException;

	/**
	 * Close.
	 */
	abstract void close();

}
