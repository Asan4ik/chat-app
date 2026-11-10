package kz.aitu.chat.decorator;

import java.util.Objects;

/** Base class for processors that wrap another MessageProcessor. */
public abstract class MessageDecorator implements MessageProcessor {

    protected final MessageProcessor processor;

    protected MessageDecorator(MessageProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor cannot be null");
    }
}
