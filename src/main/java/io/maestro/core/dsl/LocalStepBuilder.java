package io.maestro.core.dsl;

import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.LocalStep;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LocalStepBuilder<Data> {

    private SagaDefinitionBuilder<Data> parent;
    private Consumer<Data> localFunction;
    private Optional<Consumer<Data>> compensation = Optional.empty();
    private Map<String, Consumer<Data>> exceptionHandlers = new HashMap<>();

    public LocalStepBuilder(SagaDefinitionBuilder<Data> parent, Consumer<Data> localFunction){
        this.parent = parent;
        this.localFunction = localFunction;
    }

    public LocalStepBuilder<Data> withCompensation(Consumer<Data> localFunction){
        this.compensation = Optional.of(localFunction);
        return this;
    }

    public <T> LocalStepBuilder<Data> onException(Class<T> exceptionType, Consumer<Data> exceptionHandler) {
        this.exceptionHandlers.put(exceptionType.getName(), exceptionHandler);
        return this;
    }

    public StepBuilder<Data> step() {
        this.parent.addStep(new LocalStep<>(localFunction, compensation, exceptionHandlers));
        return new StepBuilder<>(this.parent);
    }

    public SagaDefinition<Data> build() {
        this.parent.addStep(new LocalStep<>(localFunction, compensation, exceptionHandlers));
        return this.parent.build();
    }
}
