package kz.aitu.chat.adapter;

import kz.aitu.chat.model.Message;

import java.util.Objects;

public final class XmppChatTransportAdapter implements ChatTransport {

    private final XmppClient xmppClient;

    public XmppChatTransportAdapter(XmppClient xmppClient) {
        this.xmppClient = Objects.requireNonNull(xmppClient);
    }

    @Override
    public void send(Message message) {
        xmppClient.sendMessage(
                message.getSender().id(),
                message.getContent()
        );
    }
}
