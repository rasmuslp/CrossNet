package legacy;

import java.io.IOException;
import java.net.InetAddress;

import crossnet.log.Log;
import crossnet.log.LogLevel;
import legacy.client.TcpSocketClient;

public class ClientStart {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );
		Log.info( "Client starting" );

		InetAddress hostAddress = InetAddress.getByName( "localhost" );
		TcpSocketClient tcpSocketClient = new TcpSocketClient( hostAddress );
	}
}
