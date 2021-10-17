package io.maestro.core.exception;

public class BadSagaTypeException extends RuntimeException {
    String message;

    public BadSagaTypeException(String message){
        super(message);
    }
}
