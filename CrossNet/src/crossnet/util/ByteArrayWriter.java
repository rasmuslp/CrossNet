package crossnet.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

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

	/**
	 * Converts a String to a UTF-8 byte array and writes it to the byte[].
	 * <p>
	 * NB: After conversion, the length must not exceed 255 bytes.
	 * 
	 * @param string
	 *            The String to write.
	 * @throws IOException
	 */
	public void writeString255( String string ) throws IOException {
		byte[] stringBytes = string.getBytes( Charset.forName( "UTF-8" ) );
		this.writeByte( stringBytes.length );
		this.writeByteArray( stringBytes );
	}

	public void writeByteArray( byte[] data ) throws IOException {
		this.dataOutputStream.write( data );
	}

	public byte[] toByteArray() throws IOException {
		this.dataOutputStream.flush();

		return this.byteArrayOutputStream.toByteArray();
	}

}
