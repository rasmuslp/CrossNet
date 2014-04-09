package legacy.server;

import crossnet.packet.Packet;

public interface Client {

	public void send( Packet packet );

}
