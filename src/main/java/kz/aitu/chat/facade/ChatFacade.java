package kz.aitu.chat.facade;

import kz.aitu.chat.adapter.ChatTransport;
import kz.aitu.chat.decorator.BasicMessageProcessor;
import kz.aitu.chat.decorator.EncryptionDecorator;
import kz.aitu.chat.decorator.LoggingDecorator;
import kz.aitu.chat.decorator.MessageProcessor;
import kz.aitu.chat.factory.MessageFactory;
import kz.aitu.chat.factory.MessageFactoryProvider;
import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import kz.aitu.chat.observer.MessageBroker;
import kz.aitu.chat.observer.MessageListener;
import kz.aitu.chat.strategy.EncryptionStrategy;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ChatFacade implements AutoCloseable {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final MessageFactoryProvider factories;
    private final MessageBroker broker;
    private final Consumer<String> logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "chat-sender");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();

    private ChatTransport transport;
    private EncryptionStrategy encryption;

    public ChatFacade(ChatTransport transport, EncryptionStrategy encryption) {
        this(new MessageFactoryProvider(), transport, encryption, new MessageBroker(), System.out::println);
    }

    public ChatFacade(
            MessageFactoryProvider factories,
            ChatTransport transport,
            EncryptionStrategy encryption,
            MessageBroker broker,
            Consumer<String> logger) {
        this.factories = Objects.requireNonNull(factories, "factories cannot be null");
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        this.encryption = Objects.requireNonNull(encryption, "encryption cannot be null");
        this.broker = Objects.requireNonNull(broker, "broker cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public Message send(User sender, MessageType type, String content) {
        validate(sender, type, content);

        synchronized (lock) {
            MessageFactory factory = factories.getFactory(type);
            MessageProcessor processor = new LoggingDecorator(
                    new EncryptionDecorator(new BasicMessageProcessor(), encryption),
                    logger
            );

            Message delivered;
            try {
                Message wireMessage = processor.process(factory.create(sender, content));
                transport.send(wireMessage);
                delivered = factory.create(sender, encryption.decrypt(wireMessage.getContent()));
            } catch (RuntimeException e) {
                throw new ChatException("Failed to send message from " + sender.name(), e);
            }

            broker.publish(delivered);
            return delivered;
        }
    }

    public CompletableFuture<Message> sendAsync(User sender, MessageType type, String content) {
        validate(sender, type, content);

        try {
            return CompletableFuture.supplyAsync(() -> send(sender, type, content), executor);
        } catch (RejectedExecutionException e) {
            throw new ChatException("Chat facade is closed", e);
        }
    }

    public boolean subscribe(MessageListener listener) {
        return broker.subscribe(listener);
    }

    public boolean unsubscribe(MessageListener listener) {
        return broker.unsubscribe(listener);
    }

    public void useTransport(ChatTransport transport) {
        Objects.requireNonNull(transport, "transport cannot be null");

        synchronized (lock) {
            this.transport = transport;
        }
    }

    public void useEncryption(EncryptionStrategy encryption) {
        Objects.requireNonNull(encryption, "encryption cannot be null");

        synchronized (lock) {
            this.encryption = encryption;
        }
    }

    @Override
    public void close() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void validate(User sender, MessageType type, String content) {
        Objects.requireNonNull(sender, "sender cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(content, "content cannot be null");

        if (content.isBlank()) {
            throw new IllegalArgumentException("content cannot be blank");
        }
    }
}
