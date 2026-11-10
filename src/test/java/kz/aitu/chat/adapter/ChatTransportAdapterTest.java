package kz.aitu.chat.adapter;

import kz.aitu.chat.factory.TextMessageFactory;
import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTransportAdapterTest {

    private final Message message = new TextMessageFactory()
            .create(new User("u-1", "Mansur"), "Hello team");

    @Test
    void adaptsMessageToXmppClient() {
        XmppClient client = new XmppClient();
        ChatTransport transport =
                new XmppChatTransportAdapter(client);

        transport.send(message);

        assertEquals(
                "XMPP from u-1: Hello team",
                client.getSentMessages().get(0)
        );
    }

    @Test
    void adaptsMessageToRestClient() {
        RestChatClient client = new RestChatClient();
        ChatTransport transport =
                new RestChatTransportAdapter(client);

        transport.send(message);

        assertEquals(
                "REST from u-1: Hello team",
                client.getPostedMessages().get(0)
        );
    }
}
