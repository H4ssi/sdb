package at.floating_integer.sdb.server;

public interface Connection {
	public interface Read {
		void read(String msg);
	}
	void enqueueWrite(String msg);
	void enqueueRead(Read read);
	void enqueueClose();
}
