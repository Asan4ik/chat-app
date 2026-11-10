package kz.aitu.chat.observer;

import kz.aitu.chat.model.Message;
import kz.aitu.chat.model.MessageType;
import kz.aitu.chat.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageBrokerTest {

    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final MessageBroker broker = new MessageBroker(errors::add);
    private final Message message = new Message(new User("u-1", "Asanali"), MessageType.TEXT, "Hi");

    @Test
    void publishesToAllSubscribers() {
        List<Message> first = new ArrayList<>();
        List<Message> second = new ArrayList<>();
        broker.subscribe(first::add);
        broker.subscribe(second::add);

        broker.publish(message);

        assertEquals(List.of(message), first);
        assertEquals(List.of(message), second);
    }

    @Test
    void unsubscribedListenerReceivesNothing() {
        List<Message> received = new ArrayList<>();
        MessageListener listener = received::add;
        broker.subscribe(listener);

        assertTrue(broker.unsubscribe(listener));
        broker.publish(message);

        assertTrue(received.isEmpty());
        assertFalse(broker.unsubscribe(listener));
    }

    @Test
    void duplicateSubscriptionIsIgnored() {
        AtomicInteger calls = new AtomicInteger();
        MessageListener listener = ignored -> calls.incrementAndGet();

        assertTrue(broker.subscribe(listener));
        assertFalse(broker.subscribe(listener));
        broker.publish(message);

        assertEquals(1, calls.get());
        assertEquals(1, broker.listenerCount());
    }

    @Test
    void failingListenerDoesNotBlockOthers() {
        List<Message> received = new ArrayList<>();
        broker.subscribe(ignored -> {
            throw new IllegalStateException("boom");
        });
        broker.subscribe(received::add);

        broker.publish(message);

        assertEquals(List.of(message), received);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("boom"));
    }

    @Test
    void listenerMayUnsubscribeItselfWhilePublishing() {
        AtomicInteger calls = new AtomicInteger();
        MessageListener[] holder = new MessageListener[1];
        holder[0] = ignored -> {
            calls.incrementAndGet();
            broker.unsubscribe(holder[0]);
        };
        broker.subscribe(holder[0]);

        broker.publish(message);
        broker.publish(message);

        assertEquals(1, calls.get());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> broker.subscribe(null));
        assertThrows(NullPointerException.class, () -> broker.unsubscribe(null));
        assertThrows(NullPointerException.class, () -> broker.publish(null));
        assertThrows(NullPointerException.class, () -> new MessageBroker(null));
    }

    @Test
    void handlesConcurrentSubscribeAndPublish() throws InterruptedException {
        int threads = 8;
        int messagesPerThread = 200;
        AtomicInteger delivered = new AtomicInteger();
        broker.subscribe(ignored -> delivered.incrementAndGet());

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                for (int i = 0; i < messagesPerThread; i++) {
                    MessageListener extra = ignored -> {};
                    broker.subscribe(extra);
                    broker.publish(message);
                    broker.unsubscribe(extra);
                }
                done.countDown();
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads * messagesPerThread, delivered.get());
        assertEquals(1, broker.listenerCount());
        assertTrue(errors.isEmpty());
    }
}
