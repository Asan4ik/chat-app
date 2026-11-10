package kz.aitu.chat;

import kz.aitu.chat.cli.ChatCli;

public class Main {

    public static void main(String[] args) {
        new ChatCli(System.in, System.out).run();
    }
}
