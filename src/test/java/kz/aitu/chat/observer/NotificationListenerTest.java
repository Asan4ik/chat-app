package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationListenerTest {

    private final User recipient = new User("u-1", "Asanali");
    private final User other = new User("u-2", "Sanzhar");
    private final List<String> output = new ArrayList<>();
    private final NotificationListener listener = new NotificationListener(recipient, output::add);

    @Test
    void announcesMessagesFromOtherUsers() {
        listener.onMessage(new Message(other, MessageType.IMAGE, "cat.png"));

        assertEquals(List.of("[Asanali] new image message from Sanzhar: cat.png"), output);
    }

    @Test
    void staysSilentForOwnMessages() {
        listener.onMessage(new Message(recipient, MessageType.TEXT, "Hello"));

        assertTrue(output.isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new NotificationListener(null));
        assertThrows(NullPointerException.class, () -> new NotificationListener(recipient, null));
        assertThrows(NullPointerException.class, () -> listener.onMessage(null));
    }
}
