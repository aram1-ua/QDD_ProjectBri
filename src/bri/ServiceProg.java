package bri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServiceProg implements Runnable {
    private final Socket socket;
    private static final ProgrammeurRegistry registry = new ProgrammeurRegistry();

    public ServiceProg(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket connection = socket) {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));

            output.write("1. Inscription\n2. Déposer service\n3. Modifier FTP\nChoix: ");
            output.flush();

            String choix = input.readLine();
            if (choix == null) return;

            switch (choix.trim()) {
                case "1" -> {
                    String login = input.readLine();
                    String mdp = input.readLine();
                    String ftp = input.readLine();
                    boolean ok = registry.register(login, mdp, ftp);
                    output.write(ok ? "OK\n" : "ERROR\n");
                }
                case "2" -> {
                    String login = input.readLine();
                    String mdp = input.readLine();
                    if (!registry.authenticate(login, mdp)) {
                        output.write("ERROR Auth\n");
                    } else {
                        String className = input.readLine();
                        Class<?> c = Class.forName(className);
                        ServiceRegistry.addService(c);
                        output.write("OK Service ajoute\n");
                    }
                }
                case "3" -> {
                    String login = input.readLine();
                    String mdp = input.readLine();
                    String newFtp = input.readLine();
                    boolean ok = registry.updateFtpUrl(login, mdp, newFtp);
                    output.write(ok ? "OK\n" : "ERROR\n");
                }
                default -> output.write("ERROR Choix invalide\n");
            }
            output.flush();

        } catch (Exception e) {
            System.err.println("Erreur ServiceProg : " + e.getMessage());
        }
    }
}