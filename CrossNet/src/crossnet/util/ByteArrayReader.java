package crossnet.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

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

	/**
	 * Reads for the length of data.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void readByteArray( byte[] data ) throws IOException {
		this.dataInputStream.readFully( data );
	}

}
