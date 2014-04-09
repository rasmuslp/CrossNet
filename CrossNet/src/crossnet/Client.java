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
import crossnet.message.framework.FrameworkMessage;
import crossnet.message.framework.messages.KeepAliveMessage;
import crossnet.message.framework.messages.RegisterMessage;
import crossnet.packet.PacketFactory;

/**
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class Client extends LocalEndPoint {

	private final Connection connection;

	private final Selector selector;

	private InetAddress connectHost;
	private int connectPort;
	private int connectTimeout;

	private volatile boolean registered = false;
	private final Object registrationLock = new Object();

	public Client( final PacketFactory packetFactory, final MessageParser messageParser ) {
		TransportLayer transportLayer = new TcpTransportLayer( packetFactory, messageParser );

		this.connection = new Connection();
		this.connection.initialize( transportLayer );

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
						if ( key.isConnectable() ) {
							this.completeConnection( key );
						} else if ( key.isReadable() ) {
							this.read( key );
						} else if ( key.isWritable() ) {
							this.write( key );
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
			} else {
				this.ping();
				this.keepAlive();
			}
			if ( this.connection.getTransportLayer().isIdle() ) {
				this.connection.notifyIdle();
			}
		}
	}

	public void connect( InetAddress host, int port, int timeout ) throws IOException {
		if ( host == null ) {
			throw new IllegalArgumentException( "host cannot be null." );
		}

		if ( Thread.currentThread() == this.updateThread ) {
			throw new IllegalStateException( "Cannot connect on the connction's update thread." );
		}

		this.connectHost = host;
		this.connectPort = port;
		this.connectTimeout = timeout;

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
				timeoutEnd = System.currentTimeMillis() + this.connectTimeout;
				SocketAddress socketAddress = new InetSocketAddress( this.connectHost, this.connectPort );
				//TODO About TCP timeout on connect ?
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

	public void reconnect() throws IOException {
		this.reconnect( this.connectTimeout );
	}

	public void reconnect( int timeout ) throws IOException {
		if ( this.connectHost == null ) {
			throw new IllegalStateException( "This Client has never been connected." );
		}
		this.connect( this.connectHost, this.connectPort, timeout );
	}

	private void ping() {
		if ( !this.connection.isConnected() ) {
			return;
		}

		long time = System.currentTimeMillis();
		if ( this.connection.needsPing( time ) ) {
			this.connection.updatePingRoundTripTime();
		}
	}

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

	private void completeConnection( SelectionKey key ) {

	}

	private void read( SelectionKey key ) throws IOException {
		while ( true ) {
			Message message = this.connection.getTransportLayer().read( this.connection );
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

			//TODO Review this.
			if ( Log.DEBUG ) {
				String objectString = message == null ? "null" : message.getClass().getSimpleName();
				if ( !( message instanceof FrameworkMessage ) ) {
					Log.debug( "CrossNet", this.connection + " received: " + objectString );
				} else if ( Log.TRACE ) {
					Log.trace( "CrossNet", this.connection + " received: " + objectString );
				}
			}

			this.connection.notifyReceived( message );
		}
	}

	private void write( SelectionKey key ) throws IOException {
		this.connection.getTransportLayer().write();
	}

}
