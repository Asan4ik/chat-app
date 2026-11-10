package kz.aitu.chat.decorator;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import kz.aitu.chat.strategy.CaesarStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageProcessorTest {

    private Message createMessage() {
        return new Message(new User("u-1", "Sanzhar"), MessageType.TEXT, "Hello");
    }

    @Test
    void basicProcessorReturnsSameMessage() {
        Message message = createMessage();

        Message result = new BasicMessageProcessor().process(message);

        assertSame(message, result);
        assertEquals("Hello", result.getContent());
    }

    @Test
    void encryptionDecoratorEncryptsMessageUsingSelectedStrategy() {
        Message message = createMessage();
        MessageProcessor processor = new EncryptionDecorator(
                new BasicMessageProcessor(),
                new CaesarStrategy()
        );

        Message result = processor.process(message);

        assertSame(message, result);
        assertEquals("Khoor", result.getContent());
    }

    @Test
    void loggingDecoratorCanWrapEncryptionDecorator() {
        Message message = createMessage();
        List<String> logs = new ArrayList<>();

        MessageProcessor processor = new LoggingDecorator(
                new EncryptionDecorator(
                        new BasicMessageProcessor(),
                        new CaesarStrategy()
                ),
                logs::add
        );

        Message result = processor.process(message);

        assertEquals("Khoor", result.getContent());
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).contains("Sanzhar"));
        assertEquals("Message processed.", logs.get(1));
    }

    @Test
    void decoratorsCanBeCombinedInDifferentLayers() {
        Message message = createMessage();

        MessageProcessor processor = new EncryptionDecorator(
                new LoggingDecorator(new BasicMessageProcessor(), ignored -> {}),
                new CaesarStrategy()
        );

        assertEquals("Khoor", processor.process(message).getContent());
    }
}
