package crossnet;

import java.io.IOException;

import crossnet.listener.Listener;
import crossnet.log.Log;

/**
 * The local end point that manages communication.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class LocalEndPoint implements Runnable {

	protected volatile boolean threadRunning = false;
	protected volatile boolean shutdownThread = false;

	protected final Object updateLock = new Object();
	protected Thread updateThread;

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
	 * Adds a Listener. A Listener cannot be added multiple times.
	 * 
	 * @param listener
	 *            The Listener to add.
	 */
	public abstract void addListener( Listener listener );

	/**
	 * Removes a Listener.
	 * 
	 * @param listener
	 *            The Listener to remove.
	 */
	public abstract void removeListener( Listener listener );

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