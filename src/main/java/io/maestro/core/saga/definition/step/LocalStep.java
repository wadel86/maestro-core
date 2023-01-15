package io.maestro.core.saga.definition.step;

import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LocalStep<D> implements SagaStep<D> {

    private Consumer<D> localFunction;
    private Optional<Consumer<D>> compensation;
    private Map<String, Consumer<D>> exceptionHandlers;

    public LocalStep() {
    }

    public LocalStep
            (Consumer<D> localFunction,
             Optional<Consumer<D>> compensation,
             Map<String, Consumer<D>> exceptionHandlers) {
        this.localFunction = localFunction;
        this.compensation = compensation;
        this.exceptionHandlers = exceptionHandlers;
    }

    @Override
    public StepOutcome<D> execute(SagaInstance sagaInstance, D d) {
        if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            //execute compensation if exists
            compensation.ifPresent(dataConsumer -> dataConsumer.accept(d));
            return new LocalStepOutcome<>(true, Optional.empty());
        }else{
            //execute action
            try{
                localFunction.accept(d);
                return new LocalStepOutcome<>(true, Optional.empty());
            }catch (RuntimeException exception){
                this.getExceptionHandler(exception.getClass().getName()).ifPresent(handler -> handler.accept(d));
                return new LocalStepOutcome<>(false, Optional.of(exception));
            }
        }
    }

    private Optional<Consumer<D>> getExceptionHandler(String exceptionType) {
        Consumer<D> exceptionHandler = exceptionHandlers.get(exceptionType);
        if(exceptionHandler == null){
            return Optional.empty();
        }
        return Optional.of(exceptionHandler);
    }
}
