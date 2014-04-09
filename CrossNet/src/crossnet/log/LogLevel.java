package crossnet.log;

/**
 * The different log levels.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public enum LogLevel {
	/**
	 * Traces. For bug hunting as it logs a lot of information.
	 */
	TRACE,

	/**
	 * Debug output. Informative messages for development.
	 */
	DEBUG,

	/**
	 * Standard output. Informative messages about the state.
	 */
	INFO,

	/**
	 * Warnings. Problems that _do not_ hinder the normal operation.
	 */
	WARN,

	/**
	 * Critical errors. Problems that _might_ hinder the normal operation.
	 */
	ERROR,

	/**
	 * Logging is disabled.
	 */
	NONE
}