package demo;

import bri.Service;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Exemple appartenant au programmeur dont le login est "demo". */
public class ReverseService implements Service {
    private final Socket socket;

    public ReverseService(Socket socket) {
        this.socket = socket;
    }

    public static String toStringue() {
        return "Inverser un texte";
    }

    @Override
    public void run() {
        try (Socket connection = socket) {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));
            output.write("Entrez le texte a inverser :\n");
            output.flush();
            String text = input.readLine();
            if (text != null) {
                output.write(new StringBuilder(text).reverse().toString());
                output.newLine();
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Service inversion : " + e.getMessage());
        }
    }
}
