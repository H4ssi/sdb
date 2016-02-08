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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBException;

import at.floating_integer.sdb.data.Database;
import at.floating_integer.sdb.data.Subscriptions;

public class Server {
	private static final Logger L = Logger.getLogger(Server.class.getName());

	private static final File DATA_FILE = new File("data.xml.gz");
	private static final File DATA_BACKUP_FILE = new File(DATA_FILE.getPath() + ".bak");

	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	private final AsynchronousChannelGroup group = AsynchronousChannelGroup //
			.withThreadPool(executor);

	private final AsynchronousServerSocketChannel serverSocket;

	private final Subscriptions subscriptions = new Subscriptions();
	private final Database database;

	private static final long STORE_INTERVAL_MILLIS = 10 * 60 * 1000;

	public Server(int port) throws IOException, JAXBException {
		database = new Database(subscriptions);

		restore();

		new Timer(true).schedule(new TimerTask() {
			@Override
			public void run() {
				submitStore();
			}
		}, STORE_INTERVAL_MILLIS, STORE_INTERVAL_MILLIS);

		serverSocket = AsynchronousServerSocketChannel.open(group);

		serverSocket.bind(new InetSocketAddress(port));

		accept();

		L.info("Server started listening on port " + port);
	}

	private void restore() throws IOException, JAXBException {
		try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(DATA_FILE))) {
			L.info("restoring data...");
			database.restore(is);
			L.info("data restored");
		} catch (FileNotFoundException e) {
			L.info("no data file found, no data restored");
		}
	}

	private void store() throws FileNotFoundException, IOException, JAXBException {
		Path tmp = Files.createTempFile(Paths.get(""), "data.xml.", ".gz.tmp");
		try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(tmp.toFile()))) {
			L.info("storing data into " + tmp + "...");
			database.store(os);
		}
		try {
			if (Files.exists(DATA_FILE.toPath())) {
				Files.move(DATA_FILE.toPath(), DATA_BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(tmp, DATA_FILE.toPath(), StandardCopyOption.ATOMIC_MOVE);
			L.info("data stored");
		} catch (IOException e) {
			L.log(Level.WARNING, "could not swap data files", e);
			throw e;
		}
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
			try {
				submitStore().get();
			} catch (InterruptedException e) {
				L.log(Level.WARNING, "interrupted while waiting for db store", e);
			} catch (ExecutionException e) {
				L.log(Level.WARNING, "error during final db store", e);
			}
			group.shutdownNow();
			// TODO maybe be more graceful
		} catch (IOException e) {
			L.log(Level.WARNING, "Error when shutting down server", e);
		}
	}

	private Future<Void> submitStore() {
		return executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					store();
				} catch (Exception e) {
					L.log(Level.WARNING, "error during db store", e);
					throw e;
				}
				return null;
			}
		});
	}
}
