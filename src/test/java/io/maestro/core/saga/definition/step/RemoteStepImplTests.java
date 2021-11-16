package io.maestro.core.saga.definition.step;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.common.util.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RemoteStepImplTests {

    private boolean compensationExecuted = false;
    private boolean replyHandlerExecuted = false;
    private final CommandWithDestination commandWithDestination = new CommandWithDestination();

    @Test
    public void executeStep_whenSagaIsExecuting_ShouldExecuteAction(){
        //given
        RemoteStepImpl<TestSagaData> remoteStep
                = new RemoteStepImpl<>(this::remoteActionToExecute,
                                       Optional.empty(),
                                       new HashMap<>());
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.EXECUTING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        RemoteStepOutcome<TestSagaData> stepOutcome
                = (RemoteStepOutcome<TestSagaData>) remoteStep.execute(sagaInstance, new TestSagaData());
        //then
        assertEquals(this.commandWithDestination, stepOutcome.getCommandToSend());
    }

    @Test
    public void executeStep_whenSagaIsCompensating_ShouldExecuteCompensation(){
        //given
        RemoteStepImpl<TestSagaData> remoteStep
                = new RemoteStepImpl<>(this::remoteActionToExecute,
                                       Optional.of(this::remoteCompensationToExecute),
                                       new HashMap<>());
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.COMPENSATING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        StepOutcome<TestSagaData> stepOutcome = remoteStep.execute(sagaInstance, new TestSagaData());
        //then
        assertTrue(stepOutcome.isSuccessful());
        assertTrue(compensationExecuted);
    }

    @Test
    public void handleReply_whenReplyIsFailure_ShouldReturnCallReplyHandlerNadReturnsAFailureOutcome(){
        //given
        Map<String, BiConsumer<TestSagaData, Object>> replyHandlers = new HashMap<>();
        BiConsumer<TestSagaData, Reply> replyHandler = this::replyHandler;
        replyHandlers.put(Reply.class.getName(), (data, rawReply) -> replyHandler.accept(data, (Reply)rawReply));
        RemoteStepImpl<TestSagaData> remoteStep
                = new RemoteStepImpl<>(this::remoteActionToExecute,
                                       Optional.empty(),
                                       replyHandlers);
        SagaExecutionState sagaExecutionState = new SagaExecutionState(1, SagaState.EXECUTING);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        Map<String, String> messageHeaders = new HashMap<>();
        messageHeaders.put("reply-outcome", "failure");
        messageHeaders.put("reply-type", "io.maestro.core.saga.definition.step.RemoteStepImplTests$Reply");
        Message replyMessage = new Message("saga-type", messageHeaders, "payload");
        //when
        try (MockedStatic<JsonMapper> utilities = Mockito.mockStatic(JsonMapper.class)) {
            utilities.when(() -> JsonMapper.fromJson("payload", TestSagaData.class))
                     .thenReturn(new TestSagaData());
            StepOutcome<TestSagaData> stepOutcome = remoteStep.handleReply(sagaInstance, new TestSagaData(), replyMessage);
            //then
            assertFalse(stepOutcome.isSuccessful());
            assertTrue(replyHandlerExecuted);
        }
    }

    protected static class TestSagaData {}

    private CommandWithDestination remoteActionToExecute(TestSagaData testSagaData){
        return this.commandWithDestination;
    }

    public void remoteCompensationToExecute(TestSagaData testSagaData){
        this.compensationExecuted = true;
    }

    public void replyHandler(TestSagaData testSagaData, Reply reply){
        this.replyHandlerExecuted = true;
    }

    private static class Reply {

    }

}
