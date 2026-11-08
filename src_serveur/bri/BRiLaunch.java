package bri;

import demo.ReverseService;
import demo.UppercaseService;
import demo.WordCountService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Two listeners, concurrent sessions, and explicit shutdown for tests and Ctrl+C. */
public final class BRiLaunch implements AutoCloseable {
    private final ServerSocket amateur;
    private final ServerSocket programmer;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final Set<Socket> sessions = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public BRiLaunch(int amateurPort, int programmerPort) throws IOException {
        amateur = new ServerSocket(amateurPort);
        try { programmer = new ServerSocket(programmerPort); }
        catch (IOException e) { amateur.close(); throw e; }
        workers.execute(() -> accept(amateur, false));
        workers.execute(() -> accept(programmer, true));
    }

    public int getAmateurPort() { return amateur.getLocalPort(); }
    public int getProgrammerPort() { return programmer.getLocalPort(); }

    private void accept(ServerSocket listener, boolean isProgrammer) {
        while (!closed) {
            try {
                Socket socket = listener.accept();
                sessions.add(socket);
                if (closed) { socket.close(); sessions.remove(socket); break; }
                workers.execute(() -> {
                    try {
                        if (isProgrammer) new ServiceProg(socket).run();
                        else new ServiceAmateur(socket).run();
                    } finally { sessions.remove(socket); }
                });
            } catch (IOException | java.util.concurrent.RejectedExecutionException e) {
                if (!closed) System.err.println("Erreur listener : " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        try { amateur.close(); } catch (IOException ignored) { }
        try { programmer.close(); } catch (IOException ignored) { }
        for (Socket socket : sessions) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        workers.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) {
            throw new IllegalArgumentException("Usage : java bri.BRiLaunch [portAmateur portProgrammeur]");
        }
        int amateurPort = args.length == 0 ? 3000 : Integer.parseInt(args[0]);
        int programmerPort = args.length == 0 ? 4000 : Integer.parseInt(args[1]);
        if (amateurPort < 1 || amateurPort > 65535 || programmerPort < 1 || programmerPort > 65535) {
            throw new IllegalArgumentException("Les ports doivent etre compris entre 1 et 65535.");
        }
        ServiceRegistry.install(ReverseService.class, "demo", false);
        ServiceRegistry.install(UppercaseService.class, "demo", false);
        ServiceRegistry.install(WordCountService.class, "demo", false);
        BRiLaunch server = new BRiLaunch(amateurPort, programmerPort);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("Serveur Amateur : localhost:" + server.getAmateurPort());
        System.out.println("Serveur Programmeur : localhost:" + server.getProgrammerPort());
        System.out.println("Arret : Ctrl+C");
    }
}
