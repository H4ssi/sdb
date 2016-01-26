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

	static class ImaCommandParser extends CommandParser {

		public ImaCommandParser() {
			super(NAME);
		}

		@Override
		protected ImaCommand parse(Tokens argTokens) {
			return new ImaCommand(argTokens.parse(new TokenParser<String>() {
				@Override
				public String parseToken(String token) {
					return token;
				}
			}));
		}
	}
}
