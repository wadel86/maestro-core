package io.maestro.core.saga.definition.step;

import java.util.Optional;

public class LocalStepOutcome<Data> implements StepOutcome<Data> {
    private final boolean isSuccessful;
    private Optional<RuntimeException> localOutcome;

    public LocalStepOutcome(boolean isSuccessful, Optional<RuntimeException> localOutcome) {
        this.isSuccessful = isSuccessful;
        this.localOutcome = localOutcome;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }
}
