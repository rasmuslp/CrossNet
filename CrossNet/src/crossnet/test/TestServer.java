package crossnet.test;

import java.io.IOException;

import crossnet.Server;
import crossnet.log.Log;
import crossnet.log.LogLevel;
import crossnet.message.MessageParser;
import crossnet.message.framework.FrameworkMessageParser;

public class TestServer {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );

		MessageParser messageParser = new FrameworkMessageParser();

		Server server = new Server( messageParser );
		server.addConnectionListener( new DefaultListener() );
		server.start( "Server" );
		server.bind( 55100 );
	}
}
