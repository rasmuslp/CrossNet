package crossnet.message;

import java.io.IOException;
import java.util.List;

import crossnet.util.ByteArrayWriter;

/**
 * Abstract implementation of {@link Message} that has a List as as a part of the payload.
 * 
 * @author Rasmus Ljungmann Pedersen <rasmuslp@gmail.com>
 * 
 * @param <T>
 *            The list payload type.
 * @param <E>
 *            The MessageType enum.
 */
public abstract class AbstractListMessage< T, E extends Enum< E > > extends AbstractMessage< E > {

	/**
	 * The list payload.
	 */
	protected final List< T > list;

	public AbstractListMessage( E messageType, final List< T > list ) {
		super( messageType );
		if ( list == null ) {
			throw new IllegalArgumentException( "List cannot be null." );
		}
		this.list = list;
	}

	/**
	 * @return The list payload.
	 */
	public List< T > getList() {
		return this.list;
	}

	@Override
	protected void serializePayload( ByteArrayWriter to ) throws IOException {
		this.serializeStatic( to );
		int count = this.list.size();
		to.writeInt( count );
		for ( int i = 0; i < count; i++ ) {
			this.serializeListObject( i, to );
		}
	}

	/**
	 * Serialises any extra non-dynamic information necessary.
	 * 
	 * @param to
	 *            The destination of the serialisation.
	 * @throws IOException
	 *             If a serialisation error occurs.
	 */
	protected abstract void serializeStatic( ByteArrayWriter to ) throws IOException;

	/**
	 * Serialises the specific object of the {@link #list}.
	 * 
	 * @param atIndex
	 *            An index of the Object to serialise.
	 * @param to
	 *            The destination of the serialisation.
	 * @throws IOException
	 *             If a serialisation error occurs.
	 */
	protected abstract void serializeListObject( int atIndex, ByteArrayWriter to ) throws IOException;

}
