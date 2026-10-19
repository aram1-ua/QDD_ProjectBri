package demo;

import bri.Service;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class WordCountService implements Service {
    private final Socket socket;

    public WordCountService(Socket socket) {
        this.socket = socket;
    }

    public static String toStringue() {
        return "Compter les mots d'un texte";
    }

    @Override
    public void run() {
        try (Socket connection = socket) {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));
            output.write("Entrez le texte dont il faut compter les mots :\n");
            output.flush();
            String text = input.readLine();
            if (text != null) {
                // Un mot est une suite de caracteres separes par des espaces Unicode.
                long count = java.util.regex.Pattern.compile("(?U)\\S+").matcher(text).results().count();
                output.write(Long.toString(count));
                output.newLine();
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Service comptage : " + e.getMessage());
        }
    }
}
