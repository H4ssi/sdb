package at.floating_integer.sdb.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
	private static final Logger L = Logger.getLogger(Server.class.getName());

	private final AsynchronousChannelGroup group = AsynchronousChannelGroup //
			.withFixedThreadPool(1, Executors.defaultThreadFactory());

	private final AsynchronousServerSocketChannel serverSocket;

	public Server(int port) throws IOException {
		serverSocket = AsynchronousServerSocketChannel.open(group);

		serverSocket.bind(new InetSocketAddress(port));

		accept();

		L.info("Server started listening on port " + port);
	}

	private void accept() {
		serverSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Void attachment) {
				accept();
				new Client(new ClientConnection(result));
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		});
	}

	public void shutdown() {
		try {
			group.shutdownNow();
			// TODO maybe be more graceful
		} catch (IOException e) {
			L.log(Level.WARNING, "Error when shutting down server", e);
		}
	}
}
