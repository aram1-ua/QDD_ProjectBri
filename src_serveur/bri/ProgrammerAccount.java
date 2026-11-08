package bri;

public class ProgrammerAccount {
    private final String login, password;

    private volatile String ftpUrl;

    public ProgrammerAccount(String login, String password, String ftpUrl){
        this.login = login;
        this.password = password;
        this.ftpUrl = ftpUrl;
    }
    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getFtpUrl() {
        return ftpUrl;
    }

    public void setFtpUrl(String ftpUrl) {
        this.ftpUrl = ftpUrl;
    }
}
