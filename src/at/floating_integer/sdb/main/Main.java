package at.floating_integer.sdb.main;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import at.floating_integer.sdb.server.Server;

public class Main {

	public static void main(String[] args) throws IOException {
		setupLogging();

		new Server(9999);
		System.in.read();
	}

	private static void setupLogging() {
		Logger l = Logger.getLogger("at.floating_integer.sdb");
		l.setLevel(Level.FINE);
		l.setUseParentHandlers(false);
		Handler h = new ConsoleHandler();
		h.setLevel(Level.ALL);
		l.addHandler(h);
	}
}
