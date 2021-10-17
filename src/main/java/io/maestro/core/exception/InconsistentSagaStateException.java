package io.maestro.core.exception;

public class InconsistentSagaStateException extends RuntimeException {

    public InconsistentSagaStateException(String message) {
        super(message);
    }
}
