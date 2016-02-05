package at.floating_integer.sdb.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.LinkedList;
import java.util.logging.Logger;

public class ClientConnection implements Connection {
	private static final Logger L = Logger.getLogger(ClientConnection.class.getName());
	private final AsynchronousSocketChannel socket;

	private final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
	private final CharBuffer cbuf = CharBuffer.allocate(1024);

	private final CharsetEncoder e = Charset.forName("UTF-8").newEncoder();
	private final CharsetDecoder d = Charset.forName("UTF-8").newDecoder();

	private final StringBuilder input = new StringBuilder();

	public ClientConnection(AsynchronousSocketChannel socket) {
		this.socket = socket;

		try {
			L.info("client connected: " + socket.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void send(String msg) {
		copyIntoBuf(msg, 0);
	}

	private void copyIntoBuf(String msg, int pos) {
		int end = Math.min(msg.length(), pos + cbuf.remaining());

		cbuf.put(msg, pos, end);

		if (end == msg.length() && cbuf.hasRemaining()) {
			cbuf.put('\n');
			cbuf.flip();
			encodeEnd();
		} else {
			cbuf.flip();
			encodePartAndContinueCopying(msg, end);
		}
	}

	private void encodePartAndContinueCopying(final String msg, final int pos) {
		CoderResult r = e.encode(cbuf, buf, false);
		buf.flip();
		if (r.isUnderflow()) {
			cbuf.clear();
		} else {
			cbuf.compact();
		}
		send(new Runnable() {
			@Override
			public void run() {
				copyIntoBuf(msg, pos);
			}
		});
	}

	private void encodeEnd() {
		CoderResult r = e.encode(cbuf, buf, true);
		if (r.isUnderflow()) {
			cbuf.clear();
			flushEncoder();
		} else {
			buf.flip();
			send(new Runnable() {
				@Override
				public void run() {
					encodeEnd();
				}
			});
		}
	}

	private void flushEncoder() {
		CoderResult r = e.flush(buf);
		buf.flip();
		if (r.isUnderflow()) {
			e.reset();
			send(new Runnable() {
				@Override
				public void run() {
					continueQueue();
				}
			});
		} else {
			send(new Runnable() {
				@Override
				public void run() {
					flushEncoder();
				}
			});
		}
	}

	private void send(final Runnable then) {
		socket.write(buf, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				if (buf.remaining() == 0) {
					buf.clear();
					then.run();
				} else {
					send(then);
				}
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		});
	}

	private void read() {
		socket.read(buf, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				buf.flip();
				parseMessage();
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		});
	}

	private void parseMessage() {
		while (true) {
			CoderResult r = d.decode(buf, cbuf, false);
			cbuf.flip();

			input.append(cbuf.toString());
			cbuf.clear();

			if (r.isOverflow()) {
				continue;
			}

			buf.compact();
			testForInput();

			return;
		}
	}

	private void testForInput() {
		int pos = input.indexOf("\n");

		if (pos == -1) {
			read();
			return;
		}

		current.read(input.substring(0, pos)); // TODO assert not null

		input.delete(0, pos + 1);

		continueQueue();
	}

	private Connection.Read current;

	private final LinkedList<Runnable> ops = new LinkedList<>();

	@Override
	public void enqueueClose() {
		enqueue(new Runnable() {
			@Override
			public void run() {
				try {
					socket.close();
					continueQueue();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void enqueueRead(final Read read) {
		// TODO Auto-generated method stub
		enqueue(new Runnable() {
			@Override
			public void run() {
				current = read;
				read();
			}
		});
	}

	@Override
	public void enqueueWrite(final String msg) {
		enqueue(new Runnable() {
			@Override
			public void run() {
				send(msg);
			}
		});
	}

	private boolean processing = false;

	private void enqueue(Runnable runnable) {
		ops.addLast(runnable);
		if (!processing) {
			continueQueue();
		}
	}

	private void continueQueue() {
		Runnable next = ops.pollFirst();
		if (next == null) {
			processing = false;
			return;
		} else {
			processing = true;
			next.run();
		}
	}
}
