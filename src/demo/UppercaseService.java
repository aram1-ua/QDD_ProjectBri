package demo;

import bri.Service;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UppercaseService implements Service {
    private final Socket socket;

    public UppercaseService(Socket socket) {
        this.socket = socket;
    }

    public static String toStringue() {
        return "Convertir un texte en majuscules";
    }

    @Override
    public void run() {
        try (Socket connection = socket) {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));
            output.write("Entrez le texte a convertir :\n");
            output.flush();
            String text = input.readLine();
            if (text != null) {
                output.write(text.toUpperCase(Locale.ROOT));
                output.newLine();
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Service majuscules : " + e.getMessage());
        }
    }
}
