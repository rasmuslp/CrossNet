package crossnet;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.packet.Packet;
import crossnet.packet.length.LengthPacketFactory;
import crossnet.util.ByteArrayReader;

/**
 * TransportLayer for raw TCP socket.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class TcpTransportLayer extends TransportLayer {

	/**
	 * The communication channel.
	 */
	private SocketChannel socketChannel;

	/**
	 * The channel selection key.
	 */
	private SelectionKey selectionKey;

	TcpTransportLayer( final Connection connection, MessageParser messageParser ) {
		super( connection, new LengthPacketFactory(), messageParser );
	}

	@Override
	public boolean needsKeepAlive( long timestamp ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( ( this.keepAliveMillis > 0 ) && ( ( timestamp - this.lastWriteTime ) > this.keepAliveMillis ) ) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isTimedOut( long timestamp ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( ( this.timeoutMillis > 0 ) && ( ( timestamp - this.lastReadTime ) > this.timeoutMillis ) ) {
			return true;
		}

		return false;
	}

	@Override
	public boolean needsPing( long timestamp ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( ( this.pingMillis > 0 ) && ( ( timestamp - this.pingSendTime ) > this.pingMillis ) ) {
			return true;
		}

		return false;
	}

	@Override
	public String getRemoteAddress() {
		if ( this.socketChannel != null ) {
			if ( this.socketChannel.socket() != null ) {
				if ( this.socketChannel.socket().getRemoteSocketAddress() != null ) {
					return this.socketChannel.socket().getRemoteSocketAddress().toString();
				}
			}
		}

		return null;
	}

	/**
	 * Send a Message.
	 * <p>
	 * NB: This will block while waiting for lock. In the case that the send buffer is empty, it will also block while
	 * sending as much as possible.
	 * 
	 * @param message
	 *            The Message to send.
	 * @return The number of bytes added to the send buffer.
	 * @throws IOException
	 *             If unable to send.
	 */
	@Override
	public int send( Message message ) throws IOException {
		if ( this.socketChannel == null ) {
			throw new SocketException( "Connection is closed." );
		}

		Packet packet = null;
		try {
			packet = this.packetFactory.newPacket( message.getBytes() );
		} catch ( IllegalArgumentException e ) {
			throw new SocketException( "Could not form Packet: " + e.getMessage() );
		}

		synchronized ( this.writeLock ) {
			int start = this.writeBuffer.position();
			this.writeBuffer.put( packet.toBytes() );
			int end = this.writeBuffer.position();

			// Write to socket if nothing was queued.
			if ( ( start == 0 ) && !this.writeToSocket() ) {
				// The write was only partial.
				this.selectionKey.interestOps( SelectionKey.OP_READ | SelectionKey.OP_WRITE );
			} else {
				// Full write. Wake up selector such that idle event will fire.
				this.selectionKey.selector().wakeup();
			}

			if ( Log.DEBUG ) {
				float bufferLoad = this.writeBuffer.position() / (float) this.writeBuffer.capacity();
				if ( Log.DEBUG && ( bufferLoad > 0.75f ) ) {
					Log.debug( "CrossNet", this.connection + " write buffer is approaching capacity: " + bufferLoad );
				} else if ( Log.TRACE && ( bufferLoad > 0.25f ) ) {
					Log.trace( "CrossNet", this.connection + " write buffer load: " + bufferLoad );
				}
			}

			this.lastWriteTime = System.currentTimeMillis();

			return end - start;
		}
	}

	@Override
	Message read() throws IOException {
		if ( this.socketChannel == null ) {
			throw new SocketException( "Connection is closed." );
		}

		//TODO: Move some buffer handling from PacketFactory to here ?
		int bytesRead = this.socketChannel.read( this.readBuffer );
		if ( bytesRead == -1 ) {
			throw new SocketException( "Connection is closed." );
		}

		this.lastReadTime = System.currentTimeMillis();

		Packet packet = this.packetFactory.parseData( this.readBuffer );
		if ( packet == null ) {
			// Not enough data to form Packet.
			return null;
		}

		return this.messageParser.parseData( new ByteArrayReader( packet.getPayload() ) );
	}

	@Override
	void write() throws IOException {
		synchronized ( this.writeLock ) {
			if ( this.writeToSocket() ) {
				// Write completed. Clear OP_WRITE.
				this.selectionKey.interestOps( SelectionKey.OP_READ );
			}
			this.lastWriteTime = System.currentTimeMillis();
		}
	}

	@Override
	void close() {
		try {
			if ( this.socketChannel != null ) {
				this.socketChannel.close();
				this.socketChannel = null;
				if ( this.selectionKey != null ) {
					this.selectionKey.selector().wakeup();
				}
			}
		} catch ( IOException e ) {
			Log.debug( "CrossNet", "Unable to close connection.", e );
		}
	}

	/**
	 * Accept an incoming connection from a {@link CrossNetClient}. Used by the {@link CrossNetServer}.
	 * 
	 * @param selector
	 *            The selector the {@link #selectionKey} will be registered to.
	 * @param socketChannel
	 *            The communication channel of the incoming connection.
	 * @return The {@link #selectionKey} for this.
	 * @throws IOException
	 *             If an error occurs while establishing the connection.
	 */
	SelectionKey accept( Selector selector, SocketChannel socketChannel ) throws IOException {
		this.writeBuffer.clear();
		this.readBuffer.clear();

		try {
			this.socketChannel = socketChannel;
			this.socketChannel.configureBlocking( false );

			this.selectionKey = this.socketChannel.register( selector, SelectionKey.OP_READ );

			Log.debug( "CrossNet", "Port " + this.socketChannel.socket().getLocalPort() + " connected to: " + this.getRemoteAddress() );

			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();

			return this.selectionKey;
		} catch ( IOException e ) {
			this.close();
			throw e;
		}

	}

	/**
	 * Open a connection to a {@link CrossNetServer}. Used by the {@link CrossNetClient}.
	 * 
	 * @param selector
	 *            The selector the {@link #selectionKey} will be registered to.
	 * @param remoteAddress
	 *            The address to connect to.
	 * @throws IOException
	 *             If an error occurs while establishing the connection.
	 */
	void connect( Selector selector, SocketAddress remoteAddress ) throws IOException {
		this.close();
		this.writeBuffer.clear();
		this.readBuffer.clear();

		try {
			this.socketChannel = SocketChannel.open();

			// Connect blocking.
			this.socketChannel.socket().connect( remoteAddress, 5000 );
			this.socketChannel.configureBlocking( false );

			this.selectionKey = this.socketChannel.register( selector, SelectionKey.OP_READ );

			Log.debug( "CrossNet", "Port " + this.socketChannel.socket().getLocalPort() + " connected to: " + this.getRemoteAddress() );

			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
		} catch ( IOException e ) {
			this.close();
			IOException ioException = new IOException( "Unable to connect to: " + remoteAddress );
			ioException.initCause( e );
			throw ioException;
		}

	}

	/**
	 * Makes the actual write from the {@link TransportLayer#writeBuffer} to the {@link #socketChannel}.
	 * 
	 * @return {@code True} iff the {@link TransportLayer#writeBuffer} was emptied; i.e. no more data to send.
	 * @throws IOException
	 *             If unable to write.
	 */
	private boolean writeToSocket() throws IOException {
		if ( this.socketChannel == null ) {
			throw new SocketException( "Connection is closed" );
		}

		this.writeBuffer.flip();
		while ( this.writeBuffer.hasRemaining() ) {
			if ( this.socketChannel.write( this.writeBuffer ) == 0 ) {
				break;
			}
		}

		this.writeBuffer.compact();

		if ( this.writeBuffer.position() == 0 ) {
			// Wrote everything
			return true;
		}

		return false;
	}

}
