package io.maestro.core.saga.definition.step;

public interface StepOutcome<Data> {
    boolean isSuccessful();
}
