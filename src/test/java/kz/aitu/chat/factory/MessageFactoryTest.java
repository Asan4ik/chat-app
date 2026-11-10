package kz.aitu.chat.factory;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageFactoryTest {

    private final User sender = new User("u-1", "Mansur");
    private final MessageFactoryProvider provider =
            new MessageFactoryProvider();

    @Test
    void createsMessageWithRequestedType() {
        Message message = provider
                .getFactory(MessageType.IMAGE)
                .create(sender, "photo.jpg");

        assertEquals(MessageType.IMAGE, message.getType());
        assertEquals("photo.jpg", message.getContent());
        assertEquals(sender, message.getSender());
    }
}
