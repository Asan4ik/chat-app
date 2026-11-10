package kz.aitu.chat.adapter;

import kz.aitu.chat.model.Message;

import java.util.Objects;

public final class RestChatTransportAdapter implements ChatTransport {

    private final RestChatClient restClient;

    public RestChatTransportAdapter(RestChatClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public void send(Message message) {
        restClient.postMessage(
                message.getSender().id(),
                message.getContent()
        );
    }
}