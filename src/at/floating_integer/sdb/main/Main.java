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

		Server s = new Server(9999);
		System.out.println("Press <Enter> to shut down server.");
		System.in.read();
		s.shutdown();
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
