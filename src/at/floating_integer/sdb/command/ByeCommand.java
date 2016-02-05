package at.floating_integer.sdb.command;

public class ByeCommand extends Command {
	private static final String NAME = "bye";

	public ByeCommand() {
		super(NAME);
	}

	static class Parser extends Command.Parser {
		public Parser() {
			super(NAME);
		}

		@Override
		protected Command parse(Tokens argTokens) {
			return new ByeCommand();
		}
	}
}
