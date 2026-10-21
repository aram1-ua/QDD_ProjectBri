package bri;

import bri.ProgrammeurRegistry;
public class ProgrammerAccountTest {
    public static void main (String[] args) {
        ProgrammeurRegistry registry = new ProgrammeurRegistry();

        System.out.println( registry.register("dodo","1234","ftp://test/services"));
        ProgrammerAccount account = registry.getAccount("dodo");

        System.out.println( registry.authenticate("dora", "1234"));
        System.out.println( registry.getAccount("dodo"));
        System.out.println(account.getLogin()+ " " + account.getFtpUrl() + " ");
        System.out.println( registry.updateFtpUrl("dodo","1234", "ftp://test2/service2"));
        System.out.println(account.getLogin()+ " " + account.getFtpUrl() + " ");

        System.out.println("double account " + registry.register("dodo","1234", "ftp://test2"));



    }
}
