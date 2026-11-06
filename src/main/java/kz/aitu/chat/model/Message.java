package kz.aitu.chat.model;

import java.time.LocalDateTime;

public class Message {
    private final User sender;
    private final MessageType type;
    private String content;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public Message(User sender, MessageType type, String content) {
        this.sender = sender;
        this.type = type;
        this.content = content;
    }

    public User getSender() { return sender; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
