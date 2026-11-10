package kz.aitu.chat.decorator;

import kz.aitu.chat.model.Message;

import java.util.Objects;

/** Base processor: returns the message without additional processing. */
public final class BasicMessageProcessor implements MessageProcessor {

    @Override
    public Message process(Message message) {
        return Objects.requireNonNull(message, "message cannot be null");
    }
}
