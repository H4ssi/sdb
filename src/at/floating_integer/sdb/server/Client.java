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
import java.util.logging.Logger;

public class Client {
	private static final Logger L = Logger.getAnonymousLogger();
	private final AsynchronousSocketChannel socket;

	private final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
	private final CharBuffer cbuf = CharBuffer.allocate(1024);

	private final CharsetEncoder e = Charset.forName("UTF-8").newEncoder();
	private final CharsetDecoder d = Charset.forName("UTF-8").newDecoder();

	private final StringBuilder input = new StringBuilder();

	private interface CommandHandler {
		boolean handle(String cmd);
	}

	private CommandHandler currentHandler;
	protected String name;

	public Client(AsynchronousSocketChannel socket) {
		this.socket = socket;

		try {
			L.info("client connected: " + socket.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		start();
	}

	private void start() {
		this.currentHandler = new CommandHandler() {
			@Override
			public boolean handle(String cmd) {
				if (!cmd.startsWith("ima ")) {
					return false;
				}
				String name = cmd.substring(4).trim();

				if ("".equals(name)) {
					return false;
				}

				Client.this.name = name;

				L.info("client name is " + name);

				sendThenRead("has login " + Client.this.name);

				return true;
			}
		};
		sendThenRead("who");
	}

	private void sendThenRead(String msg) {
		sendThenDo(msg, new Runnable() {

			@Override
			public void run() {
				read();
			}
		});
	}

	private void sendThenDo(String msg, Runnable action) {
		sendThenDo(msg, 0, action);
	}

	private void sendThenDo(String msg, int pos, Runnable action) {
		int end = Math.min(msg.length(), pos + cbuf.remaining());

		cbuf.put(msg, pos, end);

		if (end == msg.length() && cbuf.hasRemaining()) {
			cbuf.put('\n');
			cbuf.flip();
			sendThenDo(action);
		} else {
			cbuf.flip();
			keepSendingThenDo(msg, end, action);
		}
	}

	private void keepSendingThenDo(final String msg, final int pos, final Runnable action) {
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
				sendThenDo(msg, pos, action);
			}
		});
	}

	private void sendThenDo(final Runnable action) {
		CoderResult r = e.encode(cbuf, buf, true);
		if (r.isUnderflow()) {
			cbuf.clear();
			flushThenDo(action);
		} else {
			buf.flip();
			send(new Runnable() {
				@Override
				public void run() {
					sendThenDo(action);
				}
			});
		}
	}

	private void flushThenDo(final Runnable action) {
		CoderResult r = e.flush(buf);
		buf.flip();
		if (r.isUnderflow()) {
			e.reset();
			send(action);
		} else {
			send(new Runnable() {

				@Override
				public void run() {
					flushThenDo(action);
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

		onData(input.substring(0, pos));

		input.delete(0, pos + 1);
	}

	private void onData(String data) {
		if (currentHandler == null) {
			sendError();
		}
		if (!currentHandler.handle(data)) {
			sendError();
		}
	}

	private void sendError() {
		sendThenDo("err", new Runnable() {

			@Override
			public void run() {
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
}
