package kz.aitu.chat.strategy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Base64 encoding strategy. Base64 is encoding, not secure encryption;
 * this class is useful for demonstrating interchangeable strategies.
 */
public final class Base64EncodingStrategy implements EncryptionStrategy {

    @Override
    public String encrypt(String text) {
        Objects.requireNonNull(text, "text cannot be null");
        return Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String text) {
        Objects.requireNonNull(text, "text cannot be null");
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }
}
