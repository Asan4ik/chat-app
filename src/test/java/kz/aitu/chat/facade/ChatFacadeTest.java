package kz.aitu.chat.facade;

import kz.aitu.chat.adapter.RestChatClient;
import kz.aitu.chat.adapter.RestChatTransportAdapter;
import kz.aitu.chat.adapter.XmppChatTransportAdapter;
import kz.aitu.chat.adapter.XmppClient;
import kz.aitu.chat.factory.MessageFactoryProvider;
import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import kz.aitu.chat.observer.InboxListener;
import kz.aitu.chat.observer.MessageBroker;
import kz.aitu.chat.strategy.Base64EncodingStrategy;
import kz.aitu.chat.strategy.CaesarStrategy;
import kz.aitu.chat.strategy.NoEncryptionStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatFacadeTest {

    private final User sender = new User("u-1", "Asanali");
    private final User receiver = new User("u-2", "Sanzhar");
    private final RestChatClient restClient = new RestChatClient();
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final InboxListener inbox = new InboxListener(receiver);
    private final ChatFacade facade = new ChatFacade(
            new MessageFactoryProvider(),
            new RestChatTransportAdapter(restClient),
            new CaesarStrategy(),
            new MessageBroker(errors::add),
            logs::add
    );

    ChatFacadeTest() {
        facade.subscribe(inbox);
    }

    @AfterEach
    void tearDown() {
        facade.close();
    }

    @Test
    void sendsEncryptedOnTheWireAndPlaintextToListeners() {
        Message delivered = facade.send(sender, MessageType.TEXT, "Hello");

        assertEquals(List.of("REST from u-1: Khoor"), restClient.getPostedMessages());
        assertEquals("Hello", delivered.getContent());
        assertEquals(1, inbox.size());
        assertEquals("Hello", inbox.getMessages().get(0).getContent());
    }

    @Test
    void createsMessageOfRequestedType() {
        Message delivered = facade.send(sender, MessageType.IMAGE, "cat.png");

        assertEquals(MessageType.IMAGE, delivered.getType());
        assertEquals(sender, delivered.getSender());
    }

    @Test
    void logsProcessingThroughTheDecoratorChain() {
        facade.send(sender, MessageType.TEXT, "Hello");

        assertEquals(2, logs.size());
        assertTrue(logs.get(0).contains("Asanali"));
    }

    @Test
    void switchesTransportAtRuntime() {
        XmppClient xmppClient = new XmppClient();
        facade.useTransport(new XmppChatTransportAdapter(xmppClient));

        facade.send(sender, MessageType.TEXT, "Hello");

        assertEquals(List.of("XMPP from u-1: Khoor"), xmppClient.getSentMessages());
        assertTrue(restClient.getPostedMessages().isEmpty());
    }

    @Test
    void switchesEncryptionAtRuntime() {
        facade.useEncryption(new Base64EncodingStrategy());
        facade.send(sender, MessageType.TEXT, "Hello");
        facade.useEncryption(new NoEncryptionStrategy());
        facade.send(sender, MessageType.TEXT, "Hello");

        assertEquals(
                List.of("REST from u-1: SGVsbG8=", "REST from u-1: Hello"),
                restClient.getPostedMessages()
        );
        assertEquals(2, inbox.size());
        assertEquals("Hello", inbox.getMessages().get(0).getContent());
    }

    @Test
    void unsubscribedListenerStopsReceiving() {
        facade.send(sender, MessageType.TEXT, "one");
        assertTrue(facade.unsubscribe(inbox));
        facade.send(sender, MessageType.TEXT, "two");

        assertEquals(1, inbox.size());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> facade.send(null, MessageType.TEXT, "Hi"));
        assertThrows(NullPointerException.class, () -> facade.send(sender, null, "Hi"));
        assertThrows(NullPointerException.class, () -> facade.send(sender, MessageType.TEXT, null));
        assertThrows(IllegalArgumentException.class, () -> facade.send(sender, MessageType.TEXT, "   "));
        assertTrue(restClient.getPostedMessages().isEmpty());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(NullPointerException.class, () -> facade.useTransport(null));
        assertThrows(NullPointerException.class, () -> facade.useEncryption(null));
        assertThrows(NullPointerException.class, () -> new ChatFacade(null, new CaesarStrategy()));
        assertThrows(NullPointerException.class, () -> new ChatFacade(new RestChatTransportAdapter(restClient), null));
    }

    @Test
    void wrapsTransportFailureAndSkipsListeners() {
        facade.useTransport(message -> {
            throw new IllegalStateException("network down");
        });

        ChatException error = assertThrows(
                ChatException.class,
                () -> facade.send(sender, MessageType.TEXT, "Hello")
        );

        assertEquals("network down", error.getCause().getMessage());
        assertEquals(0, inbox.size());
    }

    @Test
    void failingListenerDoesNotBreakSending() {
        facade.subscribe(message -> {
            throw new IllegalStateException("listener broke");
        });

        Message delivered = facade.send(sender, MessageType.TEXT, "Hello");

        assertEquals("Hello", delivered.getContent());
        assertEquals(1, inbox.size());
        assertEquals(1, errors.size());
    }

    @Test
    void asyncSendsKeepSubmissionOrder() throws Exception {
        int count = 50;
        List<CompletableFuture<Message>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(facade.sendAsync(sender, MessageType.TEXT, "m" + i));
        }

        for (CompletableFuture<Message> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        assertEquals(count, restClient.getPostedMessages().size());
        for (int i = 0; i < count; i++) {
            assertEquals("m" + i, inbox.getMessages().get(i).getContent());
        }
    }

    @Test
    void asyncFailureCompletesFutureExceptionally() {
        facade.useTransport(message -> {
            throw new IllegalStateException("network down");
        });

        CompletableFuture<Message> future = facade.sendAsync(sender, MessageType.TEXT, "Hello");

        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        assertInstanceOf(ChatException.class, error.getCause());
    }

    @Test
    void asyncSendValidatesBeforeSubmitting() {
        assertThrows(IllegalArgumentException.class, () -> facade.sendAsync(sender, MessageType.TEXT, ""));
    }

    @Test
    void concurrentSendsFromManyThreadsAreNotLost() throws InterruptedException {
        int threads = 8;
        int messagesPerThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            int id = t;
            pool.execute(() -> {
                for (int i = 0; i < messagesPerThread; i++) {
                    facade.send(sender, MessageType.TEXT, "t" + id + "-" + i);
                }
                done.countDown();
            });
        }

        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads * messagesPerThread, restClient.getPostedMessages().size());
        assertEquals(threads * messagesPerThread, inbox.size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void closedFacadeRejectsAsyncSends() {
        facade.close();

        assertThrows(ChatException.class, () -> facade.sendAsync(sender, MessageType.TEXT, "Hello"));
    }

    @Test
    void closeIsIdempotent() {
        facade.close();

        assertDoesNotThrow(facade::close);
    }
}
