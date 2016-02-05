package at.floating_integer.sdb.server;

import java.util.logging.Logger;

import at.floating_integer.sdb.command.ByeCommand;
import at.floating_integer.sdb.command.Command;
import at.floating_integer.sdb.command.GetCommand;
import at.floating_integer.sdb.command.ImaCommand;
import at.floating_integer.sdb.command.PutCommand;

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

	private String tmp = "default";

	private void requestNextCmd() {
		connection.enqueueRead(new Connection.Read() {
			@Override
			public void read(String msg) {
				Command c = Command.parse(msg);

				if (c == null) {
					// TODO raise error
					connection.enqueueWrite("got " + msg);
				}

				if (c instanceof ByeCommand) {
					connection.enqueueWrite("bye");
					connection.enqueueClose();
					return;
				}

				if (c instanceof GetCommand) {
					String key = ((GetCommand) c).getKey();
					connection.enqueueWrite("has " + key + " " + tmp);
				}

				if (c instanceof PutCommand) {
					String key = ((PutCommand) c).getKey();
					String data = ((PutCommand) c).getData();
					tmp = data;
					connection.enqueueWrite("has " + key + " " + tmp);
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
