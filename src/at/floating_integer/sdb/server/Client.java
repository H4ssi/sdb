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
		sendThenRead("who");
	}

	private void sendThenRead(String msg) {
		sendThenRead(msg, 0);
	}

	private void sendThenRead(String msg, int pos) {
		int end = Math.min(msg.length(), pos + cbuf.remaining());

		cbuf.put(msg, pos, end);

		if (end == msg.length() && cbuf.hasRemaining()) {
			cbuf.put('\n');
			cbuf.flip();
			sendThenRead();
		} else {
			cbuf.flip();
			sendAndKeepSending(msg, end);
		}
	}

	public void sendAndKeepSending(final String msg, final int pos) {
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
				sendThenRead(msg, pos);
			}
		});
	}

	public void sendThenRead() {
		CoderResult r = e.encode(cbuf, buf, true);
		if (r.isUnderflow()) {
			cbuf.clear();
			flushThenRead();
		} else {
			buf.flip();
			send(new Runnable() {
				@Override
				public void run() {
					sendThenRead();
				}
			});
		}
	}

	public void flushThenRead() {
		CoderResult r = e.flush(buf);
		buf.flip();
		send(r.isUnderflow() //
				? new Runnable() {
					@Override
					public void run() {
						read();
					}
				} //
				: new Runnable() {
					@Override
					public void run() {
						flushThenRead();
					}
				});
	}

	public void send(final Runnable then) {
		socket.write(buf, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				buf.clear();
				then.run();
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		});
	}

	public void read() {
		socket.read(buf, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				cbuf.clear();
				buf.flip();
				d.decode(buf, cbuf, true);
				cbuf.flip();

				onData(cbuf.toString());
			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub
			}
		});
	}

	public void onData(String data) {
		L.info("client name is " + data);

		sendThenRead("has login " + data);
	}
}
