package at.floating_integer.sdb.command;

public class Command {
	private final String name;

	protected Command(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static Command parse(String cmdLine) {
		if (cmdLine.startsWith("ima ")) {
			return ImaCommand.parse(cmdLine);
		}
		return null;

	}
}
