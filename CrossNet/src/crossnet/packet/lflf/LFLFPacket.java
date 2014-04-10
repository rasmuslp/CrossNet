package crossnet.packet.lflf;

import crossnet.packet.Packet;

/**
 * The LFLFPacket is a Packet that has a LF LF (i.e. 2B) terminator.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class LFLFPacket extends Packet {

	/**
	 * Maximum payload size.
	 */
	public final static int MAX_PAYLOAD_SIZE = 1024;

	/**
	 * Maximum packet size.
	 */
	public final static int MAX_PACKET_SIZE = LFLFPacket.MAX_PAYLOAD_SIZE + 2;

	/**
	 * Create a Packet with payload.
	 * <p>
	 * NB: Due to the construction of this, the payload may not end on a LF and may not contain two consecutive LFs.
	 * 
	 * @param payload
	 *            The payload.
	 */
	public LFLFPacket( final byte[] data ) {
		//TODO Change to protected
		super( data );

		if ( data == null ) {
			throw new IllegalArgumentException( "Data cannot be null" );
		}

		if ( data[data.length - 1] == '\n' ) {
			throw new IllegalArgumentException( "LFLFPacket cannot end with a linefeed character." );
		}

		boolean prevIsLF = false;
		for ( int i = 0; i < data.length; i++ ) {
			if ( data[i] == '\n' ) {
				if ( prevIsLF ) {
					// Two consecutive LFs found.
					throw new IllegalArgumentException( "LFLFPacket cannot contain two consecutive linefeed characters." );
				}

				// Current char was LF
				prevIsLF = true;
			} else {
				// Current char was not LF
				prevIsLF = false;
			}
		}
	}

	@Override
	public byte[] toBytes() {
		byte[] terminator = "\n\n".getBytes();

		byte[] ret = new byte[this.payload.length + terminator.length];

		System.arraycopy( this.payload, 0, ret, 0, this.payload.length );
		System.arraycopy( terminator, 0, ret, this.payload.length, terminator.length );

		return ret;
	}

}
