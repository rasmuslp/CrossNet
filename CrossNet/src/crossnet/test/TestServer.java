package crossnet.test;

import java.io.IOException;

import crossnet.CrossNetServer;
import crossnet.log.Log;
import crossnet.log.LogLevel;

public class TestServer {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );

		CrossNetServer crossNetServer = new CrossNetServer();
		crossNetServer.addConnectionListener( new DefaultListener() );
		crossNetServer.start( "CrossNetServer" );
		crossNetServer.bind( 55100 );
	}
}
