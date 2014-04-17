package crossnet;

/**
 * Generates unique IDs for {@link Connection}s.
 * <p>
 * NB: Per instance of this.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 */
public class ConnectionIDGenetator {

	private int nextId = 1;

	/**
	 * Gets the next unique ID.
	 * 
	 * @return The next unique ID.
	 */
	public int getNextId() {
		return this.nextId++;
	}

}
