
## sdb

A simple database with an even simpler tcp protocol

### Disclaimer

This software is _not_ intended to be used in production.

Instead it is meant as a sample server, in order to learn, how tcp communication works.

It is designed to have a simple protocol. Thus writing clients is straight forward.

### Protocol

Try it out without programming anything!

Use `netcat`/`ncat`/`telnet` to connect to the database:

```
$ telnet $HOST $PORT
```

e.g.

```
$ telnet localhost 9999
```

We denote messages received from the server with a prefixed `<`, and messages sent to the server with a prefixed `>`.

e.g.

```
< who
> iam fridolin
```

which means: the server sent a message `"who\n"`, and the client replied `"iam fridolin\n"`.

#### Login

Upon connection, the first message received from the server is `who`.
The client needs to reply with `iam <username>`, where `<username>` is the desired username.
The server then confirms the login:

```
< who
> iam fridolin
< has login system 2016-Feb-08 15:12:40.601 fridolin
```

#### Storing data

Data can be stored under a given key. The client needs to send `put <key> <data>`; the server will respond with a confirmation:

```
> put my.chosen.key This data is very precious!
< has my.chosen.key fridolin 2016-Feb-08 15:32:26.053 This data is very precious!
```

The returned record contains (in this order), the key, the author, the date of insertion, and the data itself.

#### Retrieving data

Data can be retrieved via a given key. The client needs to send `get <key>`; the server will respond with the data for the key; or with `nil` if there is no data associated with the given key:

```
> get my.chosen.key
< has my.chosen.key fridolin 2016-Feb-08 15:32:26.053 This data is very precious!
```

The record format is the same as above.

In case there is no data associated with the given key, the server reponds with `nil`:

```
> get non_existing_key
< nil
```

#### Data subscription

The client has the possibility to subscribe to a key. The client will then receive,

* The current data associated with the key (maybe `nil`!), then
* _Each time the data is updated_, another record (in real time)
* If the subscription is cancelled, then a final `nil`

```
> sub mykey
< has mykey fridolin 2016-Feb-08 15:40:12.097 current data
< has mykey other_user 2016-Feb-08 15:40:42.551 1st update
< has mykey other_user 2016-Feb-08 15:40:59.396 another update
< has mykey other_user 2016-Feb-08 15:41:05.376 still another update
< ...
> put stop_subscription_by_issueing_another_cmd 1
< nil
> ...
```

To cancel a subscription, just issue another command to the server.

#### Disconnecting

To end a client's session, the client sends `bye`. The server will respond with `bye` and then closes the connection.

```
> bye
< bye
Connection closed
```

### License

Copyright (C) 2016 Florian Hassanen

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
