package crossnet.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;

/**
 * Simple logging framework
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class Log {

	/**
	 * Current LogLevel.
	 */
	static LogLevel currentLogLevel;

	public static boolean TRACE;
	public static boolean DEBUG;
	public static boolean INFO;
	public static boolean WARN;
	public static boolean ERROR;

	/**
	 * Static initialisation of default log level.
	 */
	static {
		Log.set( LogLevel.INFO );
	}

	/**
	 * Change LogLevel.
	 * 
	 * @param level
	 *            The new LogLevel.
	 */
	public static void set( LogLevel level ) {
		Log.currentLogLevel = level;
		Log.TRACE = Log.currentLogLevel.ordinal() <= LogLevel.TRACE.ordinal();
		Log.DEBUG = Log.currentLogLevel.ordinal() <= LogLevel.DEBUG.ordinal();
		Log.INFO = Log.currentLogLevel.ordinal() <= LogLevel.INFO.ordinal();
		Log.WARN = Log.currentLogLevel.ordinal() <= LogLevel.WARN.ordinal();
		Log.ERROR = Log.currentLogLevel.ordinal() <= LogLevel.ERROR.ordinal();
	}

	/**
	 * Default logger
	 */
	static private Logger logger = new Logger();

	/**
	 * Change logger.
	 * 
	 * @param logger
	 *            The new logger to use.
	 */
	public static void setLogger( Logger logger ) {
		Log.logger = logger;
	}

	public static void trace( String message ) {
		Log.logger.log( LogLevel.TRACE, null, message, null );
	}

	public static void trace( String group, String message ) {
		Log.logger.log( LogLevel.TRACE, group, message, null );
	}

	public static void trace( String message, Throwable throwable ) {
		Log.logger.log( LogLevel.TRACE, null, message, throwable );
	}

	public static void trace( String group, String message, Throwable throwable ) {
		Log.logger.log( LogLevel.TRACE, group, message, throwable );
	}

	public static void debug( String message ) {
		Log.logger.log( LogLevel.DEBUG, null, message, null );
	}

	public static void debug( String group, String message ) {
		Log.logger.log( LogLevel.DEBUG, group, message, null );
	}

	public static void debug( String message, Throwable throwable ) {
		Log.logger.log( LogLevel.DEBUG, null, message, throwable );
	}

	public static void debug( String group, String message, Throwable throwable ) {
		Log.logger.log( LogLevel.DEBUG, group, message, throwable );
	}

	public static void info( String message ) {
		Log.logger.log( LogLevel.INFO, null, message, null );
	}

	public static void info( String group, String message ) {
		Log.logger.log( LogLevel.INFO, group, message, null );
	}

	public static void info( String message, Throwable throwable ) {
		Log.logger.log( LogLevel.INFO, null, message, throwable );
	}

	public static void info( String group, String message, Throwable throwable ) {
		Log.logger.log( LogLevel.INFO, group, message, throwable );
	}

	public static void warn( String message ) {
		Log.logger.log( LogLevel.WARN, null, message, null );
	}

	public static void warn( String group, String message ) {
		Log.logger.log( LogLevel.WARN, group, message, null );
	}

	public static void warn( String message, Throwable throwable ) {
		Log.logger.log( LogLevel.WARN, null, message, throwable );
	}

	public static void warn( String group, String message, Throwable throwable ) {
		Log.logger.log( LogLevel.WARN, group, message, throwable );
	}

	public static void error( String message ) {
		Log.logger.log( LogLevel.ERROR, null, message, null );
	}

	public static void error( String group, String message ) {
		Log.logger.log( LogLevel.ERROR, group, message, null );
	}

	public static void error( String message, Throwable throwable ) {
		Log.logger.log( LogLevel.ERROR, null, message, throwable );
	}

	public static void error( String group, String message, Throwable throwable ) {
		Log.logger.log( LogLevel.ERROR, group, message, throwable );
	}

	/**
	 * Default logger.
	 * 
	 * Logs to standard output.
	 * 
	 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
	 * 
	 */
	public static class Logger {

		private long firstTimeStamp = new Date().getTime();

		@SuppressWarnings( "incomplete-switch" )
		public void log( LogLevel level, String group, String message, Throwable throwable ) {
			if ( level.ordinal() < Log.currentLogLevel.ordinal() ) {
				// Return if supplied log level is below the set one.
				return;
			}

			StringBuilder builder = new StringBuilder( 128 );

			// Add time stamp
			Format format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			String timeStamp = '[' + format.format( new Date() ) + "] ";
			builder.append( timeStamp );

			// Add time stamp
			long time = new Date().getTime() - this.firstTimeStamp;
			long minutes = time / ( 1000 * 60 );
			long seconds = time / ( 1000 ) % 60;
			long millis = time - ( minutes * 1000 * 60 ) - ( seconds * 1000 );
			if ( minutes < 10 ) {
				builder.append( '0' );
			}
			builder.append( minutes );
			builder.append( ':' );
			if ( seconds < 10 ) {
				builder.append( '0' );
			}
			builder.append( seconds );
			builder.append( ':' );
			if ( millis < 100 ) {
				builder.append( '0' );
			}
			if ( millis < 10 ) {
				builder.append( '0' );
			}
			builder.append( millis );

			// Add log level
			switch ( level ) {
				case TRACE:
					builder.append( " TRACE: " );
					break;
				case DEBUG:
					builder.append( " DEBUG: " );
					break;
				case INFO:
					builder.append( "  INFO: " );
					break;
				case WARN:
					builder.append( "  WARN: " );
					break;
				case ERROR:
					builder.append( " ERROR: " );
					break;
			}

			// Add group
			if ( group != null ) {
				builder.append( '[' );
				builder.append( group );
				builder.append( "] " );
			}

			// Add message
			builder.append( message.trim() + ' ' );

			// Add source location
			if ( Log.currentLogLevel.ordinal() < LogLevel.INFO.ordinal() ) {
				StackTraceElement el = Thread.currentThread().getStackTrace()[3];
				String location = "";
				try ( Formatter formatter = new Formatter() ) {
					location = formatter.format( "%s:%d", el.getFileName(), el.getLineNumber() ).toString();
				}
				builder.append( '(' + location + ')' );
			}

			// Add throwable
			if ( throwable != null ) {
				StringWriter writer = new StringWriter( 256 );
				throwable.printStackTrace( new PrintWriter( writer ) );
				builder.append( '\n' );
				builder.append( writer.toString().trim() );
			}

			System.out.println( builder.toString() );
		}

	}
}