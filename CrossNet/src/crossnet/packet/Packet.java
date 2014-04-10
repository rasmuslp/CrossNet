package crossnet.packet;

import crossnet.TransportLayer;

/**
 * Packet to be sent over a {@link TransportLayer}.
 * <p>
 * Immutable.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public abstract class Packet {

	/**
	 * The payload this packet carries.
	 */
	protected final byte[] payload;

	/**
	 * Create a Packet with payload.
	 * 
	 * @param payload
	 *            The payload.
	 */
	protected Packet( final byte[] payload ) {
		this.payload = payload;
	}

	/**
	 * Gets the payload of this Packet.
	 * 
	 * @return The payload.
	 */
	public byte[] getPayload() {
		return this.payload.clone();
	}

	/**
	 * Gets the full representation of this Packet. That is, the payload plus any additional headers and or
	 * terminators.
	 * <p>
	 * May return null if the Packet couldn't be serialised.
	 * 
	 * @return The full representation.
	 */
	public abstract byte[] toBytes();

}
