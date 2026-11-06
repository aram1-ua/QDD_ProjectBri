package bri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
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
                case "1" -> handleRegister(input, output);
                case "2" -> handleAddService(input, output);
                case "3" -> handleUpdateFtp(input, output);
                default -> output.write("ERROR Choix invalide\n");
            }
            output.flush();

        } catch (Exception e) {
            System.err.println("Erreur ServiceProg : " + e.getMessage());
        }
    }

    private void handleRegister(BufferedReader input, BufferedWriter output) throws Exception {
        String login = input.readLine();
        String mdp = input.readLine();
        String ftp = input.readLine();
        boolean ok = registry.register(login, mdp, ftp);
        output.write(ok ? "OK Inscription reussie\n" : "ERROR Inscription echouee\n");
    }

    private void handleAddService(BufferedReader input, BufferedWriter output) throws Exception {
        String login = input.readLine();
        String mdp = input.readLine();

        // Authentification
        if (!registry.authenticate(login, mdp)) {
            output.write("ERROR Auth invalide\n");
            return;
        }

        ProgrammerAccount account = registry.getAccount(login);
        String className = input.readLine(); // ex: demo.ReverseService

        // Vérification du nom de package
        if (!className.startsWith(login + ".")) {
            output.write("ERROR Le package de la classe doit correspondre au login: " + login + "\n");
            return;
        }

        // Chargement dynamique FTP
        try {
            String ftpUrl = account.getFtpUrl();
            if (!ftpUrl.endsWith("/")) {
                ftpUrl += "/";
            }
            URL[] urls = new URL[]{ new URL(ftpUrl) };

            URLClassLoader classLoader = new URLClassLoader(urls);
            Class<?> serviceClass = classLoader.loadClass(className);

            ServiceRegistry.addService(serviceClass);
            output.write("OK Service ajoute avec succes depuis FTP\n");

        } catch (ClassNotFoundException e) {
            output.write("ERROR Classe introuvable sur le serveur FTP\n");
        } catch (Exception e) {
            output.write("ERROR Erreur lors du chargement : " + e.getMessage() + "\n");
        }
    }

    private void handleUpdateFtp(BufferedReader input, BufferedWriter output) throws Exception {
        String login = input.readLine();
        String mdp = input.readLine();
        String newFtp = input.readLine();
        boolean ok = registry.updateFtpUrl(login, mdp, newFtp);
        output.write(ok ? "OK URL FTP mise a jour\n" : "ERROR Auth invalide\n");
    }
}