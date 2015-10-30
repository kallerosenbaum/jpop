package se.rosenbaum.jpop.generate;

public class PopGenerationException extends Exception {
    public PopGenerationException(String message) {
        this(message, null);
    }

    public PopGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
