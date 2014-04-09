package legacy.client;

import crossnet.packet.Packet;

public interface ClientConnection extends Runnable {

	public void send( Packet packet );

}
