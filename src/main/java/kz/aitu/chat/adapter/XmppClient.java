package kz.aitu.chat.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class XmppClient {

    private final List<String> sentMessages = new ArrayList<>();

    public void sendMessage(String senderId, String body) {
        sentMessages.add("XMPP from " + senderId + ": " + body);
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
