package clientama;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import support.AmateurTestServer;

/** Tests d'integration sur de vraies sockets, sans bibliotheque externe. */
public class AmateurFlowTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        AmateurTestServer.registerServices();
        check("menu", exchange("1\nbonjour\n").contains("3. Compter les mots"));
        endsWith("inversion", exchange("1\nbonjour\n"), "ruojnob");
        endsWith("unicode", exchange("1\nété 😀\n"), "😀 été");
        endsWith("majuscules", exchange("2\nécole\n"), "ÉCOLE");
        endsWith("comptage", exchange("3\n  un\tdeux   trois  \n"), "3");
        endsWith("espaces unicode", exchange("3\nun\u00a0deux\n"), "2");
        endsWith("texte vide", exchange("1\n\n"), "");
        endsWith("aucun mot", exchange("3\n   \n"), "0");
        String retry = exchange("abc\n0\n1\nsalut\n");
        check("saisie invalide", retry.contains("Entrez un entier positif."));
        endsWith("reprise apres saisie invalide", retry, "tulas");
        endsWith("service inexistant", exchange("99\n"), "ERROR Service inconnu.");
        check("abandon", !exchange("/quit\n").contains("Entrez le texte"));
        check("fin de saisie", !exchange("").contains("Entrez le texte"));

        ExecutorService clients = Executors.newFixedThreadPool(3);
        try {
            Future<String> a = clients.submit(() -> exchange("1\nabc\n"));
            Future<String> b = clients.submit(() -> exchange("2\nabc\n"));
            Future<String> c = clients.submit(() -> exchange("3\na b\n"));
            endsWith("client parallele 1", a.get(10, TimeUnit.SECONDS), "cba");
            endsWith("client parallele 2", b.get(10, TimeUnit.SECONDS), "ABC");
            endsWith("client parallele 3", c.get(10, TimeUnit.SECONDS), "2");
        } finally {
            clients.shutdownNow();
        }
        System.out.println("OK : " + checks + " verifications.");
    }

    private static String exchange(String consoleInput) throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (ServerSocket listener = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            listener.setSoTimeout(5000);
            Future<?> session = worker.submit(() -> {
                try {
                    AmateurTestServer.serve(listener.accept());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            StringWriter output = new StringWriter();
            ClientAma.run(InetAddress.getLoopbackAddress().getHostAddress(), listener.getLocalPort(),
                    new StringReader(consoleInput), output);
            session.get(5, TimeUnit.SECONDS);
            return output.toString();
        } finally {
            worker.shutdownNow();
        }
    }

    private static void endsWith(String label, String output, String expected) {
        check(label, output.endsWith(expected + System.lineSeparator()));
    }

    private static void check(String label, boolean condition) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
