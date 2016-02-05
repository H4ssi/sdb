package at.floating_integer.sdb.server;

import java.util.logging.Logger;

import at.floating_integer.sdb.command.ByeCommand;
import at.floating_integer.sdb.command.Command;
import at.floating_integer.sdb.command.ImaCommand;

public class Client {
	private static final Logger L = Logger.getLogger(Client.class.getName());

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
				Command c = Command.parse(msg);

				if (c == null) {
					connection.enqueueWrite("got " + msg);
				}

				if (c instanceof ByeCommand) {
					connection.enqueueWrite("bye");
					connection.enqueueClose();
					return;
				}
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
