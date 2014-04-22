package crossnet.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import crossnet.Client;
import crossnet.log.Log;
import crossnet.log.LogLevel;

public class TestClient {

	public static void main( String[] args ) throws UnknownHostException, IOException {
		Log.set( LogLevel.TRACE );

		Client client = new Client();
		client.addConnectionListener( new DefaultListener() );
		client.start( "Client" );
		client.connect( InetAddress.getByName( "localhost" ), 55100, 5000 );

		try {
			Thread.sleep( 10000 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
