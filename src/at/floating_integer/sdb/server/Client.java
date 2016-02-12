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

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.floating_integer.sdb.command.ByeCommand;
import at.floating_integer.sdb.command.Command;
import at.floating_integer.sdb.command.Command.ParserException;
import at.floating_integer.sdb.command.GetCommand;
import at.floating_integer.sdb.command.IamCommand;
import at.floating_integer.sdb.command.PutCommand;
import at.floating_integer.sdb.command.SubCommand;
import at.floating_integer.sdb.data.Database;
import at.floating_integer.sdb.data.Record;
import at.floating_integer.sdb.data.Subscriptions;

public class Client {
	private static final Logger L = Logger.getLogger(Client.class.getName());

	private final Connection connection;
	private final Database database;
	private final Subscriptions subscriptions;
	private final PrintWriter log;

	private String name;

	public Client(Connection connection, Database database, Subscriptions subscriptions, PrintWriter log) {
		super();
		this.connection = connection;
		this.database = database;
		this.subscriptions = subscriptions;
		this.log = log;
		start();
	}

	private void start() {
		dblog("con");

		connection.onClosed(new Runnable() {
			@Override
			public void run() {
				if (subscriptions.unsubscribe(Client.this)) {
					dblog("uns");
				}
				dblog("dis");
				L.info(name + " disconnected.");
			}
		});

		connection.enqueueWrite("who");
		requestLogin();
	}

	private void requestLogin() {
		connection.enqueueRead(new Connection.Read() {

			@Override
			public void read(String cmd) {
				try {
					Command c = Command.parse(cmd);

					if (c == null) {
						requestLogin();
						return;
					}
					if (c instanceof IamCommand) {
						dblog(cmd);

						name = ((IamCommand) c).getUserName();
						L.info("client name is " + name);

						connection.enqueueWrite("has login " + new Record("system", name));
						requestNextCmd();
					} else if (c instanceof ByeCommand) {
						dblog(cmd);

						bye();
					} else {
						error();
						requestLogin();
					}
				} catch (ParserException e) {
					error();
					requestLogin();
				}
			}
		});
	}

	private void requestNextCmd() {
		connection.enqueueRead(new Connection.Read() {
			@Override
			public void read(String msg) {
				try {
					try {
						Command c = Command.parse(msg);

						if (c != null) {
							if (subscriptions.unsubscribe(Client.this)) {
								connection.enqueueWrite("nil");
								dblog("uns");
							}

							dblog(msg);

							if (c instanceof ByeCommand) {
								bye();
								return;
							} else if (c instanceof GetCommand) {
								String key = ((GetCommand) c).getKey();
								getRecord(key);
							} else if (c instanceof PutCommand) {
								String key = ((PutCommand) c).getKey();
								String data = ((PutCommand) c).getData();
								Record rec = new Record(name, data);
								database.put(key, rec);
								connection.enqueueWrite("has " + key + " " + rec);
							} else if (c instanceof SubCommand) {
								String key = ((SubCommand) c).getKey();

								subscriptions.subscribe(Client.this, key);
								getRecord(key);
							} else {
								error();
							}
						}
					} catch (ParserException e) {
						error();
					}
					requestNextCmd();
				} catch (Exception e) {
					L.log(Level.WARNING, "unhandled exception during client logic", e);
					connection.enqueueClose();
				}
			}

			private void getRecord(String key) {
				Record rec = database.get(key);
				if (rec == null) {
					connection.enqueueWrite("nil");
				} else {
					connection.enqueueWrite("has " + key + " " + rec);
				}
			}
		});
	}

	private void error() {
		connection.enqueueWrite("err");
	}

	public void recordPut(String key, Record record) {
		connection.enqueueWrite("has " + key + " " + record);
	}

	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private void dblog(String msg) {
		log.println(connection + " " + FORMAT.format(new Date()) + " " + msg);
	}

	private void bye() {
		connection.enqueueWrite("bye");
		connection.enqueueClose();
	}
}
