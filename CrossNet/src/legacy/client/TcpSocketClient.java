package legacy.client;

import java.io.IOException;
import java.net.InetAddress;

import crossnet.packet.Packet;
import crossnet.packet.lflf.LFLFPacket;
import crossnet.packet.lflf.LFLFPacketFactory;

public class TcpSocketClient {

	private final Thread connectionThread;

	public TcpSocketClient( InetAddress hostAddress ) throws IOException {
		TcpSocketClientConnection tcpSocketClientConnection = new TcpSocketClientConnection( new LFLFPacketFactory(), hostAddress );
		this.connectionThread = new Thread( tcpSocketClientConnection, "TcpSocketClientConnection" );
		this.connectionThread.start();

		try {
			Packet packet = new LFLFPacket( "Hello server!".getBytes() );
			while ( true ) {
				tcpSocketClientConnection.send( packet );
				Thread.sleep( 100 );
			}
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
