package crossnet;

public class ConnectionIDGenetator {

	private int nextConnectionID = 1;

	public int getNextConnectionID() {
		return this.nextConnectionID++;
	}

}
