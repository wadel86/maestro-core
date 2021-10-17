package io.maestro.core.saga.definition.step;

import io.maestro.core.instance.SagaState;
import io.maestro.core.instance.SagaInstance;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LocalStep<Data> implements SagaStep<Data> {

    private Consumer<Data> localFunction;
    private Optional<Consumer<Data>> compensation;
    private Map<String, Consumer<Data>> exceptionHandlers;

    public LocalStep
            (Consumer<Data> localFunction,
             Optional<Consumer<Data>> compensation, Map<String,
                    Consumer<Data>> exceptionHandlers) {
        this.localFunction = localFunction;
        this.compensation = compensation;
        this.exceptionHandlers = exceptionHandlers;
    }

    @Override
    public StepOutcome<Data> execute(SagaInstance sagaInstance, Data data) {
        if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            //execute compensation if exists
            compensation.ifPresent(dataConsumer -> dataConsumer.accept(data));
            return new LocalStepOutcome<Data>(true, Optional.empty());
        }else{
            //execute action
            try{
                localFunction.accept(data);
                return new LocalStepOutcome<Data>(true, Optional.empty());
            }catch (RuntimeException exception){
                this.getExceptionHandler(exception.getClass().getName()).ifPresent((handler) -> handler.accept(data));
                return new LocalStepOutcome<Data>(false, Optional.of(exception));
            }
        }
    }

    private Optional<Consumer<Data>> getExceptionHandler(String exceptionType) {
        Consumer<Data> exceptionHandler = exceptionHandlers.get(exceptionType);
        if(exceptionHandler == null){
            return Optional.empty();
        }
        return Optional.of(exceptionHandler);
    }
}
