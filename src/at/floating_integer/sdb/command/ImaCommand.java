package at.floating_integer.sdb.command;

public class ImaCommand extends Command {

	private static final String NAME = "ima";
	private final String userName;

	public ImaCommand(String userName) {
		super(NAME);
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	static class Parser extends Command.Parser {

		public Parser() {
			super(NAME);
		}

		@Override
		protected ImaCommand parse(Tokens argTokens) {
			String userName = argTokens.parse(Tokens.ANY);

			return new ImaCommand(userName);
		}
	}
}
