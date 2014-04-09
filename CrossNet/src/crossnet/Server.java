package crossnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import crossnet.listener.Listener;
import crossnet.listener.ListenerHandler;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.RegisterMessage;
import crossnet.packet.PacketFactory;

public class Server extends LocalEndPoint {

	private final PacketFactory packetFactory;
	private final MessageParser messageParser;

	private final Selector selector;

	private final ConnectionIDGenetator connectionIDGenetator = new ConnectionIDGenetator();

	//TODO: Use a Map of ID -> Connection instead ?
	private List< Connection > connections = new ArrayList<>();

	private ServerSocketChannel serverSocketChannel;

	private ListenerHandler listenerHandler = new ListenerHandler() {

		@Override
		public void disconnected( Connection connection ) {
			Server.this.connections.remove( connection );
			for ( Listener listener : this.listeners ) {
				listener.disconnected( connection );
			}
		}
	};

	public Server( final PacketFactory packetFactory, final MessageParser messageParser ) {
		//TODO: This should be hardcoded based on transport layer ?
		this.packetFactory = packetFactory;
		this.messageParser = messageParser;

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

		Iterator< Connection > connectionIterator = this.connections.iterator();
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
	public void addListener( Listener listener ) {
		this.listenerHandler.addListener( listener );
		Log.trace( "CrossNet", "Server listener added." );
	}

	@Override
	public void removeListener( Listener listener ) {
		this.listenerHandler.removeListener( listener );
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
			if ( updateTime > 0 && ( System.currentTimeMillis() < ( updateTime + ( timeout / 2 ) ) ) ) {
				Log.trace( "CrossNet", "Update got 0 selects rather quickly." );
			}
		} else {
			Log.trace( "CrossNet", "Update got " + selects + " select(s)." );
			Set< SelectionKey > selectedKeys = this.selector.selectedKeys();
			synchronized ( selectedKeys ) {
				Iterator< SelectionKey > keyIterator = selectedKeys.iterator();
				while ( keyIterator.hasNext() ) {
					this.ping();
					this.keepAlive();

					SelectionKey key = keyIterator.next();
					keyIterator.remove();

					try {
						if ( key.isAcceptable() ) {
							this.accept( key );
						} else if ( key.isReadable() ) {
							this.read( key );
						} else if ( key.isWritable() ) {
							this.write( key );
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
		for ( Connection connection : this.connections ) {
			if ( connection.getTransportLayer().isTimedOut( time ) ) {
				Log.debug( "CrossNet", connection + " timed out." );
			} else {
				if ( connection.needsPing( time ) ) {
					connection.updatePingRoundTripTime();
				} else if ( connection.getTransportLayer().needsKeepAlive( time ) ) {
					KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
					connection.sendInternal( keepAliveMessage );
				}
			}
			if ( connection.getTransportLayer().isIdle() ) {
				connection.notifyIdle();
			}
		}
	}

	/**
	 * Bind the Server to a TCP port and start listening.
	 * 
	 * @param port
	 *            The TCP port on which to listen for new connections.
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
		Log.info( "CrossNet", "Server started listening" );
	}

	private void ping() {
		long time = System.currentTimeMillis();
		for ( Connection connection : this.connections ) {
			if ( connection.needsPing( time ) ) {
				connection.updatePingRoundTripTime();
			}
		}
	}

	private void keepAlive() {
		long time = System.currentTimeMillis();
		for ( Connection connection : this.connections ) {
			if ( connection.getTransportLayer().needsKeepAlive( time ) ) {
				KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
				connection.sendInternal( keepAliveMessage );
			}
		}
	}

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

			// Create and initialze Connection
			Connection connection = this.newConnection();
			TransportLayer transportLayer = new TcpTransportLayer( this.packetFactory, this.messageParser );
			connection.initialize( transportLayer );
			int id = this.connectionIDGenetator.getNextConnectionID();
			connection.setID( id );

			// Make TCP accept and attach Connection to SelectionKey
			TcpTransportLayer tcpTransportLayer = (TcpTransportLayer) connection.getTransportLayer();
			SelectionKey selectionKey = tcpTransportLayer.accept( this.selector, socketChannel );
			selectionKey.attach( connection );

			connection.setConnected( true );
			connection.addListener( this.listenerHandler );

			// Store Connection
			this.connections.add( connection );

			// Start registration process
			RegisterMessage registerMessage = new RegisterMessage( connection.getID() );
			connection.sendInternal( registerMessage );

			// Notify
			connection.notifyConnected();
		} catch ( IOException e ) {
			Log.debug( "CrossNet", "Unable to accept incomming connection.", e );
		}
	}

	private void read( SelectionKey key ) {
		Connection connection = (Connection) key.attachment();

		if ( connection != null ) {
			try {
				while ( true ) {
					Message message = connection.getTransportLayer().read( connection );
					if ( message == null ) {
						// No more messages could be read.
						break;
					}

					//TODO Review this.
					if ( Log.DEBUG ) {
						String objectString = message.getClass().getSimpleName();
						if ( !( message instanceof FrameworkMessage ) ) {
							Log.debug( "CrossNet", connection + " received: " + objectString );
						} else if ( Log.TRACE ) {
							Log.trace( "CrossNet", connection + " received: " + objectString );
						}
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
			Log.error( "CrossNet", "Server cannot write when Connection is null." );
		}
	}

	private void write( SelectionKey key ) {
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
			Log.error( "CrossNet", "Server cannot write when Connection is null." );
		}
	}

	/**
	 * Allows the Connections used by this to be subclassed.
	 */
	protected Connection newConnection() {
		return new Connection();
	}

	public List< Connection > getConnections() {
		return this.connections;
	}

	public void send( int connectionID, Message message ) {
		for ( Connection connection : this.connections ) {
			if ( connection.getID() == connectionID ) {
				connection.send( message );
				break;
			}
		}
	}

	public void sendToAllExcept( int connectionID, Message message ) {
		for ( Connection connection : this.connections ) {
			if ( connection.getID() == connectionID ) {
				// Skip
				continue;
			}
			connection.send( message );
		}
	}

	public void sendToAll( Message message ) {
		for ( Connection connection : this.connections ) {
			connection.send( message );
		}
	}

}
