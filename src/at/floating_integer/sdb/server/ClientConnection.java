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
package at.floating_integer.sdb.server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientConnection implements Connection {
	private static final int BUF_SIZE = 128;

	private static final Logger L = Logger.getLogger(ClientConnection.class.getName());
	private final AsynchronousSocketChannel socket;

	private final SocketAddress address;

	public ClientConnection(AsynchronousSocketChannel socket) {
		this.socket = socket;

		SocketAddress address = null;
		try {
			address = socket.getRemoteAddress();
			L.info("client connected: " + address);
		} catch (IOException e) {
			L.log(Level.WARNING, "could not get remote address of client", e);
			// TODO bailout?
		}
		this.address = address;
	}

	private abstract class Queue<T> {
		private final LinkedList<T> queue = new LinkedList<>();

		private boolean active = false;

		private Runnable closing;

		protected void end() {
			T next = queue.pollFirst();
			if (next == null) {
				active = false;
				if (closing != null) {
					closing.run();
				}
			} else {
				process(next);
			}
		}

		protected abstract void process(T next);

		public void enqueue(T op) {
			if (closing != null) {
				return;
			}
			if (!active) {
				active = true;
				process(op);
			} else {
				queue.addLast(op);
			}
		}

		public void close(Runnable onClose) {
			closing = onClose;
			if (!active) {
				onClose.run();
			}
		}
	}

	private class WriteQueue extends Queue<String> {
		private final CharsetEncoder e = Charset.forName("UTF-8").newEncoder();

		private final ByteBuffer writeBuf = ByteBuffer.allocateDirect(BUF_SIZE);

		@Override
		protected void process(String next) {
			encodePart(CharBuffer.wrap(next + "\n"));
		}

		private void encodePart(final CharBuffer msg) {
			final CoderResult r = e.encode(msg, writeBuf, false);
			if (r.isUnderflow()) {
				encodeEnd(msg);
			} else {
				writeBuf.flip();
				send(new Runnable() {
					@Override
					public void run() {
						encodePart(msg);
					}
				});
			}
		}

		private void encodeEnd(final CharBuffer msg) {
			CoderResult r = e.encode(msg, writeBuf, true);
			if (r.isUnderflow()) {
				flushEncoder();
			} else {
				writeBuf.flip();
				send(new Runnable() {
					@Override
					public void run() {
						encodeEnd(msg);
					}
				});
			}
		}

		private void flushEncoder() {
			CoderResult r = e.flush(writeBuf);
			writeBuf.flip();
			if (r.isUnderflow()) {
				e.reset();
				send(new Runnable() {
					@Override
					public void run() {
						end();
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
			socket.write(writeBuf, null, new CompletionHandler<Integer, Void>() {
				@Override
				public void completed(Integer result, Void attachment) {
					if (writeBuf.remaining() == 0) {
						writeBuf.clear();
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
	}

	private class ReadQueue extends Queue<Connection.Read> {
		private final CharsetDecoder d = Charset.forName("UTF-8").newDecoder();

		private final ByteBuffer readBuf = ByteBuffer.allocateDirect(BUF_SIZE);
		private final CharBuffer cbuf = CharBuffer.allocate((int) Math.ceil(d.maxCharsPerByte() * BUF_SIZE));

		private final StringBuilder input = new StringBuilder();

		@Override
		protected void process(Read next) {
			testForInput(next);
		}

		private void testForInput(Read next) {
			int pos = input.indexOf("\n");

			if (pos == -1) {
				read(next);
				return;
			}

			next.read(input.substring(0, pos));

			input.delete(0, pos + 1);

			end();
		}

		private void read(final Read next) {
			socket.read(readBuf, null, new CompletionHandler<Integer, Void>() {
				@Override
				public void completed(Integer result, Void attachment) {
					readBuf.flip();
					parseMessage(next);
				}

				@Override
				public void failed(Throwable exc, Void attachment) {
					// TODO Auto-generated method stub
				}
			});
		}

		private void parseMessage(Read next) {
			while (true) {
				CoderResult r = d.decode(readBuf, cbuf, false);
				cbuf.flip();

				input.append(cbuf.toString());
				cbuf.clear();

				if (r.isOverflow()) {
					continue;
				}

				readBuf.compact();
				testForInput(next);

				return;
			}
		}
	}

	private final WriteQueue writes = new WriteQueue();
	private final ReadQueue reads = new ReadQueue();

	@Override
	public void enqueueClose() {
		writes.close(new Runnable() {
			@Override
			public void run() {
				reads.close(new Runnable() {
					@Override
					public void run() {
						try {
							socket.close();
						} catch (IOException e) {
							L.log(Level.WARNING, "error closing channel", socket);
						}
					}
				});
			}
		});
	}

	@Override
	public void enqueueRead(final Read read) {
		reads.enqueue(read);
	}

	@Override
	public void enqueueWrite(final String msg) {
		writes.enqueue(msg);
	}

	@Override
	public String toString() {
		return address.toString();
	}
}
