package at.floating_integer.sdb.command;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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

		String slurp();

		static final TokenParser<String> ANY = new TokenParser<String>() {
			@Override
			public String parseToken(String token) {
				return token;
			}
		};
	}

	private static class TokensImpl implements Tokens {
		private final Matcher m;
		private int i = 0;

		public TokensImpl(Matcher m) {
			super();
			this.m = m;
		}

		@Override
		public <T> T parse(TokenParser<T> parser) {
			if (m.hitEnd() || !m.find()) {
				throw new IllegalStateException("too few args");
			}
			return parser.parseToken(m.group(0));
		}

		@Override
		public String slurp() {
			if (m.hitEnd()) {
				throw new IllegalStateException("nothing left to slurp");
			}
			m.usePattern(SLURP);
			if (m.find()) {
				return m.group(SLURP_GROUP);
			} else {
				return "";
			}
		}

		public void end() { // TODO custom exception class
			m.usePattern(TOKEN);
			if (m.find()) {
				throw new IllegalStateException("arg tokens left");
			}
		}
	}

	abstract static class Parser {
		private final String name;

		public Parser(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Command parse(Matcher m) {

			TokensImpl tokens = new TokensImpl(m);

			Command command = parse(tokens);

			tokens.end();

			return command;
		}

		protected abstract Command parse(Tokens argTokens);
	}

	private static Map<String, Parser> PARSERS = new HashMap<String, Parser>() {
		{
			put(new ImaCommand.Parser());
			put(new ByeCommand.Parser());
			put(new GetCommand.Parser());
			put(new PutCommand.Parser());
		}

		void put(Parser parser) {
			put(parser.getName(), parser);
		}

		private static final long serialVersionUID = 1L;
	};

	public static Command parse(String cmdLine) {
		Matcher m = TOKEN.matcher(cmdLine);

		if (!m.find()) {
			return null;
		}
		Parser p = PARSERS.get(m.group(TOKEN_GROUP));
		if (p == null) {
			return null;
		}
		return p.parse(m);
	}

	private static final Pattern TOKEN = Pattern.compile("\\S+");
	private static final int TOKEN_GROUP = 0;
	private static final Pattern SLURP = Pattern.compile("\\s(.*)$");
	private static final int SLURP_GROUP = 1;
}
