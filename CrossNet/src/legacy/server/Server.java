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
public class Server implements Runnable {

	private final PacketFactory packetFactory;
	private final Selector selector;
	private final List< ClientConnection > connections = new ArrayList<>();

	private volatile boolean running = false;
	private volatile boolean shutdown = false;

	private ServerSocketChannel serverSocketChannel;

	public Server( PacketFactory packetFactory ) throws IOException {
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

		Server.serverStartedListening();
	}

	public void start( String name ) {
		if ( this.running ) {
			return;
		}

		new Thread( this, name ).start();
		this.running = true;
		Server.serverStarted();
	}

	public void stop() {
		if ( this.shutdown ) {
			return;
		}

		this.shutdown = true;
	}

	private void shutdown() {
		this.running = false;
		Server.serverShutDown();
	}

	@Override
	public void run() {
		// Run while not shut down
		this.shutdown = false;

		while ( !this.shutdown ) {
			try {
				for ( ClientConnection connection : this.connections ) {
					if ( connection.getSocketChannel().isConnected() && !connection.getSendQueue().isEmpty() ) {
						SelectionKey key = connection.getSocketChannel().keyFor( this.selector );
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
		ClientConnection connection = new ClientConnection( this, socketChannel, 1024 );
		this.connections.add( connection );

		// Notify
		Server.connectionConnected( connection );

		// Register for read events
		socketChannel.register( this.selector, SelectionKey.OP_READ, connection );
	}

	private void clientDisconnect( SelectionKey selectionKey ) {
		// Get Client object
		ClientConnection connection = (ClientConnection) selectionKey.attachment();

		// Remove references
		selectionKey.cancel();
		this.connections.remove( connection );
		try {
			connection.getSocketChannel().close();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Notify
		Server.connectionDisconnected( connection );
	}

	private void read( SelectionKey key ) {
		ClientConnection connection = (ClientConnection) key.attachment();
		ByteBuffer readBuffer = connection.getReadBuffer();
		SocketChannel socketChannel = connection.getSocketChannel();

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
			Server.receivedPacketFromConnection( connection, packet );
		}
	}

	private void write( SelectionKey key ) {
		ClientConnection connection = (ClientConnection) key.attachment();
		Queue< ByteBuffer > sendQueue = connection.getSendQueue();
		SocketChannel socketChannel = connection.getSocketChannel();

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

	public static void send( ClientConnection connection, Packet packet ) {
		connection.send( packet );
	}

	public void wakeup() {
		this.selector.wakeup();
	}

	protected static void connectionConnected( ClientConnection connection ) {
		Log.info( "Client connected: " + connection.getSocketChannel().socket().getRemoteSocketAddress() );

	}

	protected static void connectionDisconnected( ClientConnection connection ) {
		Log.info( "Client disconnected: " + connection.getSocketChannel().socket().getRemoteSocketAddress() );

	}

	protected static void receivedPacketFromConnection( ClientConnection connection, Packet packet ) {
		byte[] data = packet.getData();
		String string = new String( data );
		Log.info( "Received message from client: " + connection.getSocketChannel().socket().getRemoteSocketAddress() + " message: " + string );
	}

	protected static void serverStarted() {
		Log.info( "Server started" );
	}

	protected static void serverShutDown() {
		Log.info( "Server shut down" );
	}

	protected static void serverStartedListening() {
		Log.info( "Server started listening" );
	}
}
