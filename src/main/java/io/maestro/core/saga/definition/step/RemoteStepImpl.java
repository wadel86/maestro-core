package io.maestro.core.saga.definition.step;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.reply.Message;
import io.maestro.common.reply.MessageHeaders;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.common.util.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A step carried out by a remote participant: the saga sends it a command and parks
 * until it replies.
 *
 * <p>Undoing such a step usually means asking the same participant to undo its work,
 * which is another command and another reply, so a remote step can be given a
 * {@code remoteCompensation}. When it has one, compensation is a full round trip, exactly
 * like the forward direction. A step whose undo is purely local business (releasing a
 * reservation this service holds, say) can use the plain {@code compensation} instead,
 * which runs in place and completes immediately.
 */
public class RemoteStepImpl<D> implements RemoteStep<D> {

    private Function<D, CommandWithDestination> remoteInvocation = null;
    private Map<String, BiConsumer<D, Object>> replyHandlers;
    private Optional<Consumer<D>> compensation = Optional.empty();
    private Optional<Function<D, CommandWithDestination>> remoteCompensation = Optional.empty();

    public RemoteStepImpl() {
    }

    public RemoteStepImpl
            (Function<D, CommandWithDestination> remoteInvocation,
             Optional<Consumer<D>> compensation,
             Map<String, BiConsumer<D, Object>> replyHandlers) {
        this(remoteInvocation, compensation, Optional.empty(), replyHandlers);
    }

    public RemoteStepImpl
            (Function<D, CommandWithDestination> remoteInvocation,
             Optional<Consumer<D>> compensation,
             Optional<Function<D, CommandWithDestination>> remoteCompensation,
             Map<String, BiConsumer<D, Object>> replyHandlers) {
        this.remoteInvocation = remoteInvocation;
        this.replyHandlers = replyHandlers;
        this.compensation = compensation;
        this.remoteCompensation = remoteCompensation;
    }

    @Override
    public StepOutcome<D> execute(SagaInstance sagaInstance, D data) {
        if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            if(remoteCompensation.isPresent()){
                //undoing this step is the participant's job: send the compensating
                //command and park, the same way the forward direction does.
                return RemoteStepOutcome.dispatch(remoteCompensation.get().apply(data));
            }
            //execute compensation if exists
            compensation.ifPresent(dataConsumer -> dataConsumer.accept(data));
            return new LocalStepOutcome<>(true, Optional.empty());
        }else{
            //producing the command is itself the step's success; the participant's own
            //verdict arrives later, through handleReply.
            CommandWithDestination commandToSend = this.remoteInvocation.apply(data);
            return RemoteStepOutcome.dispatch(commandToSend);
        }
    }

    @Override
    public StepOutcome<D> handleReply(
            SagaInstance sagaInstance, D data, Message message) {
        String replyType = message.getHeader(MessageHeaders.REPLY_TYPE);
        String replyOutcome = message.getHeader(MessageHeaders.REPLY_OUTCOME);
        this.getReplyHandler(replyType).ifPresent(handler -> {
            this.invokeReplyHandler(handler, data, replyType, message);
        });
        return RemoteStepOutcome.replied(MessageHeaders.SUCCESS.equalsIgnoreCase(replyOutcome));
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
