package crossnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import crossnet.listener.ConnectionListener;
import crossnet.listener.ConnectionListenerHandler;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.crossnet.messages.KeepAliveMessage;
import crossnet.message.crossnet.messages.RegisterMessage;

/**
 * Server for CrossNet.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class CrossNetServer extends LocalEndPoint {

	/**
	 * The Selector for the TCP Socket.
	 */
	private final Selector selector;

	/**
	 * Generates the IDs used to identify {@link Connection}s.
	 */
	private final ConnectionIDGenetator connectionIDGenetator = new ConnectionIDGenetator();

	/**
	 * Map of the current {@link Connection}s.
	 */
	protected Map< Integer, Connection > connections = new HashMap<>();

	/**
	 * The socket for incoming {@link Connection}s.
	 */
	private ServerSocketChannel serverSocketChannel;

	/**
	 * The CrossNetServer listener. Forwards all events.
	 */
	protected ConnectionListenerHandler connectionListenerHandler = new ConnectionListenerHandler() {

		@Override
		public void disconnected( Connection connection ) {
			// Remove the reference to the disconnected Connection.
			CrossNetServer.this.connections.remove( connection );
			super.disconnected( connection );
		}
	};

	public CrossNetServer() {
		try {
			this.selector = Selector.open();
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error opening Selector", e );
			throw new RuntimeException( "Error opening Selector", e );
		}
	}

	@Override
	public void start( String threadName ) {
		if ( this.threadRunning ) {
			Log.trace( "CrossNet", "Update thread already running." );
			return;
		}

		this.threadRunning = true;
		new Thread( this, threadName ).start();
	}

	@Override
	public void close() {
		if ( this.connections.size() > 0 ) {
			Log.info( "CrossNet", "Closing server connections..." );
		}

		Iterator< Connection > connectionIterator = this.connections.values().iterator();
		while ( connectionIterator.hasNext() ) {
			Connection connection = connectionIterator.next();
			connectionIterator.remove();
			connection.close();
		}

		if ( this.serverSocketChannel != null ) {
			try {
				this.serverSocketChannel.close();
				Log.info( "CrossNet", "Server closed." );
			} catch ( IOException e ) {
				Log.debug( "CrossNet", "Unable to close server.", e );
			}
			this.serverSocketChannel = null;
		}

		// Select one last time to complete closing the socket.
		synchronized ( this.updateLock ) {
			this.selector.wakeup();
			try {
				this.selector.selectNow();
			} catch ( IOException e ) {
			}
		}
	}

	@Override
	public void dispose() throws IOException {
		this.close();
		this.selector.close();
	}

	@Override
	public void addConnectionListener( ConnectionListener connectionListener ) {
		this.connectionListenerHandler.addConnectionListener( connectionListener );
		Log.trace( "CrossNet", "Server listener added." );
	}

	@Override
	public void removeConnectionListener( ConnectionListener connectionListener ) {
		this.connectionListenerHandler.removeConnectionListener( connectionListener );
		Log.trace( "CrossNet", "Server listener removed." );
	}

	@Override
	public void update( int timeout ) throws IOException {
		this.updateThread = Thread.currentThread();
		synchronized ( this.updateLock ) {
			// Block to avoid select while binding.
		}
		long updateTime = 0;
		int selects = 0;
		if ( 0 < timeout ) {
			updateTime = System.currentTimeMillis();
			selects = this.selector.select( timeout );
		} else {
			selects = this.selector.selectNow();
		}
		if ( selects == 0 ) {
			if ( ( updateTime > 0 ) && ( System.currentTimeMillis() < ( updateTime + ( timeout / 2 ) ) ) ) {
				Log.trace( "CrossNet", "Update got 0 selects rather quickly." );
			}
		} else {
			Log.trace( "CrossNet", "Update got " + selects + " select(s)." );
			Set< SelectionKey > selectedKeys = this.selector.selectedKeys();
			synchronized ( selectedKeys ) {
				Iterator< SelectionKey > keyIterator = selectedKeys.iterator();
				while ( keyIterator.hasNext() ) {
					this.keepAlive();

					SelectionKey key = keyIterator.next();
					keyIterator.remove();

					try {
						if ( key.isAcceptable() ) {
							this.accept( key );
						}
						if ( key.isReadable() ) {
							CrossNetServer.read( key );
						}
						if ( key.isWritable() ) {
							CrossNetServer.write( key );
						}
					} catch ( CancelledKeyException e ) {
						Connection connection = (Connection) key.attachment();
						if ( connection != null ) {
							connection.close();
						} else {
							key.cancel();
							key.channel().close();
						}
					}
				}
			}
		}

		long time = System.currentTimeMillis();
		for ( Connection connection : this.connections.values() ) {
			if ( connection.getTransportLayer().isTimedOut( time ) ) {
				Log.debug( "CrossNet", connection + " timed out." );
			} else {
				if ( connection.getTransportLayer().needsPing( time ) ) {
					connection.getTransportLayer().requestPingRoundTripTimeUpdate();
				} else if ( connection.getTransportLayer().needsKeepAlive( time ) ) {
					KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
					connection.send( keepAliveMessage );
				}
			}
			if ( connection.getTransportLayer().isIdle() ) {
				connection.notifyIdle();
			}
		}
	}

	/**
	 * Bind the CrossNetServer to a TCP port and start listening for new connections.
	 * 
	 * @param port
	 *            The TCP port on which to listen.
	 * @throws IOException
	 *             If the server could not bind correctly.
	 */
	public void bind( int port ) throws IOException {
		// Close open connections
		this.close();

		synchronized ( this.updateLock ) {
			this.selector.wakeup();

			try {
				// Create a non-blocking ServerSocketChannel.
				this.serverSocketChannel = ServerSocketChannel.open();
				this.serverSocketChannel.configureBlocking( false );

				// Bind ServerSocketChannel to port.
				InetSocketAddress inetSocketAddress = new InetSocketAddress( port );
				this.serverSocketChannel.bind( inetSocketAddress );

				// Register the ServerSocketChannel for accepting incoming connections.
				this.serverSocketChannel.register( this.selector, SelectionKey.OP_ACCEPT );

				Log.debug( "CrossNet", "Accepting connections on port: " + port );
			} catch ( IOException e ) {
				// Close open connections
				this.close();
				throw e;
			}
		}
		Log.info( "CrossNet", "CrossNetServer started listening" );
	}

	/**
	 * Checks if any {@link Connection} needs pinging.
	 * <p>
	 * Called by {@link #update(int)}.
	 */
	private void ping() {
		long time = System.currentTimeMillis();
		for ( Connection connection : this.connections.values() ) {
			if ( connection.getTransportLayer().needsPing( time ) ) {
				connection.getTransportLayer().requestPingRoundTripTimeUpdate();
			}
		}
	}

	/**
	 * Checks if any {@link Connection} needs keep alive.
	 * <p>
	 * Called by {@link #update(int)}.
	 */
	private void keepAlive() {
		long time = System.currentTimeMillis();
		for ( Connection connection : this.connections.values() ) {
			if ( connection.getTransportLayer().needsKeepAlive( time ) ) {
				KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
				connection.send( keepAliveMessage );
			}
		}
	}

	/**
	 * Accepts a new {@link Connection}.
	 * <p>
	 * Called by {@link #update(int)}.
	 * 
	 * @param key
	 *            The key that triggered the accept.
	 */
	private void accept( SelectionKey key ) {
		// New connection
		if ( this.serverSocketChannel == null ) {
			Log.trace( "CrossNet", "Unbound server can't accept incomming connections." );
			return;
		}
		try {
			// Accept the connection
			SocketChannel socketChannel = this.serverSocketChannel.accept();
			if ( socketChannel == null ) {
				Log.trace( "CrossNet", "Incomming connection's socket channel was null." );
				return;
			}

			// Create and initialise Connection
			Connection connection = this.newConnection();
			TransportLayer transportLayer = new TcpTransportLayer( connection, this.messageParser );
			connection.initialize( transportLayer );

			int id = this.connectionIDGenetator.getNextId();
			connection.setID( id );

			connection.addConnectionListener( this.connectionListenerHandler );

			// Make TCP accept and attach Connection to SelectionKey
			TcpTransportLayer tcpTransportLayer = (TcpTransportLayer) connection.getTransportLayer();
			SelectionKey selectionKey = tcpTransportLayer.accept( this.selector, socketChannel );
			selectionKey.attach( connection );
			connection.setConnected( true );

			// Store Connection
			this.connections.put( id, connection );

			// Start registration process
			RegisterMessage registerMessage = new RegisterMessage( connection.getID() );
			connection.send( registerMessage );

			// Notify
			connection.notifyConnected();
		} catch ( IOException e ) {
			Log.debug( "CrossNet", "Unable to accept incomming connection.", e );
		}
	}

	/**
	 * Reads from the {@link Connection}.
	 * <p>
	 * Called by {@link #update(int)}.
	 * 
	 * @param key
	 *            The key that triggered the read.
	 */
	private static void read( SelectionKey key ) {
		Connection connection = (Connection) key.attachment();

		if ( connection != null ) {
			try {
				// Read all the Messages !
				while ( true ) {
					Message message = connection.getTransportLayer().read();
					if ( message == null ) {
						// No more messages could be read.
						break;
					}
					connection.notifyReceived( message );
				}
			} catch ( IOException e ) {
				if ( Log.TRACE ) {
					Log.trace( "CrossNet", "Unable to read from connection: " + connection, e );
				} else if ( Log.DEBUG ) {
					Log.debug( "CrossNet", connection + " update: " + e.getMessage() );
				}
				connection.close();
			}
		} else {
			Log.error( "CrossNet", "CrossNetServer cannot write when Connection is null." );
		}
	}

	/**
	 * Writes to the {@link Connection}.
	 * <p>
	 * Called by {@link #update(int)}.
	 * 
	 * @param key
	 *            The key that triggered the write.
	 */
	private static void write( SelectionKey key ) {
		Connection connection = (Connection) key.attachment();

		if ( connection != null ) {
			try {
				connection.getTransportLayer().write();
			} catch ( IOException e ) {
				if ( Log.TRACE ) {
					Log.trace( "CrossNet", "Unable to write to connection: " + connection, e );
				} else if ( Log.DEBUG ) {
					Log.debug( "CrossNet", connection + " update: " + e.getMessage() );
				}
				connection.close();
			}
		} else {
			Log.error( "CrossNet", "CrossNetServer cannot write when Connection is null." );
		}
	}

	/**
	 * Gets a new Connection.
	 * <p>
	 * This construct allows the Connections used by this to be sub classed.
	 */
	@SuppressWarnings( "static-method" )
	protected Connection newConnection() {
		return new Connection();
	}

	/**
	 * Gets the current Connections.
	 * 
	 * @return The current Connections.
	 */
	public Map< Integer, Connection > getConnections() {
		return this.connections;
	}

	/**
	 * Broadcasts a Message to all Connections.
	 * 
	 * @param message
	 *            The Message to broadcast.
	 */
	public void sendToAll( Message message ) {
		for ( Connection connection : this.connections.values() ) {
			connection.send( message );
		}
	}

	/**
	 * Sends a Message to all Connections, except for the one with ID.
	 * 
	 * @param id
	 *            The ID to skip.
	 * @param message
	 *            The Message to send.
	 */
	public void sendToAllExcept( int id, Message message ) {
		for ( Connection connection : this.connections.values() ) {
			if ( connection.getID() == id ) {
				// Skip
				continue;
			}
			connection.send( message );
		}
	}

}
