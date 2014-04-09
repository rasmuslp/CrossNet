package legacy.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import crossnet.packet.Packet;

public class TcpSocketClient implements Client {

	private final AbstractTcpSocketServerConnection abstractTcpSocketServerConnection;
	private final SocketChannel socketChannel;

	private final ByteBuffer readBuffer;

	private final Queue< ByteBuffer > sendQueue = new ConcurrentLinkedQueue<>();

	public TcpSocketClient( final AbstractTcpSocketServerConnection abstractTcpSocketServerConnection, final SocketChannel socketChannel, final int readBufferSize ) {
		this.abstractTcpSocketServerConnection = abstractTcpSocketServerConnection;
		this.socketChannel = socketChannel;
		//TODO Figure out what to do about messages larger than the buffer size
		this.readBuffer = ByteBuffer.allocate( readBufferSize );

	}

	public SocketChannel getSocketChannel() {
		return this.socketChannel;
	}

	public ByteBuffer getReadBuffer() {
		return this.readBuffer;
	}

	public Queue< ByteBuffer > getSendQueue() {
		return this.sendQueue;
	}

	@Override
	public void send( Packet packet ) {
		ByteBuffer writeBuffer = ByteBuffer.wrap( packet.getData() );
		this.sendQueue.add( writeBuffer );

		this.abstractTcpSocketServerConnection.wakeup();
	}
}
