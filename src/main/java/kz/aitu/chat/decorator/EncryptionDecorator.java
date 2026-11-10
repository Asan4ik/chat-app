package kz.aitu.chat.decorator;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.strategy.EncryptionStrategy;

import java.util.Objects;

/** Adds encryption/encoding to the processing performed by the wrapped processor. */
public final class EncryptionDecorator extends MessageDecorator {

    private final EncryptionStrategy strategy;

    public EncryptionDecorator(
            MessageProcessor processor,
            EncryptionStrategy strategy) {
        super(processor);
        this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
    }

    @Override
    public Message process(Message message) {
        Message processedMessage = processor.process(message);
        processedMessage.setContent(strategy.encrypt(processedMessage.getContent()));
        return processedMessage;
    }
}
