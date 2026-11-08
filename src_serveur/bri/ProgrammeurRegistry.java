package bri;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import javax.lang.model.SourceVersion;

public class ProgrammeurRegistry {

    private final Map<String, ProgrammerAccount> accounts = new HashMap<>();

    public synchronized boolean register (String login, String password, String ftpUrl ){
        if (login == null || !SourceVersion.isIdentifier(login) || SourceVersion.isKeyword(login)
                || login.equals("java") || login.equals("javax") || login.equals("bri")
                || password == null || password.isBlank() || !validFtpUrl(ftpUrl)){
            return false;
        }

        if (accounts.containsKey(login)){ // if login is already used
            return false;
        }

        ProgrammerAccount account = new ProgrammerAccount(login, password, ftpUrl);

        accounts.put(login, account);
        return true;
    }

    public synchronized boolean authenticate(String login, String password) {
        ProgrammerAccount account = accounts.get(login);

        return account != null
                && account.getPassword().equals(password);
    }

    // Account operations are synchronized because programmer sessions run concurrently.
    public synchronized boolean updateFtpUrl (String login, String password, String newFtpUrl){
        ProgrammerAccount account = accounts.get(login);

        if ( account !=null && account.getPassword().equals(password) && validFtpUrl(newFtpUrl)){
            account.setFtpUrl(newFtpUrl);
            return true;
        }
        else {
            return false;
        }
    }

    public synchronized ProgrammerAccount getAccount ( String login) {
        return accounts.get(login);
    }

    private static boolean validFtpUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return "ftp".equals(uri.getScheme()) && uri.getHost() != null
                    && uri.getQuery() == null && uri.getFragment() == null
                    && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535);
        } catch (IllegalArgumentException e) { return false; }
    }

}
