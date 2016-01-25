package at.floating_integer.sdb.command;

public class ImaCommand extends Command {

	private final String userName;

	public ImaCommand(String userName) {
		super("ima");
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public static ImaCommand parse(String cmdLine) {
		if (!cmdLine.startsWith("ima ")) {
			return null;
		}

		String name = cmdLine.substring(4).trim();

		if ("".equals(name)) {
			return null;
		}

		return new ImaCommand(name);
	}
}
