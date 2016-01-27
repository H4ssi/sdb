package at.floating_integer.sdb.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Logger;

public class Server {
	private static final Logger L = Logger.getAnonymousLogger();

	public Server(int port) throws IOException {
		AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open();

		serverSocket.bind(new InetSocketAddress(port));

		accept(serverSocket);

		L.info("Server started listening on port " + port);
	}

	public void accept(final AsynchronousServerSocketChannel serverSocket) {
		serverSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Void attachment) {
				accept(serverSocket);
				new ClientConnection(result);
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub

			}
		});
	}

}
