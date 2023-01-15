package io.maestro.core.saga.definition.step;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.common.util.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RemoteStepImpl<D> implements RemoteStep<D> {

    private Function<D, CommandWithDestination> remoteInvocation = null;
    private Map<String, BiConsumer<D, Object>> replyHandlers;
    private Optional<Consumer<D>> compensation;

    public RemoteStepImpl() {
    }

    public RemoteStepImpl
            (Function<D, CommandWithDestination> remoteInvocation,
             Optional<Consumer<D>> compensation,
             Map<String, BiConsumer<D, Object>> replyHandlers) {
        this.remoteInvocation = remoteInvocation;
        this.replyHandlers = replyHandlers;
        this.compensation = compensation;
    }

    @Override
    public StepOutcome<D> execute(SagaInstance sagaInstance, D data) {
        if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            //execute compensation if exists
            compensation.ifPresent(dataConsumer -> dataConsumer.accept(data));
            return new LocalStepOutcome<>(true, Optional.empty());
        }else{
            CommandWithDestination commandToSend = this.remoteInvocation.apply(data);
            return new RemoteStepOutcome<>(commandToSend);
        }
    }

    @Override
    public StepOutcome<D> handleReply(
            SagaInstance sagaInstance, D data, Message message) {
        String replyType = message.getHeader("reply-type");
        String replyOutcome = message.getHeader("reply-outcome");
        this.getReplyHandler(replyType).ifPresent(handler -> {
            this.invokeReplyHandler(handler, data, replyType, message);
        });
        if("success".equalsIgnoreCase(replyOutcome)){
            return new RemoteStepOutcome<>(true);
        }else{
            return new RemoteStepOutcome<>(false);
        }
    }

    private Optional<BiConsumer<D, Object>> getReplyHandler(String replyType) {
        BiConsumer<D, Object> replyHandler = replyHandlers.get(replyType);
        if(replyHandler == null){
            return Optional.empty();
        }
        return Optional.of(replyHandler);
    }

    private void invokeReplyHandler(BiConsumer<D, Object> handler, D data, String replyType, Message message){
        Class m;
        try{
            m = Class.forName(replyType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found", e);
        }
        Object reply = JsonMapper.fromJson(message.getPayload(), m);
        handler.accept(data, reply);
    }

}