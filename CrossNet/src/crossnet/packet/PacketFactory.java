package crossnet.packet;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public interface PacketFactory {

	/**
	 * Gets the maximal number of bytes that Packets can carry as payload.
	 * 
	 * @return The maximal number of bytes that Packets can carry as payload.
	 */
	public int getMaxData();

	/**
	 * Gets the maximal number of bytes that can constitute a Packet. That is, the payload plus any additional headers
	 * and or terminators.
	 * 
	 * @return The maximal number of bytes that can constitute a Packet.
	 */
	public int getMaxLength();

	/**
	 * Factory constructor for Packet.
	 * 
	 * @param data
	 *            The payload of the Packet.
	 * @return The newly created Packet.
	 */
	public Packet newPacket( final byte[] data );

	public Packet parseData( ByteBuffer byteBuffer );

	public List< Packet > parseDataList( ByteBuffer byteBuffer );

}
