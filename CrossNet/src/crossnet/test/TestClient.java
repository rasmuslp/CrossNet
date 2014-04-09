package crossnet.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import crossnet.Client;
import crossnet.listener.DefaultListener;
import crossnet.log.Log;
import crossnet.log.LogLevel;
import crossnet.message.MessageParser;
import crossnet.message.framework.FrameworkMessageParser;
import crossnet.packet.PacketFactory;
import crossnet.packet.length.LengthPacketFactory;

public class TestClient {

	public static void main( String[] args ) throws UnknownHostException, IOException {
		Log.set( LogLevel.TRACE );

		PacketFactory packetFactory = new LengthPacketFactory();
		MessageParser messageParser = new FrameworkMessageParser();

		Client client = new Client( packetFactory, messageParser );
		client.addListener( new DefaultListener() );
		client.start( "Client" );
		client.connect( InetAddress.getByName( "hs.rlponline.dk" ), 55100, 5000 );

		while ( true ) {
			try {
				Thread.sleep( 1000 );
			} catch ( InterruptedException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
