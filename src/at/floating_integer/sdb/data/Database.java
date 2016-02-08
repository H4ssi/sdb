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
package at.floating_integer.sdb.data;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class Database {
	private HashMap<String, Record> records = new HashMap<>();

	private final Subscriptions subscriptions;

	public Database(Subscriptions subscriptions) throws JAXBException {
		this.subscriptions = subscriptions;

		JAXBContext context = JAXBContext.newInstance(HashMap.class, Wrapper.class, Record.class);
		marshaller = context.createMarshaller();
		unmarshaller = context.createUnmarshaller();
	}

	public void put(String key, Record record) {
		records.put(key, record);
		subscriptions.recordPut(key, record);
	}

	public Record get(String key) {
		return records.get(key);
	}

	@XmlRootElement
	private static class Wrapper {
		@XmlElement
		public HashMap<String, Record> records = new HashMap<>();
	}

	private final Marshaller marshaller;
	private final Unmarshaller unmarshaller;

	public void store(OutputStream outputStream) throws JAXBException {
		Wrapper w = new Wrapper();
		w.records = records;
		marshaller.marshal(w, outputStream);
	}

	public void restore(InputStream inputStream) throws JAXBException {
		Object o = unmarshaller.unmarshal(inputStream);
		if (!(o instanceof Wrapper)) {
			throw new RuntimeException("could not load database, given dump does not represent data");
		}
		Wrapper w = (Wrapper) o;
		records = w.records;
	}
}
