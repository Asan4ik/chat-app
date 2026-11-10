package kz.aitu.chat.decorator;

import kz.aitu.chat.model.Message;

import java.util.Objects;
import java.util.function.Consumer;

/** Adds a simple processing log without changing the message content. */
public final class LoggingDecorator extends MessageDecorator {

    private final Consumer<String> logger;

    public LoggingDecorator(MessageProcessor processor) {
        this(processor, System.out::println);
    }

    public LoggingDecorator(MessageProcessor processor, Consumer<String> logger) {
        super(processor);
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    @Override
    public Message process(Message message) {
        logger.accept("Processing message from: " + message.getSender().name());
        Message result = processor.process(message);
        logger.accept("Message processed.");
        return result;
    }
}
