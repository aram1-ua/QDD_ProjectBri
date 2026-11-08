# BRi - Dynamic Java service platform

Java 17 project for R5A08 Qualite de developpement. BRiLaunch serves amateur
users on port **3000** and programmers on port **4000**, using sockets and
concurrent session handlers. Both console clients are implemented.

## Build, test and run

### Eclipse (primary course workflow)

1. In Eclipse, choose **File > Import > General > Existing Projects into
   Workspace**, select this extracted project folder and click **Finish**.
   Do not create a new project or a module descriptor.
2. Under **Window > Preferences > Java > Installed JREs**, add/select a full
   **JDK 17 or later**. Under **Java > Installed JREs > Execution Environments**,
   associate **JavaSE-17** with that JDK. The tests require a JDK, not just a JRE.
3. Choose **Project > Clean**, select this project, then build it. The supplied
   metadata configures UTF-8, Java 17 and the three source folders.
4. In Package Explorer, expand `launch`. Right-click **BRi Server.launch** >
   **Run As > BRi Server** (or select it under **Run > Run Configurations >
   Java Application**). Leave the server running.
5. Run **BRi Amateur Client.launch** the same way. It already has arguments
   `localhost 3000`. Select its console, choose `1`, enter `bonjour`, and check
   that the result is `ruojnob`.
6. Run **BRi Programmer Client.launch** for registration, FTP installation,
   address changes and updates. It already has arguments `localhost 4000`.

You can also right-click each main Java class and choose **Run As > Java
Application**; set the two clients' arguments in their run configurations.
Use the Console view's console selector to switch between server and clients.
Each client ends after one action; run it again for the next action. Stop the
server with the red **Terminate** button. Start only one server on these ports;
if a terminal server is already running, stop it before launching Eclipse's.

To verify in Eclipse, run `bri.ProgrammerAccountTest`,
`clientama.AmateurFlowTest` and `bri.PlatformFlowTest` separately as **Java
Application**. Set their working directory to this project root (the normal
project default). Expected counts: 38, 16 and 56. They use their own ports and
do not require your FTP server. The test fixture compiles its own remote class
with the selected JDK.

### Optional terminal workflow

Install a **JDK 17 or later** with `java` and `javac` on PATH. No external Java
libraries, Maven, FTP server or internet connection are required for the tests.
The assignment specifies a Java server and two clients, but does not prescribe
a particular IDE, shell or launch script. PowerShell scripts are optional.
The Windows commands below work in both PowerShell and Command Prompt without
changing PowerShell execution policy:

```powershell
.\build.cmd
.\test.cmd
java -cp build/classes bri.BRiLaunch
```

Leave the server running. In separate terminals:

```powershell
java -cp build/classes clientama.ClientAma localhost 3000
java -cp build/classes clientprog.ClientProg localhost 4000
```

`Main` also starts BRiLaunch. To use different ports:

```powershell
java -cp build/classes bri.BRiLaunch 3001 4001
```

Stop with Ctrl+C. Startup fails visibly if either port is unavailable; it does
not leave half a server running. Each client performs one action per connection;
run it again for another action. `/quit` exits at the menu. Use a UTF-8 terminal
for accented text. On JDK 17, `java '-Dfile.encoding=UTF-8' -cp ...` explicitly
sets Java's default encoding if necessary.

For Eclipse, use `src_client`, `src_serveur` and `test` as source folders, UTF-8
encoding and Java 17. The ZIP includes `.project` and `.classpath`: choose
File > Import > General > Existing Projects into Workspace and select the
extracted project folder. Run `bri.BRiLaunch` as a Java Application, then
`clientama.ClientAma` with arguments `localhost 3000` and
`clientprog.ClientProg` with arguments `localhost 4000`. For IntelliJ, open this project and select your installed
JDK 17+. `src/module-info.java` is an unused legacy file: this is a classpath
project, and `src` must not be marked as a source root. Generated files are in
`build/`; the previous `out/` directory is not used by the scripts.

For a shell-independent build using only the JDK, from the project root:

```text
javac --release 17 -Xlint:all -encoding UTF-8 -d build/classes "@sources.txt"
java -cp build/classes bri.ProgrammerAccountTest
java -cp build/classes clientama.AmateurFlowTest
java -cp build/classes bri.PlatformFlowTest
```

`sources.txt` lists all Java sources; update it if you add or remove source files.
The `.ps1` helpers remain available, but Windows can block them under its local
execution policy. This does not prevent the `.cmd`, direct JDK or IDE routes.

The programmer client collects all console input before connecting, so time
spent entering credentials or reading instructions does not consume the server's
30-second session timeout. `clientprog.ClientProgInputTest` verifies this behavior
and cancellation/incomplete input without a server; run it as a Java Application
in Eclipse or through either test helper.

## Amateur services

At startup: 1 = reverse text, 2 = uppercase, 3 = word count. Choose a number,
then enter one line. Empty text and Unicode are supported. New services appear
in the menu after installation, without restarting BRiLaunch.

## Programmer workflow and a real FTP deployment

