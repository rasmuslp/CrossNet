package legacy.server;

import crossnet.packet.Packet;

public interface ServerConnection extends Runnable {

	public void send( Client client, Packet packet );

}
