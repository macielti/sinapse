# Sinapse

Sinapse is an Integrant component to deal with async messaging, when the messages are intent to be consumed internally
by the same service that produced the messages.

Initially, Sinapse does not persist the messages, so if the service crashes, the messages will be lost. In the future,
the message persistence will be implemented.

## License

Copyright © 2025 Bruno do Nascimento Maciel

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
