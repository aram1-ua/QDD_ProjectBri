package clientama;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Client console : une connexion permet d'utiliser un service une fois. */
public class ClientAma {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage : java clientama.ClientAma <hote> <port>");
            return;
        }
        try {
            int port = Integer.parseInt(args[1]);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Le port doit etre entre 1 et 65535.");
            }
            run(args[0], port, new InputStreamReader(System.in, StandardCharsets.UTF_8),
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Connexion ou utilisation impossible : " + e.getMessage());
        }
    }

    /** Les flux de la console restent ouverts pour leur proprietaire. */
    public static void run(String host, int port, Reader input, Writer output) throws IOException {
        BufferedReader console = new BufferedReader(input);
        PrintWriter display = new PrintWriter(output, true);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(30000);
            BufferedReader server = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter request = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = server.readLine()) != null && !line.isEmpty()) {
                display.println(line);
            }
            if (line == null) {
                throw new EOFException("Le serveur a ferme la connexion avant la fin du menu.");
            }

            while (true) {
                display.println("Numero du service (ou /quit) :");
                String choice = console.readLine();
                if (choice == null || choice.trim().equalsIgnoreCase("/quit")) {
                    return;
                }
                try {
                    if (Integer.parseInt(choice.trim()) > 0) {
                        sendLine(request, choice.trim());
                        break;
                    }
                } catch (NumberFormatException ignored) {
                    // Une nouvelle saisie sera demandee ci-dessous.
                }
                display.println("Entrez un entier positif.");
            }

            String prompt = server.readLine();
            if (prompt == null) {
                throw new EOFException("Le serveur n'a pas envoye de consigne.");
            }
            display.println(prompt);
            if (prompt.startsWith("ERROR ")) {
                return;
            }
            String text = console.readLine();
            if (text == null) {
                return;
            }
            sendLine(request, text);
            String result = server.readLine();
            if (result == null) {
                throw new EOFException("Le service n'a pas renvoye de resultat.");
            }
            display.println(result);
        }
    }

    private static void sendLine(BufferedWriter output, String text) throws IOException {
        output.write(text);
        output.newLine();
        output.flush();
    }
}
