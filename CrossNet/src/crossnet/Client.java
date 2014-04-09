package crossnet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import crossnet.listener.Listener;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.RegisterMessage;

/**
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class Client extends LocalEndPoint {

	/**
	 * The Selector for the TCP Socket.
	 */
	private final Selector selector;

	/**
	 * The Connection to the {@link Server}.
	 */
	private final Connection connection;

	/**
	 * The server address of the current Connection. If disconnected, then of the last Connection.
	 */
	private InetAddress connectHost;

	/**
	 * The server port of the current Connection. If disconnected, then of the last Connection.
	 */
	private int connectPort;

	/**
	 * The server connection registration timeout of the current Connection. If disconnected, then of the last
	 * Connection. This is given in milliseconds.
	 */
	private int connectRegistrationTimeout;

	/**
	 * Lock used by the registration mechanism.
	 */
	private final Object registrationLock = new Object();

	/**
	 * {@code True} iff registration with Server was successful.
	 */
	private volatile boolean registered = false;

	public Client( final MessageParser messageParser ) {
		try {
			this.selector = Selector.open();
		} catch ( IOException e ) {
			Log.error( "CrossNet", "Error opening Selector", e );
			throw new RuntimeException( "Error opening Selector", e );
		}

		this.connection = new Connection();
		TransportLayer transportLayer = new TcpTransportLayer( this.connection, messageParser );
		this.connection.initialize( transportLayer );
	}

	@Override
	public void start( String threadName ) {
		if ( this.threadRunning ) {
			Log.trace( "CrossNet", "Update thread already running." );
			this.shutdownThread = true;
			try {
				this.updateThread.join( 5000 );
			} catch ( InterruptedException e ) {
				// Ignored
			}
		}

		// Start thread
		this.updateThread = new Thread( this, threadName );
		this.updateThread.setDaemon( true );
		this.updateThread.start();

		this.threadRunning = true;
	}

	@Override
	public void close() {
		this.connection.close();
		// Select one last time to complete closing the socket.
		//TODO: Wake selector in try-catch ?
	}

	@Override
	public void dispose() throws IOException {
		this.close();
		this.selector.close();
	}

	@Override
	public void addListener( Listener listener ) {
		this.connection.addListener( listener );
		Log.trace( "CrossNet", "Client listener added." );
	}

	@Override
	public void removeListener( Listener listener ) {
		this.connection.removeListener( listener );
		Log.trace( "CrossNet", "Client listener removed." );
	}

	@Override
	public void update( int timeout ) throws IOException {
		this.updateThread = Thread.currentThread();
		synchronized ( this.updateLock ) {
			// Block to avoid select while connecting.
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
				Log.debug( "CrossNet", "Update got 0 selects rather quickly." );
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
						if ( key.isReadable() ) {
							this.read();
						}
						if ( key.isWritable() ) {
							this.write();
						}
					} catch ( CancelledKeyException e ) {
						// Connection closed.
						Log.trace( "CrossNet", "Connection closed." );
					}
				}
			}
		}

		if ( this.connection.isConnected() ) {
			long time = System.currentTimeMillis();
			if ( this.connection.getTransportLayer().isTimedOut( time ) ) {
				Log.debug( "CrossNet", this.connection + " timed out." );
				this.close();
			}
			if ( this.connection.getTransportLayer().isIdle() ) {
				this.connection.notifyIdle();
			}
		}
	}

	/**
	 * Connects this Client to a Server.
	 * <p>
	 * As a minimal registration procedure is needed, the {@link #update(int)} method must be called on another thread
	 * when this method is called.
	 * 
	 * @param host
	 *            The address to connect to.
	 * @param port
	 *            The port to connect to.
	 * @param registrationTimeout
	 *            The registration timeout in milliseconds.
	 * @throws IOException
	 *             If the connection could not be opened or the attempt timed out.
	 */
	public void connect( InetAddress host, int port, int registrationTimeout ) throws IOException {
		if ( host == null ) {
			throw new IllegalArgumentException( "host cannot be null." );
		}

		if ( Thread.currentThread() == this.updateThread ) {
			throw new IllegalStateException( "Cannot connect on the connction's update thread." );
		}

		this.connectHost = host;
		this.connectPort = port;
		this.connectRegistrationTimeout = registrationTimeout;

		// Close any existing connection
		this.close();

		Log.info( "CrossNet", "Connecting to " + this.connectHost + ":" + this.connectPort );

		// Clear ID
		this.connection.setID( -1 );

		try {
			long timeoutEnd;
			synchronized ( this.updateLock ) {
				this.registered = false;
				this.selector.wakeup();
				timeoutEnd = System.currentTimeMillis() + this.connectRegistrationTimeout;
				SocketAddress socketAddress = new InetSocketAddress( this.connectHost, this.connectPort );

				TcpTransportLayer tcpTransportLayer = (TcpTransportLayer) this.connection.getTransportLayer();
				tcpTransportLayer.connect( this.selector, socketAddress, 5000 );
			}

			synchronized ( this.registrationLock ) {
				while ( !this.registered && System.currentTimeMillis() < timeoutEnd ) {
					try {
						this.registrationLock.wait( 100 );
					} catch ( InterruptedException e ) {
						// Ignored
					}
				}

				if ( !this.registered ) {
					throw new SocketTimeoutException( "Connected, but timed out during registration.\nNote: Client#update must be called in a separate thread during connect." );
				}
			}
		} catch ( IOException e ) {
			this.close();
			throw e;
		}
	}

	/**
	 * Reconnects by calling {@link #connect(InetAddress, int, int)} with the values last passed to that method.
	 * 
	 * @throws IOException
	 */
	public void reconnect() throws IOException {
		this.reconnect( this.connectRegistrationTimeout );
	}

	/**
	 * Reconnects by calling {@link #connect(InetAddress, int, int)} with the values last passed to that method, except
	 * for the registration timeout.
	 * 
	 * @param registrationTimeout
	 *            The new registration timeout in milliseconds.
	 * @throws IOException
	 */
	public void reconnect( int registrationTimeout ) throws IOException {
		if ( this.connectHost == null ) {
			throw new IllegalStateException( "This Client has never been connected." );
		}
		this.connect( this.connectHost, this.connectPort, registrationTimeout );
	}

	/**
	 * Checks if the {@link Connection} needs pinging.
	 * <p>
	 * Called by {@link #update(int)}.
	 */
	private void ping() {
		if ( !this.connection.isConnected() ) {
			return;
		}

		long time = System.currentTimeMillis();
		if ( this.connection.needsPing( time ) ) {
			this.connection.updatePingRoundTripTime();
		}
	}

	/**
	 * Checks if the {@link Connection} needs keep alive.
	 * <p>
	 * Called by {@link #update(int)}.
	 */
	private void keepAlive() {
		if ( !this.connection.isConnected() ) {
			return;
		}

		long time = System.currentTimeMillis();
		if ( this.connection.getTransportLayer().needsKeepAlive( time ) ) {
			KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
			this.connection.sendInternal( keepAliveMessage );
		}
	}

	/**
	 * Reads from the {@link Connection}.
	 * <p>
	 * Called by {@link #update(int)}.
	 * 
	 * @throws IOException
	 *             If a read error occurs.
	 */
	private void read() throws IOException {
		while ( true ) {
			Message message = this.connection.getTransportLayer().read();
			if ( message == null ) {
				// No more messages could be read.
				break;
			}

			if ( !this.registered ) {
				if ( message instanceof RegisterMessage ) {
					RegisterMessage registerMessage = (RegisterMessage) message;
					this.connection.setID( registerMessage.getConnectionID() );
					synchronized ( this.registrationLock ) {
						this.registered = true;
						this.registrationLock.notifyAll();
						Log.trace( "CrossNet", this.connection + " received: RegisterMessage" );
						this.connection.setConnected( true );
					}
					this.connection.notifyConnected();
				}
				continue;
			}
			if ( !this.connection.isConnected() ) {
				continue;
			}

			this.connection.notifyReceived( message );
		}
	}

	/**
	 * Writes to the {@link Connection}.
	 * <p>
	 * Called by {@link #update(int)}.
	 * 
	 * @throws IOException
	 *             If a write error occurs.
	 */
	private void write() throws IOException {
		this.connection.getTransportLayer().write();
	}

	/**
	 * Gets the Connection to the {@link Server}.
	 * 
	 * @return The Connection to the {@link Server}.
	 */
	public Connection getConnection() {
		return this.connection;
	}

}
