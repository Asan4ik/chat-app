package kz.aitu.chat.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionStrategyTest {

    @Test
    void caesarEncryptsAndDecryptsText() {
        EncryptionStrategy strategy = new CaesarStrategy();

        assertEquals("Khoor, Zruog! 123", strategy.encrypt("Hello, World! 123"));
        assertEquals("Hello, World! 123", strategy.decrypt("Khoor, Zruog! 123"));
    }

    @Test
    void base64EncodesAndDecodesUtf8Text() {
        EncryptionStrategy strategy = new Base64EncodingStrategy();

        String original = "Hello, мир!";
        assertEquals(original, strategy.decrypt(strategy.encrypt(original)));
    }

    @Test
    void noEncryptionReturnsOriginalText() {
        EncryptionStrategy strategy = new NoEncryptionStrategy();

        assertEquals("Hello", strategy.encrypt("Hello"));
        assertEquals("Hello", strategy.decrypt("Hello"));
    }
}
