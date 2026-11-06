package bri;

import java.util.HashMap;
import java.util.Map;

public class ProgrammeurRegistry {

    private final Map<String, ProgrammerAccount> accounts = new HashMap<>();

    public boolean register (String login, String password, String ftpUrl ){
        if ( login == null || login.isBlank() || password == null || password.isBlank() || ftpUrl == null || ftpUrl.isBlank()){
            return false;
        }

        if (accounts.containsKey(login)){ // if login is already used
            return false;
        }

        ProgrammerAccount account = new ProgrammerAccount(login, password, ftpUrl);

        accounts.put(login, account);
        return true;
    }

    public boolean authenticate(String login, String password) {
        ProgrammerAccount account = accounts.get(login);

        return account != null
                && account.getPassword().equals(password);
    }

    // how to update the url of an account ?
    public boolean updateFtpUrl (String login, String password, String newFtpUrl){
        ProgrammerAccount account = accounts.get(login);

        if ( account !=null && account.getPassword().equals(password) && newFtpUrl != null && !newFtpUrl.isBlank()){
            account.setFtpUrl(newFtpUrl);
            return true;
        }
        else {
            return false;
        }
    }

    public ProgrammerAccount getAccount ( String login) {
        return accounts.get(login);
    }

}
