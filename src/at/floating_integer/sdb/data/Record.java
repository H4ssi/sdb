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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Record {
	private final String author;
	private final Date datetime = new Date();
	private final String data;

	public Record(String author, String data) {
		this.author = author;
		this.data = data;
	}

	public String getAuthor() {
		return author;
	}

	public Date getDatetime() {
		return new Date(datetime.getTime());
	}

	public String getData() {
		return data;
	}

	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");

	@Override
	public String toString() {
		return author + " " + FORMAT.format(datetime) + " " + data;
	}
}
