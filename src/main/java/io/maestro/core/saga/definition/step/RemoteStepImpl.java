package io.maestro.core.saga.definition.step;

import io.maestro.core.command.CommandWithDestination;
import io.maestro.core.instance.SagaState;
import io.maestro.core.instance.SagaInstance;
import io.maestro.core.reply.Message;
import io.maestro.core.util.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RemoteStepImpl<Data> implements RemoteStep<Data> {

    private final Function<Data, CommandWithDestination> remoteInvocation;
    private Map<String, BiConsumer<Data, Object>> replyHandlers;
    private Optional<Consumer<Data>> compensation;

    public RemoteStepImpl
            (Function<Data, CommandWithDestination> remoteInvocation, Map<String, BiConsumer<Data, Object>> replyHandlers,
                      Optional<Consumer<Data>> compensation) {
        this.remoteInvocation = remoteInvocation;
        this.replyHandlers = replyHandlers;
        this.compensation = compensation;
    }

    @Override
    public StepOutcome<Data> execute(SagaInstance sagaInstance, Data data) {
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
    public StepOutcome<Data> handleReply(
            SagaInstance sagaInstance, Data data, Message message) {
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

    private Optional<BiConsumer<Data, Object>> getReplyHandler(String replyType) {
        BiConsumer<Data, Object> replyHandler = replyHandlers.get(replyType);
        if(replyHandler == null){
            return Optional.empty();
        }
        return Optional.of(replyHandler);
    }

    private void invokeReplyHandler(BiConsumer<Data, Object> handler, Data data, String replyType, Message message){
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