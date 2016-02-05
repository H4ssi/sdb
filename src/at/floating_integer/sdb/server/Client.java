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

import java.util.logging.Logger;

import at.floating_integer.sdb.command.ByeCommand;
import at.floating_integer.sdb.command.Command;
import at.floating_integer.sdb.command.GetCommand;
import at.floating_integer.sdb.command.ImaCommand;
import at.floating_integer.sdb.command.PutCommand;
import at.floating_integer.sdb.data.Database;
import at.floating_integer.sdb.data.Record;

public class Client {
	private static final Logger L = Logger.getLogger(Client.class.getName());

	private final Connection connection;
	private final Database database;

	private String name;

	public Client(Connection connection, Database database) {
		super();
		this.connection = connection;
		this.database = database;
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
					Record rec = database.get(key);
					if (rec == null) {
						connection.enqueueWrite("nil");
					} else {
						connection.enqueueWrite("has " + key + " " + rec);
					}
				}

				if (c instanceof PutCommand) {
					String key = ((PutCommand) c).getKey();
					String data = ((PutCommand) c).getData();
					Record rec = new Record(name, data);
					database.put(key, rec);
					connection.enqueueWrite("has " + key + " " + rec);
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
