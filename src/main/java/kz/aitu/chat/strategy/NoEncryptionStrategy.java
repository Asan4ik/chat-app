package kz.aitu.chat.strategy;

import java.util.Objects;

/** Strategy that leaves message content unchanged. */
public final class NoEncryptionStrategy implements EncryptionStrategy {

    @Override
    public String encrypt(String text) {
        return Objects.requireNonNull(text, "text cannot be null");
    }

    @Override
    public String decrypt(String text) {
        return Objects.requireNonNull(text, "text cannot be null");
    }
}
