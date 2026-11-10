package kz.aitu.chat.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RestChatClient {

    private final List<String> postedMessages = new ArrayList<>();

    public void postMessage(String author, String text) {
        postedMessages.add("REST from " + author + ": " + text);
    }

    public List<String> getPostedMessages() {
        return Collections.unmodifiableList(postedMessages);
    }
}