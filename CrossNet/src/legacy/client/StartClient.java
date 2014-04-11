package legacy.client;

import java.io.IOException;
import java.net.InetAddress;

import crossnet.log.Log;
import crossnet.log.LogLevel;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;
import crossnet.packet.lflf.LFLFPacketFactory;

public class StartClient {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );
		Log.info( "Client starting" );

		PacketFactory packetFactory = new LFLFPacketFactory();

		Client client = new Client( packetFactory, InetAddress.getByName( "localhost" ) );
		Thread thread = new Thread( client, "Client" );
		thread.start();

		try {
			Packet packet = packetFactory.newPacket( "Hello server!".getBytes() );
			while ( true ) {
				client.send( packet );
				Thread.sleep( 100 );
			}
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
