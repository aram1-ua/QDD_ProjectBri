package support;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Loopback-only FTP fixture, including passive binary transfers. Not a production FTP server. */
public final class MiniFtpServer implements AutoCloseable {
    public final Map<String, byte[]> files = new ConcurrentHashMap<>();
    public final AtomicInteger transfers = new AtomicInteger();
    private final ServerSocket control = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final Set<Socket> sessions = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public MiniFtpServer() throws IOException {
        workers.execute(() -> {
            while (!closed) {
                try {
                    Socket socket = control.accept();
                    sessions.add(socket);
                    workers.execute(() -> serve(socket));
                } catch (IOException e) {
                    if (!closed) throw new UncheckedIOException(e);
                }
            }
        });
    }

    public String url() { return "ftp://127.0.0.1:" + control.getLocalPort() + "/"; }

    private void serve(Socket socket) {
        ServerSocket data = null;
        try (socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true)) {
            socket.setSoTimeout(10000);
            send(output, "220 BRi test FTP");
            String cwd = "";
            String line;
            while ((line = input.readLine()) != null) {
                String[] parts = line.split(" ", 2);
                String argument = parts.length == 2 ? parts[1] : "";
                switch (parts[0].toUpperCase(java.util.Locale.ROOT)) {
                    case "USER" -> send(output, "331 Password required");
                    case "PASS" -> send(output, "230 Logged in");
                    case "TYPE", "NOOP" -> send(output, "200 OK");
                    case "SYST" -> send(output, "215 UNIX Type: L8");
                    case "PWD" -> send(output, "257 \"/" + cwd + "\"");
                    case "CWD" -> {
                        cwd = argument.startsWith("/") ? argument.substring(1) : cwd + argument;
                        if (!cwd.isEmpty() && !cwd.endsWith("/")) cwd += "/";
                        send(output, "250 Directory changed");
                    }
                    case "EPSV", "PASV" -> {
                        if (argument.equals("ALL")) { send(output, "200 EPSV ALL"); break; }
                        if (data != null) data.close();
                        data = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                        data.setSoTimeout(5000);
                        int port = data.getLocalPort();
                        send(output, parts[0].equalsIgnoreCase("EPSV")
                                ? "229 Entering Extended Passive Mode (|||" + port + "|)"
                                : "227 Entering Passive Mode (127,0,0,1," + port / 256 + "," + port % 256 + ")");
                    }
                    case "RETR" -> {
                        String path = argument.startsWith("/") ? argument.substring(1) : cwd + argument;
                        byte[] bytes = files.get(path);
                        if (bytes == null || data == null) { send(output, "550 File not found"); break; }
                        send(output, "150 Opening binary data connection (" + bytes.length + " bytes)");
                        try (Socket transfer = data.accept()) { transfer.getOutputStream().write(bytes); }
                        data.close();
                        data = null;
                        transfers.incrementAndGet();
                        send(output, "226 Transfer complete");
                    }
                    case "QUIT" -> { send(output, "221 Goodbye"); return; }
                    default -> send(output, "502 Unsupported command");
                }
            }
        } catch (IOException e) {
            if (!closed) throw new UncheckedIOException(e);
        } finally {
            sessions.remove(socket);
            if (data != null) try { data.close(); } catch (IOException ignored) { }
        }
    }

    private static void send(PrintWriter output, String text) { output.print(text + "\r\n"); output.flush(); }

    @Override
    public void close() {
        closed = true;
        try { control.close(); } catch (IOException ignored) { }
        for (Socket socket : sessions) try { socket.close(); } catch (IOException ignored) { }
        workers.shutdownNow();
    }
}
