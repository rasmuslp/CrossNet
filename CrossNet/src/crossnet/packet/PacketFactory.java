package crossnet.packet;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Creates {@link Packet}s with payload from byte arrays and {@link ByteBuffer}s.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface PacketFactory {

	/**
	 * Gets the maximal number of bytes that {@link Packet}s can carry as payload.
	 * 
	 * @return The maximal number of bytes that {@link Packet}s can carry as payload.
	 */
	public int getMaxPayloadSize();

	/**
	 * Gets the maximal number of bytes that can constitute a {@link Packet}. That is, the payload plus any additional
	 * headers
	 * and or terminators.
	 * 
	 * @return The maximal number of bytes that can constitute a {@link Packet}.
	 */
	public int getMaxPacketSize();

	/**
	 * Factory constructor for Packet.
	 * 
	 * @param payload
	 *            The payload of the Packet.
	 * @return The newly created Packet.
	 */
	public Packet newPacket( final byte[] payload );

	/**
	 * Reads from the buffer and tries to construct a Packet.
	 * <p>
	 * May return null if there was not enough data or an error occured.
	 * 
	 * @param byteBuffer
	 *            The buffer to read from.
	 * @return A freshly parsed Packet.
	 */
	public Packet parseData( ByteBuffer byteBuffer );

	public List< Packet > parseDataList( ByteBuffer byteBuffer );

}
