package kz.aitu.chat.factory;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;

public final class ImageMessageFactory implements MessageFactory {

    @Override
    public Message create(User sender, String content) {
        return new Message(sender, MessageType.IMAGE, content);
    }
}