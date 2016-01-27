package at.floating_integer.sdb.server;

import java.util.logging.Logger;

import at.floating_integer.sdb.command.Command;
import at.floating_integer.sdb.command.ImaCommand;

public class Client {
	private static final Logger L = Logger.getAnonymousLogger();

	private final Connection connection;

	private String name;

	public Client(Connection connection) {
		super();
		this.connection = connection;
		start();
	}

	private void start() {
		connection.enqueueWrite("who");
		connection.enqueueRead(new Connection.Read() {

			@Override
			public void read(String cmd) {
				Command c = Command.parse(cmd);

				if (!(c instanceof ImaCommand)) {
					error();
					return;
				}

				name = ((ImaCommand) c).getUserName();
				L.info("client name is " + name);

				connection.enqueueWrite("has login " + name);
				requestNextCmd();
			}
		});
	}

	private void requestNextCmd() {
		connection.enqueueRead(new Connection.Read() {
			@Override
			public void read(String msg) {
				if ("bye".equals(msg.trim())) { // TODO proper command
					connection.enqueueWrite("bye");
					connection.enqueueClose();
					return;
				}
				connection.enqueueWrite("got " + msg);
				requestNextCmd();
			}
		});
		// TODO implement
	}

	private void error() {
		connection.enqueueWrite("err");
		connection.enqueueClose();
	}
}
