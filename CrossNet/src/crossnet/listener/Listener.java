package crossnet.listener;

import crossnet.Connection;
import crossnet.message.Message;

public interface Listener {

	public void connected( Connection connection );

	public void disconnected( Connection connection );

	public void received( Connection connection, Message message );

	public void idle( Connection connection );

}
