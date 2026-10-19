package support;

import bri.Service;
import bri.ServiceRegistry;
import demo.ReverseService;
import demo.UppercaseService;
import demo.WordCountService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Serveur de demonstration uniquement : ne remplace pas BRiLaunch (role A). */
public class AmateurTestServer {
    public static void registerServices() throws Exception {
        ServiceRegistry.addService(ReverseService.class);
        ServiceRegistry.addService(UppercaseService.class);
        ServiceRegistry.addService(WordCountService.class);
    }

    public static void serve(Socket socket) throws Exception {
        try (Socket connection = socket) {
            connection.setSoTimeout(120000);
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            output.write(ServiceRegistry.toStringue());
            output.newLine(); // Une ligne vide termine le menu.
            output.flush();
            String selection = input.readLine();
            if (selection == null) {
                return;
            }
            Class<?> serviceClass = null;
            try {
                serviceClass = ServiceRegistry.getServiceClass(Integer.parseInt(selection));
            } catch (NumberFormatException ignored) {
                // Meme reponse que pour un numero inexistant.
            }
            if (serviceClass == null) {
                output.write("ERROR Service inconnu.\n");
                output.flush();
                return;
            }
            Service service = (Service) serviceClass.getConstructor(Socket.class).newInstance(connection);
            service.run();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
        registerServices();
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getLoopbackAddress())) {
            System.out.println("Serveur de test amateur : localhost:" + server.getLocalPort());
            System.out.println("Arret : Ctrl+C");
            while (true) {
                Socket connection = server.accept();
                new Thread(() -> {
                    try {
                        serve(connection);
                    } catch (Exception e) {
                        System.err.println("Session de test : " + e.getMessage());
                    }
                }).start();
            }
        }
    }
}
