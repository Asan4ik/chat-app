package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InboxListenerTest {

    private final User owner = new User("u-1", "Asanali");
    private final User other = new User("u-2", "Sanzhar");
    private final InboxListener inbox = new InboxListener(owner);

    @Test
    void storesMessagesFromOtherUsers() {
        Message message = new Message(other, MessageType.TEXT, "Hello");

        inbox.onMessage(message);

        assertEquals(List.of(message), inbox.getMessages());
        assertEquals(1, inbox.size());
    }

    @Test
    void ignoresOwnMessages() {
        inbox.onMessage(new Message(owner, MessageType.TEXT, "Note to self"));

        assertEquals(0, inbox.size());
    }

    @Test
    void exposesReadOnlyView() {
        inbox.onMessage(new Message(other, MessageType.TEXT, "Hello"));

        assertThrows(UnsupportedOperationException.class, () -> inbox.getMessages().clear());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new InboxListener(null));
        assertThrows(NullPointerException.class, () -> inbox.onMessage(null));
    }
}
