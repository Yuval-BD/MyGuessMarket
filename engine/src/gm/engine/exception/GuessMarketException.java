package gm.engine.exception;

public abstract class GuessMarketException extends RuntimeException {
    protected GuessMarketException(String message) {
        super(message);
    }
}