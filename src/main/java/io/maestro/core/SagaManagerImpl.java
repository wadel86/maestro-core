package io.maestro.core;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.exception.BadSagaTypeException;
import io.maestro.common.exception.InconsistentSagaStateException;
import io.maestro.common.port.CommandProducer;
import io.maestro.common.port.ReplyConsumer;
import io.maestro.common.port.SagaDataGateway;
import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaSerializedData;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.Saga;

import io.maestro.core.saga.definition.step.RemoteStepOutcome;
import io.maestro.core.saga.definition.step.SagaStep;
import io.maestro.core.saga.definition.step.StepOutcome;

import javax.annotation.PostConstruct;
import java.util.List;

public class SagaManagerImpl<D> implements SagaManager<D> {
    private final SagaDataGateway sagaDataGateway;
    private final CommandProducer commandProducer;
    private final ReplyConsumer replyConsumer;
    private final Saga<D> saga;

    public SagaManagerImpl
            (SagaDataGateway sagaDataGateway, CommandProducer commandProducer,
             ReplyConsumer replyConsumer, Saga<D> saga) {
        this.sagaDataGateway = sagaDataGateway;
        this.commandProducer = commandProducer;
        this.replyConsumer = replyConsumer;
        this.saga = saga;
    }

    @Override
    public SagaInstance create
            (D sagaData) throws BadSagaTypeException {
        SagaInstance sagaInstance
                = new SagaInstance
                ((String)null,
                 this.saga.getSagaType(),
                 SagaExecutionState.initialize(),
                 SagaSerializedData.serializeSagaData(sagaData));
        sagaInstance = sagaDataGateway.saveSaga(sagaInstance);
        sagaInstance.start();
        List<SagaStep<D>> startingSteps = saga.getNextSteps(sagaInstance);
        processSteps(sagaInstance.getId(), sagaInstance, sagaData, startingSteps);
        return sagaInstance;
    }

    @PostConstruct
    private void subscribeToReplyChannel
            () {
        this.replyConsumer.subscribe(getSagaReplyChannel(), this::handleReply);
    }

    private String getSagaReplyChannel
            () {
        return this.saga.getSagaType() + "-reply-channel";
    }

    private void handleReply
            (Message message) {
        if(this.saga.getSagaType().equalsIgnoreCase(message.getSagaType())) {
            String sagaId = message.getHeader("Saga-ID");
            String sagaType = message.getHeader("Saga-Type");
            SagaInstance sagaInstance = sagaDataGateway.findSaga(sagaId, sagaType);
            D sagaData = sagaInstance.getSerializedData().deserializeSagaData();
            StepOutcome<D> stepOutcome = saga.handleReply(sagaInstance, sagaData, message);
            List<SagaStep<D>> stepsToExecute;
            if(stepOutcome.isSuccessful()){
                sagaInstance.getSagaExecutionState().stepUp();
                stepsToExecute = this.saga.getNextSteps(sagaInstance);
            }else{
                sagaInstance.reverseToCompensationState();
                stepsToExecute = this.saga.getStepsToCompensate(sagaInstance);
            }
            processSteps(sagaInstance.getId(), sagaInstance, sagaData, stepsToExecute);
        }
    }

    private void processSteps
            (String sagaId, SagaInstance sagaInstance, D data, List<SagaStep<D>> stepsToProcess) {
        for (SagaStep<D> sagaStep : stepsToProcess) {
            CommandWithDestination command = null;
            StepOutcome<D> stepOutcome = sagaStep.execute(sagaInstance, data);
            if(stepOutcome.isSuccessful()){
                if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
                    //if compensating, update saga instance state
                    sagaInstance.stepDown();
                }else{
                    if(stepOutcome instanceof RemoteStepOutcome){
                        //send command.
                        RemoteStepOutcome<D> remoteStepOutcome = (RemoteStepOutcome<D>)stepOutcome;
                        command = remoteStepOutcome.getCommandToSend();
                        this.commandProducer.sendCommand
                                (this.saga.getSagaType(), sagaInstance.getId(), remoteStepOutcome.getCommandToSend());
                    }else{
                        //update saga instance state.
                        sagaInstance.stepUp();
                    }
                }
                //update saga instance data.
                sagaInstance.setSerializedData(
                        SagaSerializedData.serializeSagaData(data));
            }else{
                if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
                    //If compensation fails, nothing to do, maybe retry.
                    throw new InconsistentSagaStateException
                            ("Compensation failed, nothing to do");
                }else{
                    //stop step execution, reverse pointer direction and get compensation steps.
                    sagaInstance.reverseToCompensationState();
                    processSteps(sagaId, sagaInstance, data, this.saga.getStepsToCompensate(sagaInstance));
                    return;
                }
            }
            if(command != null){
                this.sagaDataGateway.saveSagaAndSendCommand(sagaInstance, command);
            }else{
                this.sagaDataGateway.saveSaga(sagaInstance);
            }
        }
        if(SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState())){
            if(sagaInstance.getSagaExecutionState().getPointer() != -1){
                //when compensating, every step executed must be undone,
                //therefore, the saga instance pointer must point to the first step.
                //throw exception if is not the case.
                throw new InconsistentSagaStateException
                        ("Saga terminated with failure status without compensating all steps");
            }
        }else{
            if(sagaInstance.getSagaExecutionState().getPointer() == saga.getSagaSize()){
                //saga is ended, execute some ending actions.
                sagaInstance.terminate();
            }
        }
        this.sagaDataGateway.saveSaga(sagaInstance);
    }

}