package kz.aitu.chat;

import kz.aitu.chat.adapter.ChatTransport;
import kz.aitu.chat.adapter.RestChatClient;
import kz.aitu.chat.adapter.RestChatTransportAdapter;
import kz.aitu.chat.factory.MessageFactoryProvider;
import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;

public class Main {

    public static void main(String[] args) {
        User sender = new User("u-1", "Mansur");

        MessageFactoryProvider factoryProvider =
                new MessageFactoryProvider();

        Message message = factoryProvider
                .getFactory(MessageType.TEXT)
                .create(sender, "Hello team");

        RestChatClient restClient = new RestChatClient();
        ChatTransport transport =
                new RestChatTransportAdapter(restClient);

        transport.send(message);

        System.out.println(
                "Sent messages: " + restClient.getPostedMessages()
        );
    }
}