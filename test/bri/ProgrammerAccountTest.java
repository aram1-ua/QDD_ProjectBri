package bri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Account validation, authentication, URL changes and concurrent duplicate registration. */
public final class ProgrammerAccountTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        ProgrammeurRegistry registry = new ProgrammeurRegistry();
        check("registration", registry.register("dodo", "1234", "ftp://localhost/services/"));
        check("unknown login", !registry.authenticate("dora", "1234"));
        check("wrong password", !registry.authenticate("dodo", "wrong"));
        check("null password", !registry.authenticate("dodo", null));
        check("valid authentication", registry.authenticate("dodo", "1234"));
        check("update authentication", !registry.updateFtpUrl("dodo", "wrong", "ftp://localhost/new/"));
        check("update", registry.updateFtpUrl("dodo", "1234", "ftp://localhost/new/"));
        check("updated value", registry.getAccount("dodo").getFtpUrl().equals("ftp://localhost/new/"));
        check("duplicate", !registry.register("dodo", "1234", "ftp://localhost/"));
        for (String login : new String[]{null, "", "two words", "a.b", "class", "java", "javax", "bri", "_"}) {
            check("invalid login " + login, !registry.register(login, "secret", "ftp://localhost/"));
        }
        check("empty password", !registry.register("empty", " ", "ftp://localhost/"));
        for (String url : new String[]{null, "", "http://localhost/", "file:///tmp/", "ftp:///no-host", "ftp://host:0/", "ftp://host:70000/", "ftp://host/?query", "ftp://host/#fragment"}) {
            check("invalid ftp " + url, !registry.register("invalid", "secret", url));
            check("invalid update " + url, !registry.updateFtpUrl("dodo", "1234", url));
        }
        ExecutorService clients = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                results.add(clients.submit(() -> registry.register("race", "secret", "ftp://localhost/")));
            }
            int successes = 0;
            for (Future<Boolean> result : results) if (result.get(5, TimeUnit.SECONDS)) successes++;
            check("one concurrent winner", successes == 1);
        } finally { clients.shutdownNow(); }
        System.out.println("OK : " + checks + " account verifications.");
    }

    private static void check(String label, boolean condition) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
