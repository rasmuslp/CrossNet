package crossnet.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Wrapper reader class for a byte[].
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ByteArrayReader {

	private final ByteArrayInputStream byteArrayInputStream;
	private final DataInputStream dataInputStream;

	public ByteArrayReader( byte[] data ) {
		this.byteArrayInputStream = new ByteArrayInputStream( data );
		this.dataInputStream = new DataInputStream( this.byteArrayInputStream );
	}

	public boolean readBoolean() throws IOException {
		return this.dataInputStream.readBoolean();
	}

	public byte readByte() throws IOException {
		return this.dataInputStream.readByte();
	}

	public int readUnsignedByte() throws IOException {
		return this.dataInputStream.readUnsignedByte();
	}

	public short readShort() throws IOException {
		return this.dataInputStream.readShort();
	}

	public int readUnsignedShort() throws IOException {
		return this.dataInputStream.readUnsignedShort();
	}

	public int readInt() throws IOException {
		return this.dataInputStream.readInt();
	}

	public long readLong() throws IOException {
		return this.dataInputStream.readLong();
	}

	public int bytesAvailable() throws IOException {
		return this.dataInputStream.available();
	}

	/**
	 * Reads from the byte[] and converts it to an UTF-8 String.
	 * <p>
	 * The String to read must not exceed 255 bytes.
	 * 
	 * @return The read String.
	 * @throws IOException
	 */
	public String readString255() throws IOException {
		int stringLength = this.readUnsignedByte();
		byte[] stringBytes = new byte[stringLength];
		this.readByteArray( stringBytes );
		return new String( stringBytes, Charset.forName( "UTF-8" ) );
	}

	/**
	 * Reads for the length of data.
	 * 
	 * @param data
	 *            The byte array to fill with data.
	 * @throws IOException
	 */
	public void readByteArray( byte[] data ) throws IOException {
		this.dataInputStream.readFully( data );
	}

}
