package crossnet.packet.lflf;

import crossnet.packet.Packet;

public class LFLFPacket extends Packet {

	//TODO With the headers from Message, this might be an issue as '\n' is 0xA. Hence this will easily confuse the PacketFactory.

	public final static int MAX_DATA = 1024;
	public final static int MAX_LENGTH = LFLFPacket.MAX_DATA + 2;

	public LFLFPacket( final byte[] data ) {
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

		byte[] ret = new byte[this.data.length + terminator.length];

		System.arraycopy( this.data, 0, ret, 0, this.data.length );
		System.arraycopy( terminator, 0, ret, this.data.length, terminator.length );

		return ret;
	}

}
