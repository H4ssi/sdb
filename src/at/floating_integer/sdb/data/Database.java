package at.floating_integer.sdb.data;

import java.util.HashMap;
import java.util.Map;

public class Database {
	private Map<String, Record> records = new HashMap<>();

	public void put(String key, Record record) {
		records.put(key, record);
	}

	public Record get(String key) {
		return records.get(key);
	}
}
