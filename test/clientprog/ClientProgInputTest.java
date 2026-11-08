package clientprog;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Regression: console input must finish before any timed network session starts. */
public final class ClientProgInputTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch inputRequested = new CountDownLatch(1);
        CountDownLatch allowInput = new CountDownLatch(1);
        AtomicBoolean connected = new AtomicBoolean();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try (ServerSocket listener = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            listener.setSoTimeout(5000);
            Future<?> server = workers.submit(() -> {
                try (Socket socket = listener.accept()) {
                    connected.set(true);
                    socket.setSoTimeout(2000);
                    BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter reply = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    reply.print("1. Inscription\n\n");
                    reply.flush();
                    for (String expected : new String[]{"1", "slowuser", "secret", "ftp://localhost/services/"}) {
                        if (!expected.equals(request.readLine())) throw new AssertionError("Request contents");
                    }
                    reply.println("OK Inscription reussie");
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            Reader console = new StringReader("1\nslowuser\nsecret\nftp://localhost/services/\n") {
                @Override
                public int read(char[] buffer, int offset, int length) throws IOException {
                    inputRequested.countDown();
                    try {
                        if (!allowInput.await(5, TimeUnit.SECONDS)) throw new IOException("Input gate timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                    return super.read(buffer, offset, length);
                }
            };
            StringWriter display = new StringWriter();
            Future<?> client = workers.submit(() -> {
                try { ClientProg.run("localhost", listener.getLocalPort(), console, display); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            });
            if (!inputRequested.await(3, TimeUnit.SECONDS)) throw new AssertionError("Console was not requested");
            if (connected.get()) throw new AssertionError("Connected before user input was ready");
            allowInput.countDown();
            client.get(5, TimeUnit.SECONDS);
            server.get(5, TimeUnit.SECONDS);
            if (!display.toString().contains("OK Inscription reussie")) throw new AssertionError("Registration did not finish");
            // Quit and incomplete input must also work without a listening server.
            int closedPort;
            try (ServerSocket unused = new ServerSocket(0)) { closedPort = unused.getLocalPort(); }
            ClientProg.run("localhost", closedPort, new StringReader("/quit\n"), new StringWriter());
            ClientProg.run("localhost", closedPort, new StringReader("1\nslowuser\n"), new StringWriter());
            System.out.println("OK : programmer input timing, registration, quit and incomplete input.");
        } finally {
            allowInput.countDown();
            workers.shutdownNow();
        }
    }
}
