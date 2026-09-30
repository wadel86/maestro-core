package io.maestro.core;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.common.exception.BadSagaTypeException;
import io.maestro.common.exception.InconsistentSagaStateException;
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

import java.util.List;

/**
 * Drives instances of one saga type: starts them, runs their steps, routes participant
 * replies back to them, and unwinds them when a step fails.
 *
 * <p>Commands are never published from here. They are handed to
 * {@link SagaDataGateway#saveSagaAndSendCommand} so that dispatching a command and
 * recording that it was dispatched happen atomically; publication is the adapter's job.
 *
 * <p>A manager is not usable until {@link #subscribeToReplyChannel()} has been called,
 * which {@link SagaManagerFactory} does as part of building one.
 */
public class SagaManagerImpl<D> implements SagaManager<D> {

    private final SagaDataGateway sagaDataGateway;
    private final ReplyConsumer replyConsumer;
    private final Saga<D> saga;

    public SagaManagerImpl
            (SagaDataGateway sagaDataGateway, ReplyConsumer replyConsumer, Saga<D> saga) {
        if (saga.getSagaType() == null || saga.getSagaType().isBlank()) {
            throw new BadSagaTypeException
                    ("A saga must declare a saga type before it can be managed");
        }
        this.sagaDataGateway = sagaDataGateway;
        this.replyConsumer = replyConsumer;
        this.saga = saga;
    }

    @Override
    public SagaInstance create
            (D sagaData) throws BadSagaTypeException {
        SagaInstance sagaInstance
                = new SagaInstance
                (null,
                 this.saga.getSagaType(),
                 SagaExecutionState.initialize(),
                 SagaSerializedData.serializeSagaData(sagaData));
        sagaInstance = sagaDataGateway.saveSaga(sagaInstance);
        sagaInstance.start();
        List<SagaStep<D>> startingSteps = saga.getNextSteps(sagaInstance);
        processSteps(sagaInstance, sagaData, startingSteps);
        return sagaInstance;
    }

    /**
     * Starts listening for this saga type's replies. Called once, by the factory, after
     * construction &mdash; deliberately not from the constructor, which would publish a
     * half-built {@code this} to the messaging adapter.
     */
    public void subscribeToReplyChannel
            () {
        this.replyConsumer.subscribe(getSagaReplyChannel(), this::handleReply);
    }

    private String getSagaReplyChannel
            () {
        return this.saga.getSagaType() + "-reply-channel";
    }

    private void handleReply
            (Message message) {
        if(!this.saga.getSagaType().equalsIgnoreCase(message.getSagaType())) {
            return;
        }
        String sagaId = message.getHeader("Saga-ID");
        String sagaType = message.getHeader("Saga-Type");
        SagaInstance sagaInstance = sagaDataGateway.findSaga(sagaId, sagaType);
        D sagaData = sagaInstance.getSerializedData().deserializeSagaData();
        StepOutcome<D> stepOutcome = saga.handleReply(sagaInstance, sagaData, message);
        List<SagaStep<D>> stepsToExecute;
        if(stepOutcome.isSuccessful()){
            //the participant is done, so the step this saga was parked on is complete.
            sagaInstance.stepUp();
            stepsToExecute = this.saga.getNextSteps(sagaInstance);
        }else{
            sagaInstance.reverseToCompensationState();
            stepsToExecute = this.saga.getStepsToCompensate(sagaInstance);
        }
        processSteps(sagaInstance, sagaData, stepsToExecute);
    }

    private void processSteps
            (SagaInstance sagaInstance, D data, List<SagaStep<D>> stepsToProcess) {
        for (SagaStep<D> sagaStep : stepsToProcess) {
            StepOutcome<D> stepOutcome = sagaStep.execute(sagaInstance, data);
            if(!stepOutcome.isSuccessful()){
                if(isCompensating(sagaInstance)){
                    //If compensation fails, nothing to do, maybe retry.
                    throw new InconsistentSagaStateException
                            ("Compensation failed, nothing to do");
                }
                //stop step execution, reverse pointer direction and undo what has completed.
                sagaInstance.reverseToCompensationState();
                processSteps(sagaInstance, data, this.saga.getStepsToCompensate(sagaInstance));
                return;
            }
            CommandWithDestination command = null;
            if(isCompensating(sagaInstance)){
                //one more step undone.
                sagaInstance.stepDown();
            }else if(stepOutcome instanceof RemoteStepOutcome){
                //hand the command to the gateway and park the saga on this step:
                //the pointer only advances once the participant replies.
                command = ((RemoteStepOutcome<D>)stepOutcome).getCommandToSend();
            }else{
                sagaInstance.stepUp();
            }
            //update saga instance data.
            sagaInstance.setSerializedData(
                    SagaSerializedData.serializeSagaData(data));
            if(command != null){
                this.sagaDataGateway.saveSagaAndSendCommand(sagaInstance, command);
            }else{
                this.sagaDataGateway.saveSaga(sagaInstance);
            }
        }
        terminateIfFinished(sagaInstance);
    }

    private void terminateIfFinished
            (SagaInstance sagaInstance) {
        SagaExecutionState executionState = sagaInstance.getSagaExecutionState();
        if(isCompensating(sagaInstance)){
            if(executionState.getPointer() != -1){
                //when compensating, every step executed must be undone,
                //therefore, the saga instance pointer must point back before the first step.
                //throw exception if is not the case.
                throw new InconsistentSagaStateException
                        ("Saga terminated with failure status without compensating all steps");
            }
        }else if(executionState.getPointer() != saga.getSagaSize()){
            //still steps to run, or waiting on a participant's reply.
            return;
        }
        //saga is ended, execute some ending actions.
        sagaInstance.terminate();
        this.sagaDataGateway.saveSaga(sagaInstance);
    }

    private boolean isCompensating
            (SagaInstance sagaInstance) {
        return SagaState.COMPENSATING.equals(sagaInstance.getSagaExecutionState().getState());
    }

}
