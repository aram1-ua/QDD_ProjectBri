package bri;

import clientama.ClientAma;
import clientprog.ClientProg;
import demo.ReverseService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.tools.ToolProvider;
import support.MiniFtpServer;

/** End-to-end tests through the real handlers, clients, FTP and fresh class versions. */
public final class PlatformFlowTest {
    private static int checks;
    private static Path compilation;

    public static void main(String[] args) throws Exception {
        compilation = Files.createTempDirectory(Path.of("build"), "ftp-test-");
        try (MiniFtpServer ftp = new MiniFtpServer(); BRiLaunch server = new BRiLaunch(0, 0)) {
            int prog = server.getProgrammerPort();
            int ama = server.getAmateurPort();
            expect("register", programmer(prog, "1\nremote\nsecret\n" + ftp.url() + "\n"), "OK Inscription");
            expect("duplicate account", programmer(prog, "1\nremote\nsecret\n" + ftp.url() + "\n"), "ERROR");
            expect("bad login", programmer(prog, "1\nnot.a.package\nsecret\n" + ftp.url() + "\n"), "ERROR");
            expect("keyword login", programmer(prog, "1\nclass\nsecret\n" + ftp.url() + "\n"), "ERROR");
            expect("bad ftp", programmer(prog, "1\ninvalidurl\nsecret\nhttp://localhost/\n"), "ERROR");
            expect("bad auth", programmer(prog, "2\nremote\nwrong\nremote.EchoService\n"), "ERROR Auth");
            expect("wrong package", programmer(prog, "2\nremote\nsecret\nother.EchoService\n"), "ERROR Le package");
            expect("nested package", programmer(prog, "2\nremote\nsecret\nremote.other.EchoService\n"), "ERROR Le package");
            expect("missing class", action(prog, "2", "remote.Absent"), "ERROR");
            ftp.files.put("remote/EchoService.class", service("v1"));
            expect("update absent", action(prog, "4", "remote.EchoService"), "ERROR");
            expect("install", action(prog, "2", "remote.EchoService"), "OK Service ajoute");
            check("FTP binary transfer actually happened", ftp.transfers.get() >= 2);
            check("class loaded remotely", ServiceRegistry.getServiceClass(1).getClassLoader() instanceof FtpServiceLoader);
            expect("v1 execution", amateur(ama, "1\nhello\n"), "v1:hello");
            expect("unicode", amateur(ama, "1\nété 😀\n"), "v1:été 😀");
            expect("duplicate install", action(prog, "2", "remote.EchoService"), "ERROR");

            // Keep an old session waiting while a new version is installed.
            try (Socket old = new Socket("127.0.0.1", ama)) {
                old.setSoTimeout(10000);
                BufferedReader input = new BufferedReader(new InputStreamReader(old.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter output = new PrintWriter(new OutputStreamWriter(old.getOutputStream(), StandardCharsets.UTF_8), true);
                menu(input);
                output.println("1");
                check("old session prompt", "Text:".equals(input.readLine()));
                ftp.files.put("remote/EchoService.class", service("v2"));
                expect("live update", action(prog, "4", "remote.EchoService"), "OK Service mis a jour");
                expect("v2 execution", amateur(ama, "1\nhello\n"), "v2:hello");
                output.println("old");
                check("running v1 survives update", "v1:old".equals(input.readLine()));
            }
            check("update preserves service number", ServiceRegistry.getServiceClass(2) == null);
            Class<?> validVersion = ServiceRegistry.getServiceClass(1);
            for (String body : List.of(
                    "public class EchoService {}",
                    "public abstract class EchoService implements bri.Service { public EchoService(java.net.Socket s) {} public static String toStringue(){return \"bad\";} }",
                    "public class EchoService implements bri.Service { private EchoService(java.net.Socket s) {} public static String toStringue(){return \"bad\";} public void run(){} }",
                    "public class EchoService implements bri.Service { public EchoService(java.net.Socket s) {} public String toStringue(){return \"bad\";} public void run(){} }",
                    "public class EchoService implements bri.Service { public EchoService(java.net.Socket s) {} public static int toStringue(){return 1;} public void run(){} }",
                    "public class EchoService implements bri.Service { public EchoService(java.net.Socket s) {} public static String toStringue(){throw new IllegalStateException();} public void run(){} }")) {
                ftp.files.put("remote/EchoService.class", compile("package remote; " + body));
                expect("reject invalid BRi version", action(prog, "4", "remote.EchoService"), "ERROR");
                check("failed update retains class", ServiceRegistry.getServiceClass(1) == validVersion);
            }
            ftp.files.put("remote/EchoService.class", new byte[]{0, 1, 2});
            expect("reject corrupt bytecode", action(prog, "4", "remote.EchoService"), "ERROR");
            expect("previous version still works", amateur(ama, "1\nok\n"), "v2:ok");
            expect("bad ftp update", programmer(prog, "3\nremote\nsecret\nfile:///tmp/\n"), "ERROR");
            expect("ftp change", programmer(prog, "3\nremote\nsecret\n" + ftp.url() + "new/\n"), "OK URL");
            ftp.files.put("new/remote/EchoService.class", service("v3"));
            expect("new ftp directory", action(prog, "4", "remote.EchoService"), "OK Service mis a jour");
            expect("v3 execution", amateur(ama, "1\nnew\n"), "v3:new");

            // A class present on the application classpath must still come from FTP.
            ServiceRegistry.addService(ReverseService.class);
            expect("demo register", programmer(prog, "1\ndemo\nsecret\n" + ftp.url() + "\n"), "OK");
            expect("no classpath fallback", programmer(prog, "2\ndemo\nsecret\ndemo.ReverseService\n"), "ERROR");
            ftp.files.put("demo/ReverseService.class", Files.readAllBytes(Path.of("build/classes/demo/ReverseService.class")));
            expect("protect another owner", programmer(prog, "4\ndemo\nsecret\ndemo.ReverseService\n"), "ERROR");
            expect("unknown service", amateur(ama, "99\n"), "ERROR Service inconnu");
            check("quit programmer", !programmer(prog, "/quit\n").contains("Login :"));
            check("EOF programmer", !programmer(prog, "").contains("Login :"));
            expect("client invalid input retry", programmer(prog, "abc\n0\n3\nremote\nsecret\n" + ftp.url() + "\n"), "OK URL");
            malformed(prog);
            ExecutorService clients = Executors.newFixedThreadPool(3);
            try {
                List<Future<String>> results = new ArrayList<>();
                for (int i = 0; i < 6; i++) results.add(clients.submit(() -> amateur(ama, "1\nparallel\n")));
                for (Future<String> result : results) expect("parallel clients", result.get(10, TimeUnit.SECONDS), "v3:parallel");
            } finally { clients.shutdownNow(); }
            int deadPort;
            try (ServerSocket unused = new ServerSocket(0)) { deadPort = unused.getLocalPort(); }
            expect("unreachable ftp address accepted", programmer(prog, "3\nremote\nsecret\nftp://127.0.0.1:" + deadPort + "/\n"), "OK URL");
            expect("unreachable ftp rejected at load", action(prog, "4", "remote.EchoService"), "ERROR");
            expect("failed network update retains service", amateur(ama, "1\nstill here\n"), "v3:still here");
        }
        System.out.println("OK : " + checks + " platform verifications (real FTP, sockets, install and live update).");
    }

    private static byte[] service(String version) throws IOException {
        return compile("""
                package remote;
                import java.io.*;
                import java.net.Socket;
                import java.nio.charset.StandardCharsets;
                public class EchoService implements bri.Service {
                    private final Socket socket;
                    public EchoService(Socket socket) { this.socket = socket; }
                    public static String toStringue() { return "Echo %s"; }
                    public void run() {
                        try {
                            var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                            var output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                            output.println("Text:");
                            String text = input.readLine();
                            if (text != null) output.println("%s:" + text);
                        } catch (IOException e) { throw new UncheckedIOException(e); }
                    }
                }
                """.formatted(version, version));
    }

    private static byte[] compile(String source) throws IOException {
        Path file = compilation.resolve("remote/EchoService.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "17", "-encoding", "UTF-8", "-cp", System.getProperty("java.class.path"),
                "-d", compilation.toString(), file.toString());
        if (result != 0) throw new AssertionError("Fixture compilation failed.");
        return Files.readAllBytes(compilation.resolve("remote/EchoService.class"));
    }

    private static String action(int port, String action, String type) throws IOException {
        return programmer(port, action + "\nremote\nsecret\n" + type + "\n");
    }

    private static String programmer(int port, String input) throws IOException {
        StringWriter output = new StringWriter();
        ClientProg.run("127.0.0.1", port, new StringReader(input), output);
        return output.toString();
    }

    private static String amateur(int port, String input) throws IOException {
        StringWriter output = new StringWriter();
        ClientAma.run("127.0.0.1", port, new StringReader(input), output);
        return output.toString();
    }

    private static void malformed(int port) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            menu(reader);
            socket.getOutputStream().write("2\nremote\n".getBytes(StandardCharsets.UTF_8));
            socket.shutdownOutput();
            expect("truncated request", reader.readLine(), "ERROR");
        }
    }

    private static void menu(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) { }
        if (line == null) throw new EOFException("Incomplete menu");
    }

    private static void expect(String label, String actual, String expected) {
        check(label + ": " + actual, actual != null && actual.contains(expected));
    }

    private static void check(String label, boolean condition) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
