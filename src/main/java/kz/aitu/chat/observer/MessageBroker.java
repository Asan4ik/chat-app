package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class MessageBroker {

    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final Consumer<String> errorLogger;

    public MessageBroker() {
        this(System.err::println);
    }

    public MessageBroker(Consumer<String> errorLogger) {
        this.errorLogger = Objects.requireNonNull(errorLogger, "errorLogger cannot be null");
    }

    public boolean subscribe(MessageListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return listeners.addIfAbsent(listener);
    }

    public boolean unsubscribe(MessageListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        return listeners.remove(listener);
    }

    public int listenerCount() {
        return listeners.size();
    }

    public void publish(Message message) {
        Objects.requireNonNull(message, "message cannot be null");

        for (MessageListener listener : listeners) {
            try {
                listener.onMessage(message);
            } catch (RuntimeException e) {
                errorLogger.accept("Listener " + listener + " failed: " + e);
            }
        }
    }
}
