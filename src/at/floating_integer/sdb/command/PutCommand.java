/*******************************************************************************
 * sdb - a simple database with an even simpler tcp protocol
 * Copyright (C) 2016 Florian Hassanen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
