package kz.aitu.chat.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ChatCliTest {

    private String run(String... lines) {
        String script = String.join("\n", lines) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ChatCli(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), out).run();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void notifiesOtherParticipantsAndFillsTheirInboxes() {
        String output = run("hello team", "/inbox Sanzhar", "/quit");

        assertTrue(output.contains("[Sanzhar] new text message from Asanali: hello team"));
        assertTrue(output.contains("[Mansur] new text message from Asanali: hello team"));
        assertFalse(output.contains("[Asanali] new"));
        assertTrue(output.contains("Inbox of Sanzhar (1):"));
    }

    @Test
    void encryptsOnTheWireButNotInInboxes() {
        String output = run("/encrypt caesar", "/transport xmpp", "Hello", "/wire", "/inbox Mansur", "/quit");

        assertTrue(output.contains("XMPP from u-1: Khoor"));
        assertTrue(output.contains("  Asanali: Hello"));
    }

    @Test
    void leavingParticipantStopsReceiving() {
        String output = run("/leave Mansur", "hi", "/quit");

        assertTrue(output.contains("Mansur left the chat."));
        assertFalse(output.contains("[Mansur] new"));
        assertTrue(output.contains("[Sanzhar] new"));
    }

    @Test
    void joinedParticipantBecomesCurrentUser() {
        String output = run("/join Aigerim", "hi", "/quit");

        assertTrue(output.contains("Sending as Aigerim."));
        assertTrue(output.contains("[Asanali] new text message from Aigerim: hi"));
    }

    @Test
    void burstDeliversEveryMessage() {
        String output = run("/burst 30", "/inbox Mansur", "/quit");

        assertTrue(output.contains("Burst complete: 30 messages sent."));
        assertTrue(output.contains("Inbox of Mansur (20):"));
    }

    @Test
    void reportsErrorsAndKeepsRunning() {
        String output = run(
                "/foo", "/as Nobody", "/type video", "/encrypt rot13", "/transport smtp",
                "/burst 0", "/burst abc", "/join Asanali", "/join", "hello", "/quit"
        );

        assertTrue(output.contains("Error: Unknown command /foo"));
        assertTrue(output.contains("Error: No such user: Nobody"));
        assertTrue(output.contains("Error: Unknown message type: video"));
        assertTrue(output.contains("Error: Unknown encryption: rot13"));
        assertTrue(output.contains("Error: Unknown transport: smtp"));
        assertTrue(output.contains("Error: Burst size must be between 1 and 100"));
        assertTrue(output.contains("Error: Burst size must be a number: abc"));
        assertTrue(output.contains("Error: User already in chat: Asanali"));
        assertTrue(output.contains("Error: Usage: /join <name>"));
        assertTrue(output.contains("[Sanzhar] new text message from Asanali: hello"));
        assertTrue(output.endsWith("Bye." + System.lineSeparator()));
    }

    @Test
    void refusesToSendWithoutCurrentUser() {
        String output = run("/leave Asanali", "/leave Sanzhar", "/leave Mansur", "hello", "/quit");

        assertTrue(output.contains("Error: No current user"));
    }

    @Test
    void endsWhenInputIsExhausted() {
        String output = run("hello");

        assertTrue(output.contains("[Sanzhar] new text message from Asanali: hello"));
    }

    @Test
    void rejectsNullStreams() {
        assertThrows(NullPointerException.class, () -> new ChatCli(null, System.out));
        assertThrows(NullPointerException.class, () -> new ChatCli(System.in, null));
    }
}
