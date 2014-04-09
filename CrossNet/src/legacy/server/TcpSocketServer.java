package legacy.server;

import java.io.IOException;

import crossnet.packet.lflf.LFLFPacketFactory;

public class TcpSocketServer {

	protected final static int LOBBY_PORT = 3000;
	protected final static int ASSET_PORT = 3001;
	protected final static int GAME_PORT = 3002;

	public TcpSocketServer() throws IOException {
		TcpSocketServerConnection tcpSocketServerConnection = new TcpSocketServerConnection( new LFLFPacketFactory() );
		tcpSocketServerConnection.start( "Main connection thread" );
		tcpSocketServerConnection.bind( LOBBY_PORT );
		try {
			Thread.sleep( 10000 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tcpSocketServerConnection.stop();
	}

}
