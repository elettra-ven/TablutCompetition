package it.unibo.ai.didattica.competition.tablut.alielegio;

public class OurClient {
    public static void main(String[] args) {
        String role = args[0].toUpperCase();
        int timeout = Integer.parseInt(args[1]);
        String ip = args[2];
        System.out.println("role=" + role + " timeout=" + timeout + " ip=" + ip);
    }
}