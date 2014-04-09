package crossnet.util;

public class CrossNetUtil {

	public static String bytesToHex( byte[] in ) {
		final StringBuilder builder = new StringBuilder();
		for ( byte b : in ) {
			builder.append( String.format( "%02x", b ) );
		}
		return builder.toString();
	}

}
