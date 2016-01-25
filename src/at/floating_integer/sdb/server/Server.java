package at.floating_integer.sdb.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

public class Server {
	private static final Logger L = Logger.getAnonymousLogger();

	public Server(int port) throws IOException {
		AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open();

		serverSocket.bind(new InetSocketAddress(port));

		serverSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Void attachment) {
				handle(result);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub

			}
		});

		L.info("Server started listening on port " + port);
	}

	protected void handle(AsynchronousSocketChannel result) {
		try {
			L.info("client connected: " + result.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.write(ByteBuffer.wrap("hallo!\n".getBytes()));
	}
}
