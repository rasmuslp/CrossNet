package crossnet.util;

public class CrossNetUtil {

	public static String bytesToHex( byte[] in ) {
		final StringBuilder builder = new StringBuilder();
		for ( byte b : in ) {
			builder.append( String.format( "%02x:", b ) );
		}
		if ( builder.length() > 0 ) {
			builder.setLength( builder.length() - 1 );
		}
		return builder.toString();
	}

}
