package crossnet;

import java.io.IOException;
import java.net.Socket;
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

public class TcpTransportLayer extends TransportLayer {

	private SocketChannel socketChannel;
	private SelectionKey selectionKey;

	TcpTransportLayer( final Connection connection, MessageParser messageParser ) {
		super( connection, new LengthPacketFactory(), messageParser );
	}

	SelectionKey accept( Selector selector, SocketChannel socketChannel ) throws IOException {
		this.writeBuffer.clear();
		this.readBuffer.clear();

		try {
			this.socketChannel = socketChannel;
			this.socketChannel.configureBlocking( false );

			this.selectionKey = socketChannel.register( selector, SelectionKey.OP_READ );

			Socket socket = this.socketChannel.socket();
			Log.debug( "CrossNet", "Port " + socket.getLocalPort() + " connected to: " + socket.getRemoteSocketAddress() );

			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();

			return selectionKey;
		} catch ( IOException e ) {
			this.close();
			throw e;
		}

	}

	void connect( Selector selector, SocketAddress remoteAddress, int timeout ) throws IOException {
		this.close();
		this.writeBuffer.clear();
		this.readBuffer.clear();

		try {
			SocketChannel socketChannel = SocketChannel.open();

			//TODO: Connect non-blocking.
			// Connect blocking.
			Socket socket = socketChannel.socket();
			socket.connect( remoteAddress, timeout );
			socketChannel.configureBlocking( false );
			this.socketChannel = socketChannel;

			this.selectionKey = socketChannel.register( selector, SelectionKey.OP_READ );

			//TODO: This doesn't seem necessary.
			this.selectionKey.attach( this );

			Log.debug( "CrossNet", "Port " + socket.getLocalPort() + " connected to: " + socket.getRemoteSocketAddress() );

			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
		} catch ( IOException e ) {
			this.close();
			IOException ioException = new IOException( "Unable to connect to: " + remoteAddress );
			ioException.initCause( e );
			throw ioException;
		}

	}

	@Override
	public boolean needsKeepAlive( long time ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( this.keepAliveMillis > 0 && time - this.lastWriteTime > this.keepAliveMillis ) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isTimedOut( long time ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( this.timeoutMillis > 0 && time - this.lastReadTime > this.timeoutMillis ) {
			return true;
		}

		return false;
	}

	@Override
	public boolean needsPing( long time ) {
		if ( this.socketChannel == null ) {
			return false;
		}

		if ( this.pingMillis > 0 && time - this.pingSendTime > this.pingMillis ) {
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

	@Override
	public int send( Message message ) throws IOException {
		if ( this.socketChannel == null ) {
			throw new SocketException( "Connection is closed." );
		}

		Packet packet = null;
		try {
			packet = this.packetFactory.newPacket( message.getBytes() );
		} catch ( IllegalArgumentException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		synchronized ( this.writeLock ) {
			int start = this.writeBuffer.position();
			this.writeBuffer.put( packet.toBytes() );
			int end = this.writeBuffer.position();

			// Write to socket if nothing was queued.
			if ( start == 0 && !writeToSocket() ) {
				// The write was only partial.
				this.selectionKey.interestOps( SelectionKey.OP_READ | SelectionKey.OP_WRITE );
			} else {
				// Full write. Wake up selector such that idle event will fire.
				this.selectionKey.selector().wakeup();
			}

			if ( Log.DEBUG ) {
				float bufferLoad = this.writeBuffer.position() / (float) this.writeBuffer.capacity();
				if ( Log.DEBUG && bufferLoad > 0.75f ) {
					Log.debug( "CrossNet", this.connection + " write buffer is approaching capacity: " + bufferLoad );
				} else if ( Log.TRACE && bufferLoad > 0.25f ) {
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

		return this.messageParser.parseData( packet.getData() );
	}

	@Override
	void write() throws IOException {
		synchronized ( this.writeLock ) {
			if ( this.writeToSocket() ) {
				// Write successful, clear OP_WRITE.
				this.selectionKey.interestOps( SelectionKey.OP_READ );
			}
			this.lastWriteTime = System.currentTimeMillis();
		}
	}

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

}
