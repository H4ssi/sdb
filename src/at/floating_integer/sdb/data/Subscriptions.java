package at.floating_integer.sdb.data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import at.floating_integer.sdb.server.Client;

public class Subscriptions {
	private final Map<String, Set<Client>> keyToClients = new HashMap<>();
	private final Map<Client, String> clientToKey = new HashMap<>();

	public void subscribe(Client client, String key) {
		clientToKey.put(client, key);
		Set<Client> clients = keyToClients.get(key);
		if (clients == null) {
			clients = new LinkedHashSet<>();
			keyToClients.put(key, clients);
		}
		clients.add(client);
	}

	public void recordPut(String key, Record record) {
		Set<Client> clients = keyToClients.get(key);
		if (clients == null) {
			return;
		}
		for (Client client : clients) {
			client.recordPut(key, record);
		}
	}

	public boolean unsubscribe(Client client) {
		String key = clientToKey.get(client);
		if (key == null) {
			return false;
		}
		keyToClients.get(key).remove(client);
		return true;
	}
}
