package io.maestro.core.dsl;

import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.LocalStep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LocalStepBuilder<D> {

    private SagaDefinitionBuilder<D> parent;
    private Consumer<D> localFunction;
    private Optional<Consumer<D>> compensation = Optional.empty();
    private Map<String, Consumer<D>> exceptionHandlers = new HashMap<>();

    public LocalStepBuilder(SagaDefinitionBuilder<D> parent, Consumer<D> localFunction){
        this.parent = parent;
        this.localFunction = localFunction;
    }

    public LocalStepBuilder<D> withCompensation(Consumer<D> localFunction){
        this.compensation = Optional.of(localFunction);
        return this;
    }

    public LocalStepBuilder<D> onExceptions(List<Class<?>> exceptionTypes, Consumer<D> exceptionHandler) {
        for(Class<?> exceptionType : exceptionTypes){
            addExceptionHandler(exceptionType, exceptionHandler);
        }
        return this;
    }

    public <T> LocalStepBuilder<D> onException(Class<T> exceptionType, Consumer<D> exceptionHandler) {
        addExceptionHandler(exceptionType, exceptionHandler);
        return this;
    }

    private <T> void addExceptionHandler(Class<T> exceptionType, Consumer<D> exceptionHandler) {
        this.exceptionHandlers.put(exceptionType.getName(), exceptionHandler);
    }

    public StepBuilder<D> step() {
        this.parent.addStep(new LocalStep<>(localFunction, compensation, exceptionHandlers));
        return new StepBuilder<>(this.parent);
    }

    public SagaDefinition<D> build() {
        this.parent.addStep(new LocalStep<>(localFunction, compensation, exceptionHandlers));
        return this.parent.build();
    }
}
