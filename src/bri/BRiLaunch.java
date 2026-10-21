package bri;

import demo.ReverseService;
import demo.UppercaseService;
import demo.WordCountService;

import java.net.ServerSocket;
import java.net.Socket;

public class BRiLaunch{
    public static void main(String[] args) {
        // Enregistrement initial des services dans le registre
        try {
            ServiceRegistry.addService(ReverseService.class);
            ServiceRegistry.addService(UppercaseService.class);
            ServiceRegistry.addService(WordCountService.class);
            System.out.println("Services enregistrés dans le registre avec succès.");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'enregistrement des services : " + e.getMessage());
        }

        // --- PORT AMATEURS (3000) ---
        new Thread(() -> {
            try (ServerSocket serverAmateur = new ServerSocket(3000)) {
                System.out.println("Serveur Amateur démarré sur le port 3000...");

                while (true) {
                    Socket socket = serverAmateur.accept();
                    new Thread(new ServiceAmateur(socket)).start();
                }
            } catch (Exception e) {
                System.err.println("Erreur serveur amateur : " + e.getMessage());
            }
        }).start();


        // --- PORT PROGRAMMEURS (4000) ---
        new Thread(() -> {
            try (ServerSocket serverProg = new ServerSocket(4000)) {
                System.out.println("Serveur Programmeur démarré sur le port 4000...");

                while (true) {
                    Socket socket = serverProg.accept();

                    // new Thread(new ServiceProg(socket)).start();

                }
            } catch (Exception e) {
                System.err.println("Erreur serveur programmeur : " + e.getMessage());
            }
        }).start();
    }
}