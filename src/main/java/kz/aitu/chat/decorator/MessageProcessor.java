package kz.aitu.chat.decorator;

import kz.aitu.chat.model.Message;

public interface MessageProcessor {
    Message process(Message message);
}
