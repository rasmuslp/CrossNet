package crossnet.packet;

/**
 * Immutable.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class Packet {

	protected final byte[] data;

	protected Packet( final byte[] data ) {
		this.data = data;
	}

	/**
	 * Gets the payload of this Packet.
	 * 
	 * @return The payload.
	 */
	public byte[] getData() {
		return this.data.clone();
	}

	/**
	 * Gets the full representation of this Packet. That is, the payload plus any additional headers and or
	 * terminators.
	 * 
	 * @return The full representation.
	 */
	public abstract byte[] toBytes();

}
