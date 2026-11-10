package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.User;

import java.util.Objects;
import java.util.function.Consumer;

public final class NotificationListener implements MessageListener {

    private final User recipient;
    private final Consumer<String> output;

    public NotificationListener(User recipient) {
        this(recipient, System.out::println);
    }

    public NotificationListener(User recipient, Consumer<String> output) {
        this.recipient = Objects.requireNonNull(recipient, "recipient cannot be null");
        this.output = Objects.requireNonNull(output, "output cannot be null");
    }

    @Override
    public void onMessage(Message message) {
        Objects.requireNonNull(message, "message cannot be null");

        if (!recipient.equals(message.getSender())) {
            output.accept(String.format(
                    "[%s] new %s message from %s: %s",
                    recipient.name(),
                    message.getType().name().toLowerCase(),
                    message.getSender().name(),
                    message.getContent()
            ));
        }
    }

    @Override
    public String toString() {
        return "NotificationListener[" + recipient.name() + "]";
    }
}