1. Run ClientProg and choose **1**. Enter a login (a single Java package name,
   for example `alice`), a password, and an FTP directory URL. Account
   certification is automatic, as requested. `java`, `javax` and `bri` are
   reserved. To update the bundled demo services, first register login `demo`.
2. Write a public concrete class `alice.MyService` implementing `bri.Service`.
   It must have a **public constructor accepting Socket**, implement `run()`,
   and declare **public static String toStringue()** returning a nonempty
   single-line menu description. All programmer classes belong to the login's
   package. Use UTF-8, send a prompt ending in a newline and flush it, read a
   line, send a result line and flush. See `src_client/demo/ReverseService.java`.
3. Compile against the platform interface:

   ```powershell
   javac --release 17 -encoding UTF-8 -cp build/classes -d build/ftp alice/MyService.java
   ```

4. Upload `build/ftp/alice/MyService.class` to your own FTP server, preserving
   the `alice/` package directory. For a registered URL
   `ftp://host/services/`, BRi fetches
   `ftp://host/services/alice/MyService.class`. Authenticated FTP URLs such as
   `ftp://user:password@host/services/` are supported; percent-encode reserved
   characters in credentials. This project connects to an FTP server; it does
   not host one. `support.MiniFtpServer` is an in-memory automated-test fixture.
5. Run ClientProg, choose **2**, authenticate, and enter `alice.MyService`.
   BRi actually retrieves the class from FTP, checks the BRi contract and adds
   it to the amateur menu. Duplicate installation is rejected.
6. Recompile a changed implementation, replace the FTP `.class`, then choose
   **4** with the same credentials and full class name. New requests use the
   new version at the same service number. Already running sessions finish
   using their original version. A failed update leaves the last good version
   installed. Updates are restricted to the service's creator.
7. Choose **3** to change the FTP directory for subsequent installations and
   updates. Existing installed services continue to run.

## Verification

`.\test.cmd` (or `./test.ps1`) builds with `--release 17 -Xlint:all`, then runs:

| Test | Checks | Coverage |
| --- | ---: | --- |
| `bri.ProgrammerAccountTest` | 38 | Validation, authentication, FTP changes, concurrent duplicate registration |
| `clientama.AmateurFlowTest` | 16 | Three demo services, Unicode, empty text, invalid input, quitting and parallel clients |
| `bri.PlatformFlowTest` | 56 | Real BRiLaunch handlers, both clients, binary FTP downloads, installation and live updates |

The platform test compiles a class **outside the application classpath**, serves
it over real loopback FTP, installs v1, runs it, replaces it with v2 while an old
session is active, then loads v3 from a changed FTP directory. It also checks
wrong credentials/packages, duplicate installation, invalid BRi classes, corrupt
bytecode, interrupted requests, ownership, unavailable FTP and preservation of
the previous version. It requires a JDK because it compiles test services.
All tests create their own temporary listeners; a running main server need not
be stopped. The FTP fixture and test services are not used by production code.

## Protocol and design

Both ports use UTF-8 line messages. The server sends a menu and a blank line.
The amateur sends a service number, waits for its prompt, sends text, and reads
one result. The programmer sends an action number followed by login, password
and one value (FTP URL for actions 1/3, full class name for 2/4). The server
returns one `OK ...` or `ERROR ...` line and closes the session. Registration
does not require an existing account; other actions authenticate first.

`ServiceRegistry` validates before atomically adding or replacing an entry.
It retains service order and records ownership. A fresh `FtpServiceLoader` per
version retrieves programmer classes directly from FTP, without falling back
to classes already on the server. The shared `bri.Service` interface is loaded
by the platform loader so type checks retain the same Java identity.
Registry operations are synchronized. FTP connections have 5-second connect/read
timeouts, sessions have a 30-second read timeout, and class files are limited
to 4 MiB. Stopping the server closes listeners and active session sockets.

## Scope and limitations

The minimum assignment features are implemented and covered by local tests.
Accounts and installed services are kept in memory and reset on restart.
Passwords are stored in memory and the exercise's FTP/socket protocols are
unencrypted. Submitted service code executes in the server JVM and is assumed
trusted. Session threads are unbounded; this is a teaching platform, not a
production deployment. Network timeouts bound individual blocked reads, not a
whole transfer's total duration. External FTP-server configurations have not
all been tested.

JAR deployment, shared-resource services, XML/file analysis, email, and explicit
start/stop/uninstall are assignment options and are not implemented. The BRi
checks implement the contract documented here; verify any extra TP5 rules
against your course material, which was not supplied.

## Submission

The French design/results report is `output/pdf/Presentation_BRi.pdf`.
`tools/generate_report.py` regenerates it using Python/reportlab (not needed
to build or run Java). Create a source/report ZIP under 2 MB:

```powershell
./package.ps1 -GroupName DupontDurand
```

Replace the example with the actual group members' names. The ZIP is written
to `output/submission/`; generated classes, IntelliJ metadata and dependencies are
excluded. Eclipse import metadata is included. Extract it, then run `.\test.cmd`
to verify the submission independently, or use the direct JDK commands above.
