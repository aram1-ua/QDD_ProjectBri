package bri;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.lang.model.SourceVersion;

/** UTF-8, one action per connection. A blank line terminates the menu. */
public class ServiceProg implements Runnable {
    private final Socket socket;
    private static final ProgrammeurRegistry registry = new ProgrammeurRegistry();

    public ServiceProg(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (Socket connection = socket) {
            connection.setSoTimeout(30000);
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
            output.write("1. Inscription\n2. Deposer service\n3. Modifier FTP\n4. Mettre a jour un service\n\n");
            output.flush();
            try {
                String choice = input.readLine();
                if (choice == null) return;
                if (choice.equals("1")) {
                    boolean ok = registry.register(required(input), required(input), required(input));
                    reply(output, ok ? "OK Inscription reussie" : "ERROR Login deja utilise ou donnees invalides (login Java, mot de passe, URL ftp://).");
                } else if (choice.equals("2") || choice.equals("3") || choice.equals("4")) {
                    String login = required(input);
                    String password = required(input);
                    if (!registry.authenticate(login, password)) {
                        reply(output, "ERROR Auth invalide");
                        return;
                    }
                    String value = required(input);
                    if (choice.equals("3")) {
                        reply(output, registry.updateFtpUrl(login, password, value)
                                ? "OK URL FTP mise a jour" : "ERROR URL FTP invalide");
                    } else {
                        int separator = value.lastIndexOf('.');
                        if (!SourceVersion.isName(value) || separator < 0 || !value.substring(0, separator).equals(login)) {
                            reply(output, "ERROR Le package de la classe doit correspondre au login: " + login);
                            return;
                        }
                        FtpServiceLoader loader = new FtpServiceLoader(registry.getAccount(login).getFtpUrl(), login);
                        Class<?> service = loader.loadClass(value);
                        ServiceRegistry.install(service, login, choice.equals("4"));
                        reply(output, choice.equals("4") ? "OK Service mis a jour depuis FTP" : "OK Service ajoute depuis FTP");
                    }
                } else {
                    reply(output, "ERROR Choix invalide");
                }
            } catch (Exception | LinkageError e) {
                reply(output, "ERROR " + e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).replace('\n', ' ').replace('\r', ' '));
            }
        } catch (IOException e) {
            System.err.println("Session programmeur terminee : " + e.getMessage());
        }
    }

    private static String required(BufferedReader input) throws IOException {
        String value = input.readLine();
        if (value == null) throw new EOFException("Requete incomplete.");
        return value;
    }

    private static void reply(BufferedWriter output, String message) throws IOException {
        output.write(message);
        output.newLine();
        output.flush();
    }
}
