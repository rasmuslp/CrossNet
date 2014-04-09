package legacy.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;

/**
 * CLIENT
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class TcpSocketClientConnection implements ClientConnection {

	protected final static int LOBBY_PORT = 3000;
	protected final static int ASSET_PORT = 3001;
	protected final static int GAME_PORT = 3002;

	private final PacketFactory packetFactory;

	/**
	 * The address to connect to.
	 */
	private final InetAddress hostAddress;

	private SocketChannel lobbyChannel;

	private Selector selector;

	private ByteBuffer readBuffer = ByteBuffer.allocate( 1024 );

	private Queue< ByteBuffer > sendQueue = new ConcurrentLinkedQueue<>();

	public TcpSocketClientConnection( PacketFactory packetFactory, InetAddress hostAddress ) throws IOException {
		this.packetFactory = packetFactory;
		this.hostAddress = hostAddress;

		// Create Selector
		this.selector = Selector.open();
	}

	@Override
	public void run() {
		try {
			this.start();
		} catch ( IOException e ) {
			//TODO: Improve
			throw new RuntimeException( e );
		}

		while ( true ) {
			try {
				// Register for writes if sendQueue is non-empty
				if ( this.lobbyChannel.isConnected() && !this.sendQueue.isEmpty() ) {
					SelectionKey key = this.lobbyChannel.keyFor( this.selector );
					key.interestOps( SelectionKey.OP_WRITE );
				}

				// Wait for an event on one of the registered channels
				this.selector.select( 100 );

				Iterator< SelectionKey > selectedKeys = this.selector.selectedKeys().iterator();
				while ( selectedKeys.hasNext() ) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					// Discard invalid keys
					if ( !key.isValid() ) {
						continue;
					}

					// Handle event
					if ( key.isConnectable() ) {
						this.completeConnection( key );
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
	}

	private void start() throws IOException {
		// Create a non-blocking SocketChannel
		this.lobbyChannel = SocketChannel.open();
		this.lobbyChannel.configureBlocking( false );

		// Start connecting
		InetSocketAddress inetSocketAddress = new InetSocketAddress( this.hostAddress, LOBBY_PORT );
		this.lobbyChannel.connect( inetSocketAddress );

		// Register the SocketChannel for connection completion
		this.lobbyChannel.register( this.selector, SelectionKey.OP_CONNECT );

		Log.info( "Client started." );
	}

	private void completeConnection( SelectionKey key ) {
		try {
			while ( !this.lobbyChannel.finishConnect() ) {
				Log.info( "Waiting for connection to be stablished" );
			}

			// Prepare for reading
			key.interestOps( SelectionKey.OP_READ );
		} catch ( IOException e ) {
			// Cancel registration of this key, as the connection now looks invalid.
			key.cancel();

			Log.error( "Error occured when completing connection to server:", e );

			return;
		}

		Log.info( "Client connected to server" );
	}

	private void read( SelectionKey key ) {
		try {
			int bytesRead = this.lobbyChannel.read( this.readBuffer );
			if ( bytesRead == -1 ) {
				// Server closed the connection cleanly.
				this.lobbyChannel.close();
				key.cancel();
				return;
				//TODO: Client dies this way..
			}
			Log.info( "Client read " + bytesRead + " B" );
		} catch ( IOException e ) {
			// Cancel registration of this key, as the connection now looks invalid.
			key.cancel();

			Log.error( "Error occured while writing to server:", e );

			//TODO: Client dies this way..
			return;
		}

		List< Packet > packetsRead = this.packetFactory.parseDataList( this.readBuffer );

		// TODO: Notify / send somewhere
		for ( Packet packet : packetsRead ) {
			byte[] data = packet.getData();
			String string = new String( data );
			Log.info( string );
			// Echoing as a start
			//this.send( message );
		}
	}

	private void write( SelectionKey key ) {
		try {
			// Send a batch of data
			while ( !this.sendQueue.isEmpty() ) {
				// Write to channel
				ByteBuffer data = this.sendQueue.peek();
				int bytesWritten = this.lobbyChannel.write( data );
				if ( bytesWritten == -1 ) {
					// Server closed the connection cleanly.
					this.lobbyChannel.close();
					key.cancel();
					return;
					//TODO: Client dies this way..
				}

				if ( data.remaining() > 0 ) {
					// Channel buffer must be full, break out and do other stuff.
					break;
				}

				// If we got this far, the current buffer was completely written and can be removed.
				this.sendQueue.poll();
			}
		} catch ( IOException e ) {
			// Cancel registration of this key, as the connection now looks invalid.
			key.cancel();

			Log.error( "Error occured while writing to server:", e );

			//TODO: Client dies this way..
			return;
		}

		// If the queue is empty all the data has been written. Switch back to waiting for data to be read.
		if ( this.sendQueue.isEmpty() ) {
			key.interestOps( SelectionKey.OP_READ );
		}
	}

	@Override
	public void send( Packet packet ) {
		ByteBuffer writeBuffer = ByteBuffer.wrap( packet.getData() );
		this.sendQueue.add( writeBuffer );
		this.selector.wakeup();
	}

}
