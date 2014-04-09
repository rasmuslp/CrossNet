package legacy.server;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.log.LogLevel;
import crossnet.packet.lflf.LFLFPacketFactory;

public class StartServer {

	public static void main( String[] args ) throws IOException {
		Log.set( LogLevel.TRACE );
		Log.info( "Client starting" );

		Server server = new Server( new LFLFPacketFactory() );
		server.start( "Main connection thread" );
		server.bind( 3000 );
		try {
			Thread.sleep( 10000 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		server.stop();
	}

}
