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
