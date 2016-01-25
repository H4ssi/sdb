package at.floating_integer.sdb.main;

import java.io.IOException;

import at.floating_integer.sdb.server.Server;

public class Main {

	public static void main(String[] args) throws IOException {
		new Server(9999);
		System.in.read();
	}
}
