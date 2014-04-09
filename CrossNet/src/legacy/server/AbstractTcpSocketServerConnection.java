package legacy.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;

/**
 * SERVER
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class AbstractTcpSocketServerConnection implements ServerConnection {

	private final PacketFactory packetFactory;
	private final Selector selector;
	private final List< TcpSocketClient > clients = new ArrayList<>();

	private volatile boolean running = false;
	private volatile boolean shutdown = false;

	private ServerSocketChannel serverSocketChannel;

	public AbstractTcpSocketServerConnection( PacketFactory packetFactory ) throws IOException {
		this.packetFactory = packetFactory;

		// Create Selector
		this.selector = Selector.open();
	}

	public void bind( int port ) throws IOException {
		//TODO: Close current connection

		// Create a non-blocking ServerSocketChannel.
		this.serverSocketChannel = ServerSocketChannel.open();
		this.serverSocketChannel.configureBlocking( false );

		// Bind ServerSocketChannel to port.
		InetSocketAddress inetSocketAddress = new InetSocketAddress( port );
		this.serverSocketChannel.bind( inetSocketAddress );

		// Register the ServerSocketChannel for accepting incoming connections.
		this.serverSocketChannel.register( this.selector, SelectionKey.OP_ACCEPT );

		// Wake selector to immediately be able to accept new connections.
		this.selector.wakeup();

		this.serverStartedListening();
	}

	public void start( String name ) {
		if ( this.running ) {
			return;
		}

		new Thread( this, name ).start();
		this.running = true;
		this.serverStarted();
	}

	public void stop() {
		if ( this.shutdown ) {
			return;
		}

		this.shutdown = true;
	}

	private void shutdown() {
		this.running = false;
		this.serverShutDown();
	}

	@Override
	public void run() {
		// Run while not shut down
		this.shutdown = false;

		while ( !this.shutdown ) {
			try {
				for ( TcpSocketClient client : this.clients ) {
					if ( client.getSocketChannel().isConnected() && !client.getSendQueue().isEmpty() ) {
						SelectionKey key = client.getSocketChannel().keyFor( this.selector );
						key.interestOps( SelectionKey.OP_WRITE );
					}
				}

				// Wait for an event on one of the registered channels
				this.selector.select( 100 );

				Iterator< SelectionKey > selectedKeys = this.selector.selectedKeys().iterator();
				while ( selectedKeys.hasNext() ) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if ( !key.isValid() ) {
						continue;
					}

					if ( key.isAcceptable() ) {
						this.accept( key );
					} else if ( key.isReadable() ) {
						this.read( key );
					} else if ( key.isWritable() ) {
						this.write( key );
					}
				}

			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}

		// Thread ending.
		this.shutdown();
	}

	private void accept( SelectionKey key ) throws IOException {
		// Get the corresponding ServerSocketChannel
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking( false );

		// Create and store Client
		TcpSocketClient client = new TcpSocketClient( this, socketChannel, 1024 );
		this.clients.add( client );

		// Notify
		this.clientConnected( client );

		// Register for read events
		socketChannel.register( this.selector, SelectionKey.OP_READ, client );
	}

	private void clientDisconnect( SelectionKey selectionKey ) {
		// Get Client object
		TcpSocketClient client = (TcpSocketClient) selectionKey.attachment();

		// Remove references
		selectionKey.cancel();
		this.clients.remove( client );
		try {
			client.getSocketChannel().close();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Notify
		this.clientDisconnected( client );
	}

	private void read( SelectionKey key ) {
		TcpSocketClient client = (TcpSocketClient) key.attachment();
		ByteBuffer readBuffer = client.getReadBuffer();
		SocketChannel socketChannel = client.getSocketChannel();

		try {
			int bytesRead = socketChannel.read( readBuffer );
			if ( bytesRead == -1 ) {
				// Client closed connection cleanly.
				this.clientDisconnect( key );
				return;
			}
			Log.info( "Server read " + bytesRead + " B" );
		} catch ( IOException e ) {
			// Client closed connection forcibly.
			this.clientDisconnect( key );
			return;
		}

		List< Packet > packetsRead = this.packetFactory.parseDataList( readBuffer );

		// Notify
		for ( Packet packet : packetsRead ) {
			this.receivedPacketFromClient( client, packet );
		}
	}

	private void write( SelectionKey key ) {
		TcpSocketClient client = (TcpSocketClient) key.attachment();
		Queue< ByteBuffer > sendQueue = client.getSendQueue();
		SocketChannel socketChannel = client.getSocketChannel();

		try {
			// Send a batch of data
			while ( !sendQueue.isEmpty() ) {
				// Write to channel
				ByteBuffer data = sendQueue.peek();
				int bytesWritten = socketChannel.write( data );
				if ( bytesWritten == -1 ) {
					// Client closed connection cleanly.
					this.clientDisconnect( key );
					return;
				}
				Log.info( "Server wrote " + bytesWritten + " B" );

				if ( data.remaining() > 0 ) {
					// Channel buffer must be full, break out and do other stuff.
					break;
				}

				// If we got this far, the current buffer was completely written and can be removed.
				sendQueue.poll();
			}
		} catch ( IOException e ) {
			// Client closed connection forcibly.
			this.clientDisconnect( key );
			return;
		}

		// If the queue is empty all the data has been written. Switch back to waiting for data to be read.
		if ( sendQueue.isEmpty() ) {
			key.interestOps( SelectionKey.OP_READ );
		}

	}

	@Override
	public void send( Client client, Packet packet ) {
		client.send( packet );
	}

	public void wakeup() {
		this.selector.wakeup();
	}

	protected abstract void serverStarted();

	protected abstract void serverShutDown();

	protected abstract void serverStartedListening();

	protected abstract void clientConnected( TcpSocketClient client );

	protected abstract void clientDisconnected( TcpSocketClient client );

	protected abstract void receivedPacketFromClient( TcpSocketClient client, Packet packet );
}
