package kz.aitu.chat.strategy;

import java.util.Objects;

/**
 * Demonstration Caesar cipher. Shifts English letters by three positions.
 * Non-letter characters are preserved.
 */
public final class CaesarStrategy implements EncryptionStrategy {

    private static final int SHIFT = 3;

    @Override
    public String encrypt(String text) {
        Objects.requireNonNull(text, "text cannot be null");
        return shift(text, SHIFT);
    }

    @Override
    public String decrypt(String text) {
        Objects.requireNonNull(text, "text cannot be null");
        return shift(text, -SHIFT);
    }

    private String shift(String text, int amount) {
        StringBuilder result = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (ch >= 'a' && ch <= 'z') {
                result.append((char) ('a' + (ch - 'a' + amount + 26) % 26));
            } else if (ch >= 'A' && ch <= 'Z') {
                result.append((char) ('A' + (ch - 'A' + amount + 26) % 26));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
