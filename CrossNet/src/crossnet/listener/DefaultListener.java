package crossnet.listener;

import crossnet.Connection;
import crossnet.message.Message;

public class DefaultListener implements Listener {

	@Override
	public void connected( Connection connection ) {
		System.out.println( connection + " connected." );

	}

	@Override
	public void disconnected( Connection connection ) {
		System.out.println( connection + " disconnected." );
	}

	@Override
	public void received( Connection connection, Message message ) {
		System.out.println( connection + " received: " + message.getClass().getSimpleName() );
	}

	@Override
	public void idle( Connection connection ) {
		//System.out.println( connection + " idle." );
	}

}
