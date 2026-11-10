package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InboxListener implements MessageListener {

    private final User owner;
    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public InboxListener(User owner) {
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
    }

    public User getOwner() {
        return owner;
    }

    @Override
    public void onMessage(Message message) {
        Objects.requireNonNull(message, "message cannot be null");

        if (!owner.equals(message.getSender())) {
            messages.add(message);
        }
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int size() {
        return messages.size();
    }

    @Override
    public String toString() {
        return "InboxListener[" + owner.name() + "]";
    }
}
