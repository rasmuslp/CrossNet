package crossnet;

import java.io.IOException;

import crossnet.listener.Listener;

/**
 * The local end point that manages communication.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface LocalEndPoint extends Runnable {

	/**
	 * Starts a new thread that drives the network {@link #run()} loop.
	 * 
	 * @param threadName
	 *            The name of the thread.
	 */
	public void start( String threadName );

	/**
	 * Continually drives the network run loop until {@link #stop()} is called.
	 */
	@Override
	public void run();

	/**
	 * Returns the last thread that called {@link #update(int)}.
	 * 
	 * This can be useful to detect when long running code will be run on the update thread.
	 * 
	 * @return The last thread that called {@link #update(int)}.
	 */
	public Thread getUpdateThread();

	/**
	 * Stops the thread that drives the network {@link #run()} loop.
	 */
	public void stop();

	/**
	 * Closes all {@link Connections}.
	 * 
	 * @see Client
	 * @see Server
	 */
	public void close();

	/**
	 * Release resources.
	 * 
	 * @throws IOException
	 */
	public void dispose() throws IOException;

	/**
	 * Adds a Listener. A Listener cannot be added multiple times.
	 * 
	 * @param listener
	 *            The Listener to add.
	 */
	public void addListener( Listener listener );

	/**
	 * Removes a Listener.
	 * 
	 * @param listener
	 *            The Listener to remove.
	 */
	public void removeListener( Listener listener );

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
	public void update( int timeout ) throws IOException;

}