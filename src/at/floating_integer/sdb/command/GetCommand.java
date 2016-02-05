package at.floating_integer.sdb.command;

public class GetCommand extends Command {
	private static final String NAME = "get";

	private final String key;

	public GetCommand(String key) {
		super(NAME);

		this.key = key;
	}

	public String getKey() {
		return key;
	}

	static class Parser extends Command.Parser {
		public Parser() {
			super(NAME);
		}

		@Override
		protected Command parse(Tokens argTokens) {
			String key = argTokens.parse(Tokens.ANY);

			return new GetCommand(key);
		}
	}
}
