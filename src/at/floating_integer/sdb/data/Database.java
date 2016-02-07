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

import java.util.HashMap;
import java.util.Map;

public class Database {
	private Map<String, Record> records = new HashMap<>();

	private final Subscriptions subscriptions;

	public Database(Subscriptions subscriptions) {
		this.subscriptions = subscriptions;
	}

	public void put(String key, Record record) {
		records.put(key, record);
		subscriptions.recordPut(key, record);
	}

	public Record get(String key) {
		return records.get(key);
	}
}
