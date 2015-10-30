package se.rosenbaum.jpop.generate;

public class PopSigningException extends Exception {
    boolean badDecryptionKey = false;

    public PopSigningException(String message, Exception cause, boolean isBadDecryptionKey) {
        this.badDecryptionKey = isBadDecryptionKey;
    }

    public PopSigningException(String message, Exception cause) {
        super(message, cause);
    }

    public boolean isBadDecryptionKey() {
        return badDecryptionKey;
    }
}
