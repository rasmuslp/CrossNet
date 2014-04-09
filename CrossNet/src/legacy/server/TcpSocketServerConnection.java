package legacy.server;

import java.io.IOException;

import crossnet.log.Log;
import crossnet.packet.Packet;
import crossnet.packet.PacketFactory;

public class TcpSocketServerConnection extends AbstractTcpSocketServerConnection {

	public TcpSocketServerConnection( PacketFactory packetFactory ) throws IOException {
		super( packetFactory );
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void clientConnected( TcpSocketClient client ) {
		Log.info( "Client connected: " + client.getSocketChannel().socket().getRemoteSocketAddress() );

	}

	@Override
	protected void clientDisconnected( TcpSocketClient client ) {
		Log.info( "Client disconnected: " + client.getSocketChannel().socket().getRemoteSocketAddress() );

	}

	@Override
	protected void receivedPacketFromClient( TcpSocketClient client, Packet packet ) {
		byte[] data = packet.getData();
		String string = new String( data );
		Log.info( "Received message from client: " + client.getSocketChannel().socket().getRemoteSocketAddress() + " message: " + string );
	}

	@Override
	protected void serverStarted() {
		Log.info( "Server started" );
	}

	@Override
	protected void serverShutDown() {
		Log.info( "Server shut down" );
	}

	@Override
	protected void serverStartedListening() {
		Log.info( "Server started listening" );
	}

}
