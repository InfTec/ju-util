package ch.inftec.ju.security;

/**
 * Interface for text encryption and decryption.
 * <p>
 * Similar to org.jasypt.util.text.TextEncryptor
 * @author martin.meyer@inftec.ch
 *
 */
public interface JuTextEncryptor {
    /**
     * Encrypts a message.
     * 
     * @param message the message to be encrypted.
     */
    public String encrypt(String message);

    /**
     * Decrypts a message.
     * 
     * @param encryptedMessage the message to be decrypted.
     */
    public String decrypt(String encryptedMessage);
}
