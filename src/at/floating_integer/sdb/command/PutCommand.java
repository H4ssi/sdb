package at.floating_integer.sdb.command;

public class PutCommand extends Command {
	private static final String NAME = "put";

	private final String key;
	private final String data;

	public PutCommand(String key, String data) {
		super(NAME);
		this.key = key;
		this.data = data;
	}

	public String getKey() {
		return key;
	}

	public String getData() {
		return data;
	}

	static class Parser extends Command.Parser {
		public Parser() {
			super(NAME);
		}

		@Override
		protected Command parse(Tokens argTokens) {
			String key = argTokens.parse(Tokens.ANY);
			String data = argTokens.slurp();

			return new PutCommand(key, data);
		}
	}
}
