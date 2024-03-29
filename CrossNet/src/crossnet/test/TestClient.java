package crossnet.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import crossnet.CrossNetClient;
import crossnet.log.Log;
import crossnet.log.LogLevel;

public class TestClient {

	public static void main( String[] args ) throws UnknownHostException, IOException {
		Log.set( LogLevel.TRACE );

		CrossNetClient crossNetClient = new CrossNetClient();
		crossNetClient.addConnectionListener( new DefaultListener() );
		crossNetClient.start( "CrossNetClient" );
		crossNetClient.connect( InetAddress.getByName( "localhost" ), 55100, 5000 );

		try {
			Thread.sleep( 10000 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
