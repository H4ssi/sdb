package at.floating_integer.sdb.command;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Command {
	private final String name;

	protected Command(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	interface TokenParser<T> {
		T parseToken(String token);
	}

	interface Tokens {
		<T> T parse(TokenParser<T> parser);
	}

	private static class TokensImpl implements Tokens {
		private final String[] tokens;
		private int i = 0;

		public TokensImpl(String[] tokens) {
			super();
			this.tokens = tokens;
		}

		@Override
		public <T> T parse(TokenParser<T> parser) {
			if (i == tokens.length) {
				throw new IllegalStateException("too few args");
			}
			return parser.parseToken(tokens[i++]);
		}

		public void end() { // TODO custom exception class
			if (i != tokens.length) {
				throw new IllegalStateException("arg tokens left");
			}
		}
	}

	abstract static class CommandParser {
		private final String name;

		public CommandParser(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Command parse(String[] tokens) {

			TokensImpl ts = new TokensImpl(tokens);

			ts.parse(new TokenParser<Void>() {

				@Override
				public Void parseToken(String token) {
					if (!name.equals(token)) {
						throw new IllegalArgumentException("cannot parse");
					}
					return null;
				}
			});

			Command command = parse(ts);

			ts.end();

			return command;
		}

		protected abstract Command parse(Tokens argTokens);
	}

	private static Map<String, CommandParser> PARSERS = new HashMap<String, CommandParser>() {
		{
			put(new ImaCommand.ImaCommandParser());
		}

		void put(CommandParser parser) {
			put(parser.getName(), parser);
		}

		private static final long serialVersionUID = 1L;
	};

	public static Command parse(String cmdLine) {
		String[] tokens = splitTokens(cmdLine);
		if (tokens.length == 0) {
			return null;
		}
		CommandParser p = PARSERS.get(tokens[0]);
		if (p == null) {
			return null;
		}
		return p.parse(tokens);

	}

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	static String[] splitTokens(String cmdLine) {
		return WHITESPACE.split(cmdLine);
	}
}
