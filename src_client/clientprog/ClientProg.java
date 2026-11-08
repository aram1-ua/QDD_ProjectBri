package clientprog;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Interactive programmer client: registration, installation, FTP changes and updates. */
public final class ClientProg {
    private ClientProg() { }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage : java clientprog.ClientProg <hote> <port>");
            return;
        }
        try {
            int port = Integer.parseInt(args[1]);
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Port invalide.");
            run(args[0], port, new InputStreamReader(System.in, StandardCharsets.UTF_8),
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Connexion ou utilisation impossible : " + e.getMessage());
        }
    }

    public static void run(String host, int port, Reader consoleInput, Writer consoleOutput) throws IOException {
        BufferedReader console = new BufferedReader(consoleInput);
        PrintWriter display = new PrintWriter(consoleOutput, true);
        // Human input has no network deadline. Connect only when the request is complete.
        display.println("1. Inscription");
        display.println("2. Deposer service");
        display.println("3. Modifier FTP");
        display.println("4. Mettre a jour un service");
        String choice;
        while (true) {
            display.println("Choix (1-4 ou /quit) :");
            choice = console.readLine();
            if (choice == null || choice.trim().equalsIgnoreCase("/quit")) return;
            choice = choice.trim();
            if (choice.matches("[1-4]")) break;
            display.println("Choix invalide.");
        }
        String[] prompts = {"Login :", "Mot de passe :", choice.equals("1") || choice.equals("3")
                ? "URL du repertoire FTP (ftp://hote/repertoire/) :" : "Classe du service (login.NomClasse) :"};
        String[] values = new String[3];
        for (int i = 0; i < prompts.length; i++) {
            display.println(prompts[i]);
            values[i] = console.readLine();
            if (values[i] == null) return;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(30000);
            BufferedReader server = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter request = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = server.readLine()) != null && !line.isEmpty()) { /* Consume server menu. */ }
            if (line == null) throw new EOFException("Menu incomplet.");
            request.write(choice);
            request.newLine();
            for (String value : values) { request.write(value); request.newLine(); }
            request.flush();
            String response = server.readLine();
            if (response == null) throw new EOFException("Aucune reponse du serveur.");
            display.println(response);
        }
    }
}
