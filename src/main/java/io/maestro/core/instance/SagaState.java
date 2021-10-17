package io.maestro.core.instance;

public enum SagaState {
    CREATED, EXECUTING, COMPENSATING, TERMINATED;
}
