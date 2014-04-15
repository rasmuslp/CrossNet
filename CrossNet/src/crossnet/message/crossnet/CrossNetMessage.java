package crossnet.message.crossnet;

import crossnet.message.AbstractMessage;
import crossnet.message.Message;

/**
 * These are used internally to maintain state and wrap external {@link Message}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class CrossNetMessage extends AbstractMessage< CrossNetMessageType > {

	public CrossNetMessage( CrossNetMessageType messageType ) {
		super( messageType );
	}

}
