package crossnet.test;

import java.io.IOException;

import crossnet.Server;
import crossnet.log.Log;
import crossnet.log.LogLevel;

public class TestServer {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );

		Server server = new Server();
		server.addConnectionListener( new DefaultListener() );
		server.start( "Server" );
		server.bind( 55100 );
	}
}
