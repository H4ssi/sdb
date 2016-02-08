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
package at.floating_integer.sdb.main;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import at.floating_integer.sdb.server.Server;

public class Main {

	public static void main(String[] args) throws IOException, JAXBException {
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
