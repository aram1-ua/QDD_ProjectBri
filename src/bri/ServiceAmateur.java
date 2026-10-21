package bri;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServiceAmateur implements Runnable{
    private final Socket socket;
    public ServiceAmateur(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket connection = socket) {
            // Flux pour lire ce que le client nous envoie
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));

            // Flux pour écrire et envoyer du texte au client
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8));

            //pour envoyer le menu au client
            output.write(ServiceRegistry.toStringue());
            output.newLine();
            output.flush();

            String selection = input.readLine();
            if (selection == null){
                return;
            }

            Class<?> serviceClass = null;
            try{
                serviceClass = ServiceRegistry.getServiceClass(Integer.parseInt(selection));
            }catch (NumberFormatException ignored){
                //si l'utilisateur tape des lettres au lieu d'un nombre
            }

            if (serviceClass == null){
                output.write("ERROR Service inconnu.\n");
                output.flush();
                return;
            }

            Service service = (Service) serviceClass.getConstructor(Socket.class).newInstance(connection);
            service.run();


        } catch (Exception e) {
            System.err.println("Erreur session amateur : " + e.getMessage());
        }


    }
}