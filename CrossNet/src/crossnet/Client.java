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
public class Client extends Connection implements LocalEndPoint {

	private final PacketFactory packetFactory;
	private final MessageParser messageParser;

	private final Selector selector;

	private final Object updateLock = new Object();
	private Thread updateThread;

	private volatile boolean threadRunning = false;
	private volatile boolean shutdownThread = false;

	private InetAddress connectHost;
	private int connectPort;
	private int connectTimeout;

	private volatile boolean registered = false;
	private final Object registrationLock = new Object();

	public Client( final PacketFactory packetFactory, final MessageParser messageParser ) {
		this.packetFactory = packetFactory;
		this.messageParser = messageParser;

		TransportLayer transportLayer = new TcpTransportLayer( this.packetFactory, this.messageParser );
		this.initialize( transportLayer );

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
			Log.trace( "CrossNet", "Client thread already running." );
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
	public void run() {
		Log.trace( "CrossNet", "Client thread started." );
		this.shutdownThread = false;
		while ( !this.shutdownThread ) {
			try {
				this.update( 100 );
			} catch ( IOException e ) {
				Log.error( "CrossNet", "Unable to update connection.", e );
				this.close();
			}
		}
		this.threadRunning = false;
		Log.trace( "CrossNet", "Client thread stopped." );
	}

	@Override
	public Thread getUpdateThread() {
		return this.updateThread;
	}

	@Override
	public void stop() {
		if ( this.shutdownThread ) {
			Log.trace( "CrossNet", "Client thread shutdown already in progress." );
			return;
		}

		Log.trace( "CrossNet", "Server thread shutting down." );
		// Close open connections
		this.close();
		this.shutdownThread = true;
		this.selector.wakeup();
	}

	@Override
	public void close() {
		super.close();
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
		super.addListener( listener );
		Log.trace( "CrossNet", "Client listener added." );
	}

	@Override
	public void removeListener( Listener listener ) {
		super.removeListener( listener );
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

		if ( this.isConnected() ) {
			long time = System.currentTimeMillis();
			if ( this.getTransportLayer().isTimedOut( time ) ) {
				Log.debug( "CrossNet", this + " timed out." );
				this.close();
			} else {
				this.ping();
				this.keepAlive();
			}
			if ( this.getTransportLayer().isIdle() ) {
				this.notifyIdle();
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
		this.setID( -1 );

		try {
			long timeoutEnd;
			synchronized ( this.updateLock ) {
				this.registered = false;
				this.selector.wakeup();
				timeoutEnd = System.currentTimeMillis() + this.connectTimeout;
				SocketAddress socketAddress = new InetSocketAddress( this.connectHost, this.connectPort );
				//TODO About TCP timeout on connect ?
				TcpTransportLayer tcpTransportLayer = (TcpTransportLayer) this.getTransportLayer();
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
		if ( !this.isConnected() ) {
			return;
		}

		long time = System.currentTimeMillis();
		if ( this.needsPing( time ) ) {
			this.updatePingRoundTripTime();
		}
	}

	private void keepAlive() {
		if ( !this.isConnected() ) {
			return;
		}

		long time = System.currentTimeMillis();
		if ( this.getTransportLayer().needsKeepAlive( time ) ) {
			KeepAliveMessage keepAliveMessage = new KeepAliveMessage();
			super.sendInternal( keepAliveMessage );
		}
	}

	private void completeConnection( SelectionKey key ) {

	}

	private void read( SelectionKey key ) throws IOException {
		while ( true ) {
			Message message = this.getTransportLayer().read( this );
			if ( message == null ) {
				// No more messages could be read.
				break;
			}

			if ( !this.registered ) {
				if ( message instanceof RegisterMessage ) {
					RegisterMessage registerMessage = (RegisterMessage) message;
					this.setID( registerMessage.getConnectionID() );
					synchronized ( this.registrationLock ) {
						this.registered = true;
						this.registrationLock.notifyAll();
						Log.trace( "CrossNet", this + " received: RegisterMessage" );
						this.setConnected( true );
					}
					this.notifyConnected();
				}
				continue;
			}
			if ( !this.isConnected() ) {
				continue;
			}

			//TODO Review this.
			if ( Log.DEBUG ) {
				String objectString = message == null ? "null" : message.getClass().getSimpleName();
				if ( !( message instanceof FrameworkMessage ) ) {
					Log.debug( "CrossNet", this + " received: " + objectString );
				} else if ( Log.TRACE ) {
					Log.trace( "CrossNet", this + " received: " + objectString );
				}
			}

			this.notifyReceived( message );
		}
	}

	private void write( SelectionKey key ) throws IOException {
		this.getTransportLayer().write();
	}

}
