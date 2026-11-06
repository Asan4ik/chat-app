package kz.aitu.chat.factory;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.User;

public interface MessageFactory {
    Message create(User sender, String content);
}
