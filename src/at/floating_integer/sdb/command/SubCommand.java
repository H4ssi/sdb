package at.floating_integer.sdb.command;

public class SubCommand extends Command {
	private static final String NAME = "sub";

	private final String key;

	public SubCommand(String key) {
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
			return new SubCommand(key);
		}
	}
}
