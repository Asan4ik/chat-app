package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;

public interface MessageListener {
    void onMessage(Message message);
}
