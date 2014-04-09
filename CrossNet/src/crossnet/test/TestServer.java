package crossnet.test;

import java.io.IOException;

import crossnet.Server;
import crossnet.listener.DefaultListener;
import crossnet.log.Log;
import crossnet.log.LogLevel;
import crossnet.message.MessageParser;
import crossnet.message.framework.FrameworkMessageParser;
import crossnet.packet.PacketFactory;
import crossnet.packet.length.LengthPacketFactory;

public class TestServer {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );

		PacketFactory packetFactory = new LengthPacketFactory();
		MessageParser messageParser = new FrameworkMessageParser();

		Server server = new Server( packetFactory, messageParser );
		server.addListener( new DefaultListener() );
		server.start( "Server" );
		server.bind( 55100 );
	}
}
