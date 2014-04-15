package crossnet;

import java.io.IOException;

import crossnet.listener.ConnectionListener;
import crossnet.log.Log;
import crossnet.message.Message;
import crossnet.message.MessageParser;
import crossnet.message.crossnet.CrossNetMessageParser;

/**
 * The local end point that manages communication.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class LocalEndPoint implements Runnable {

	/**
	 * The MessageParser for the CrossNet {@link Message}s.
	 */
	protected final MessageParser messageParser = new CrossNetMessageParser();

	/**
	 * {@code True} iff the update thread is running.
	 */
	protected volatile boolean threadRunning = false;

	/**
	 * {@code True} iff the update thread should shut down or has shut down.
	 */
	protected volatile boolean shutdownThread = false;

	/**
	 * Lock used to hinder interference in the {@link #update(int)} method.
	 */
	protected final Object updateLock = new Object();

	/**
	 * The last thread to call {@link #update(int)}.
	 */
	protected Thread updateThread;

	/**
	 * Gets the MessageParser for the CrossNet {@link Message}s.
	 * 
	 * @return The MessageParser for the CrossNet {@link Message}s.
	 */
	public MessageParser getMessageParser() {
		return this.messageParser;
	}

	/**
	 * Starts a new thread that drives the network {@link #run()} loop.
	 * 
	 * @param threadName
	 *            The name of the thread.
	 */
	public abstract void start( String threadName );

	/**
	 * Continually drives the network run loop until {@link #stop()} is called.
	 */
	@Override
	public void run() {
		Log.trace( "CrossNet", "Update thread started." );
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
		Log.trace( "CrossNet", "Update thread stopped." );
	}

	/**
	 * Returns the last thread that called {@link #update(int)}.
	 * 
	 * This can be useful to detect when long running code will be run on the update thread.
	 * 
	 * @return The last thread that called {@link #update(int)}.
	 */
	public Thread getUpdateThread() {
		return this.updateThread;
	}

	/**
	 * Stops the thread that drives the network {@link #run()} loop.
	 */
	public void stop() {
		if ( this.shutdownThread ) {
			Log.trace( "CrossNet", "Update thread shutdown already in progress." );
			return;
		}

		Log.trace( "CrossNet", "Update thread shutting down." );
		// Close open connections
		this.close();
		this.shutdownThread = true;
	}

	/**
	 * Closes all {@link Connections}.
	 * 
	 * @see Client
	 * @see Server
	 */
	public abstract void close();

	/**
	 * Release resources.
	 * 
	 * @throws IOException
	 */
	public abstract void dispose() throws IOException;

	/**
	 * Adds a listener. A listener cannot be added multiple times.
	 * 
	 * @param connectionListener
	 *            The ConnectionListener to add.
	 */
	public abstract void addConnectionListener( ConnectionListener connectionListener );

	/**
	 * Removes a listener.
	 * 
	 * @param connectionListener
	 *            The ConnectionListener to remove.
	 */
	public abstract void removeConnectionListener( ConnectionListener connectionListener );

	/**
	 * Updates the network state.
	 * 
	 * @see Client#update(int)
	 * @see Server#update(int)
	 * 
	 * @param timeout
	 *            The maximum time to wait for data. May be zero to return immediately if there is no data to process.
	 * @throws IOException
	 */
	public abstract void update( int timeout ) throws IOException;

}