package kz.aitu.chat.strategy;

public interface EncryptionStrategy {
    String encrypt(String text);
    String decrypt(String text);
}
