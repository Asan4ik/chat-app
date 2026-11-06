package kz.aitu.chat.adapter;

import kz.aitu.chat.model.Message;

public interface ChatTransport {
    void send(Message message);
}
