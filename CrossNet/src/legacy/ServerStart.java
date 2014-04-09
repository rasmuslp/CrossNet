package legacy;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.log.LogLevel;
import legacy.server.TcpSocketServer;

public class ServerStart {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );
		Log.info( "Server starting" );

		TcpSocketServer tcpSocketServer = new TcpSocketServer();
	}

}
