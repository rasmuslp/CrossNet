package crossnet.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Wrapper writer class for a byte[].
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ByteArrayWriter {

	private final ByteArrayOutputStream byteArrayOutputStream;
	private final DataOutputStream dataOutputStream;

	public ByteArrayWriter() {
		this.byteArrayOutputStream = new ByteArrayOutputStream();
		this.dataOutputStream = new DataOutputStream( this.byteArrayOutputStream );
	}

	public void writeBoolean( boolean b ) throws IOException {
		this.dataOutputStream.writeBoolean( b );
	}

	public void writeByte( int number ) throws IOException {
		if ( number < Byte.MIN_VALUE || 255 < number ) {
			throw new IllegalArgumentException( "Number requires more than one byte: " + number );
		}

		this.dataOutputStream.writeByte( number );
	}

	public void writeShort( int number ) throws IOException {
		if ( number < Short.MIN_VALUE || 65535 < number ) {
			throw new IllegalArgumentException( "Number requires more than two bytes: " + number );
		}

		this.dataOutputStream.writeShort( number );
	}

	public void writeInt( int number ) throws IOException {
		this.dataOutputStream.writeInt( number );
	}

	public void writeLong( long number ) throws IOException {
		this.dataOutputStream.writeLong( number );
	}

	public void writeByteArray( byte[] data ) throws IOException {
		this.dataOutputStream.write( data );
	}

	public byte[] toByteArray() throws IOException {
		this.dataOutputStream.flush();

		return this.byteArrayOutputStream.toByteArray();
	}

}
