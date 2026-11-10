package kz.aitu.chat.cli;

import kz.aitu.chat.adapter.ChatTransport;
import kz.aitu.chat.adapter.RestChatClient;
import kz.aitu.chat.adapter.RestChatTransportAdapter;
import kz.aitu.chat.adapter.XmppChatTransportAdapter;
import kz.aitu.chat.adapter.XmppClient;
import kz.aitu.chat.facade.ChatException;
import kz.aitu.chat.facade.ChatFacade;
import kz.aitu.chat.factory.MessageFactoryProvider;
import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import kz.aitu.chat.observer.InboxListener;
import kz.aitu.chat.observer.MessageBroker;
import kz.aitu.chat.observer.NotificationListener;
import kz.aitu.chat.strategy.Base64EncodingStrategy;
import kz.aitu.chat.strategy.CaesarStrategy;
import kz.aitu.chat.strategy.EncryptionStrategy;
import kz.aitu.chat.strategy.NoEncryptionStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ChatCli {

    private static final int MAX_BURST = 100;
    private static final List<String> DEFAULT_USERS = List.of("Asanali", "Sanzhar", "Mansur");
    private static final String HELP = String.join(System.lineSeparator(),
            "Type a line of text to send it as the current user. Commands:",
            "  /help                      show this help",
            "  /users                     list chat participants",
            "  /join <name>               add a participant and switch to them",
            "  /leave <name>              remove a participant",
            "  /as <name>                 switch the sending user",
            "  /type text|image|system    choose the message type",
            "  /encrypt none|caesar|base64  choose the encryption strategy",
            "  /transport rest|xmpp       choose the transport",
            "  /inbox [name]              show what a participant received",
            "  /wire                      show what the transports carried",
            "  /burst <n>                 send n messages concurrently",
            "  /quit                      exit"
    );

    private record Participant(User user, InboxListener inbox, NotificationListener notifications) {}

    private final BufferedReader in;
    private final PrintStream out;
    private final RestChatClient restClient = new RestChatClient();
    private final XmppClient xmppClient = new XmppClient();
    private final ChatFacade facade;
    private final Map<String, Participant> participants = new LinkedHashMap<>();

    private Participant current;
    private MessageType messageType = MessageType.TEXT;
    private int nextUserId = 1;

    public ChatCli(InputStream input, PrintStream output) {
        Objects.requireNonNull(input, "input cannot be null");
        this.out = Objects.requireNonNull(output, "output cannot be null");
        this.in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.facade = new ChatFacade(
                new MessageFactoryProvider(),
                new RestChatTransportAdapter(restClient),
                new NoEncryptionStrategy(),
                new MessageBroker(output::println),
                line -> output.println("  [log] " + line)
        );
        DEFAULT_USERS.forEach(this::join);
        current = participants.get(key(DEFAULT_USERS.get(0)));
    }

    public void run() {
        out.println("Chat app. Type /help for commands.");
        out.println("Sending as " + current.user().name() + ".");

        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (!handle(line.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read input", e);
        } finally {
            facade.close();
        }
    }

    private boolean handle(String line) {
        if (line.isEmpty()) {
            return true;
        }

        try {
            if (line.startsWith("/")) {
                return command(line);
            }
            send(line);
        } catch (ChatException | IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }

        return true;
    }

    private boolean command(String line) {
        String[] parts = line.split("\\s+", 2);
        String name = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (name) {
            case "/help" -> out.println(HELP);
            case "/users" -> listUsers();
            case "/join" -> {
                join(requireArgument(argument, "/join <name>"));
                switchTo(argument);
            }
            case "/leave" -> leave(requireArgument(argument, "/leave <name>"));
            case "/as" -> switchTo(requireArgument(argument, "/as <name>"));
            case "/type" -> chooseType(requireArgument(argument, "/type text|image|system"));
            case "/encrypt" -> chooseEncryption(requireArgument(argument, "/encrypt none|caesar|base64"));
            case "/transport" -> chooseTransport(requireArgument(argument, "/transport rest|xmpp"));
            case "/inbox" -> showInbox(argument.isEmpty() ? currentOrFail().user().name() : argument);
            case "/wire" -> showWire();
            case "/burst" -> burst(requireArgument(argument, "/burst <n>"));
            case "/quit" -> {
                out.println("Bye.");
                return false;
            }
            default -> throw new IllegalArgumentException("Unknown command " + name + ", try /help");
        }

        return true;
    }

    private void send(String content) {
        Participant sender = currentOrFail();
        facade.send(sender.user(), messageType, content);
    }

    private void join(String name) {
        String key = key(name);

        if (participants.containsKey(key)) {
            throw new IllegalArgumentException("User already in chat: " + name);
        }

        User user = new User("u-" + nextUserId++, name);
        Participant participant = new Participant(
                user,
                new InboxListener(user),
                new NotificationListener(user, out::println)
        );
        facade.subscribe(participant.inbox());
        facade.subscribe(participant.notifications());
        participants.put(key, participant);
    }

    private void leave(String name) {
        Participant participant = find(name);
        facade.unsubscribe(participant.inbox());
        facade.unsubscribe(participant.notifications());
        participants.remove(key(name));

        if (participant.equals(current)) {
            current = participants.values().stream().findFirst().orElse(null);
        }

        out.println(participant.user().name() + " left the chat.");
    }

    private void switchTo(String name) {
        current = find(name);
        out.println("Sending as " + current.user().name() + ".");
    }

    private void listUsers() {
        participants.values().forEach(participant -> out.println(
                (participant.equals(current) ? "* " : "  ") + participant.user().name()
        ));
    }

    private void chooseType(String value) {
        try {
            messageType = MessageType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown message type: " + value);
        }

        out.println("Message type: " + messageType.name().toLowerCase(Locale.ROOT));
    }

    private void chooseEncryption(String value) {
        EncryptionStrategy strategy = switch (value.toLowerCase(Locale.ROOT)) {
            case "none" -> new NoEncryptionStrategy();
            case "caesar" -> new CaesarStrategy();
            case "base64" -> new Base64EncodingStrategy();
            default -> throw new IllegalArgumentException("Unknown encryption: " + value);
        };

        facade.useEncryption(strategy);
        out.println("Encryption: " + value.toLowerCase(Locale.ROOT));
    }

    private void chooseTransport(String value) {
        ChatTransport transport = switch (value.toLowerCase(Locale.ROOT)) {
            case "rest" -> new RestChatTransportAdapter(restClient);
            case "xmpp" -> new XmppChatTransportAdapter(xmppClient);
            default -> throw new IllegalArgumentException("Unknown transport: " + value);
        };

        facade.useTransport(transport);
        out.println("Transport: " + value.toLowerCase(Locale.ROOT));
    }

    private void showInbox(String name) {
        Participant participant = find(name);
        List<Message> messages = participant.inbox().getMessages();

        out.println("Inbox of " + participant.user().name() + " (" + messages.size() + "):");
        messages.forEach(message -> out.println(
                "  " + message.getSender().name() + ": " + message.getContent()
        ));
    }

    private void showWire() {
        out.println("REST (" + restClient.getPostedMessages().size() + "):");
        restClient.getPostedMessages().forEach(line -> out.println("  " + line));
        out.println("XMPP (" + xmppClient.getSentMessages().size() + "):");
        xmppClient.getSentMessages().forEach(line -> out.println("  " + line));
    }

    private void burst(String value) {
        int count = parseBurstSize(value);
        currentOrFail();
        List<Participant> senders = new ArrayList<>(participants.values());
        List<CompletableFuture<Message>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Participant sender = senders.get(i % senders.size());
            futures.add(facade.sendAsync(sender.user(), messageType, "burst " + (i + 1)));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new ChatException("Burst failed: " + e.getCause().getMessage(), e.getCause());
        }

        out.println("Burst complete: " + count + " messages sent.");
    }

    private static int parseBurstSize(String value) {
        int count;
        try {
            count = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Burst size must be a number: " + value);
        }

        if (count < 1 || count > MAX_BURST) {
            throw new IllegalArgumentException("Burst size must be between 1 and " + MAX_BURST);
        }

        return count;
    }

    private Participant find(String name) {
        Participant participant = participants.get(key(name));

        if (participant == null) {
            throw new IllegalArgumentException("No such user: " + name);
        }

        return participant;
    }

    private Participant currentOrFail() {
        if (current == null) {
            throw new IllegalArgumentException("No current user, use /join <name> first");
        }

        return current;
    }

    private static String requireArgument(String argument, String usage) {
        if (argument.isEmpty()) {
            throw new IllegalArgumentException("Usage: " + usage);
        }

        return argument;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
