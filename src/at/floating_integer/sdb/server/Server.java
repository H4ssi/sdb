/*******************************************************************************
 * sdb - a simple database with an even simpler tcp protocol
 * Copyright (C) 2016 Florian Hassanen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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

import at.floating_integer.sdb.data.Database;
import at.floating_integer.sdb.data.Subscriptions;

public class Server {
	private static final Logger L = Logger.getLogger(Server.class.getName());

	private final AsynchronousChannelGroup group = AsynchronousChannelGroup //
			.withFixedThreadPool(1, Executors.defaultThreadFactory());

	private final AsynchronousServerSocketChannel serverSocket;

	private final Subscriptions subscriptions = new Subscriptions();
	private final Database database = new Database(subscriptions);

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
				new Client(new ClientConnection(result), database, subscriptions);
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
