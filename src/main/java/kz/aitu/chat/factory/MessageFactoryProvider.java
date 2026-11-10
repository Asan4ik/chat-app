package kz.aitu.chat.factory;

import kz.aitu.chat.model.MessageType;

import java.util.EnumMap;
import java.util.Map;

public final class MessageFactoryProvider {

    private final Map<MessageType, MessageFactory> factories =
            new EnumMap<>(MessageType.class);

    public MessageFactoryProvider() {
        factories.put(MessageType.TEXT, new TextMessageFactory());
        factories.put(MessageType.IMAGE, new ImageMessageFactory());
        factories.put(MessageType.SYSTEM, new SystemMessageFactory());
    }

    public MessageFactory getFactory(MessageType type) {
        MessageFactory factory = factories.get(type);

        if (factory == null) {
            throw new IllegalArgumentException(
                    "Unsupported message type: " + type
            );
        }

        return factory;
    }
}
