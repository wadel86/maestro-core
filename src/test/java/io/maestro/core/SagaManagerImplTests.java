package io.maestro.core;

import io.maestro.common.port.CommandProducer;
import io.maestro.common.port.ReplyConsumer;
import io.maestro.common.port.SagaDataGateway;
import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaSerializedData;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.dsl.SagaDefinitionDsl;
import io.maestro.core.saga.Saga;
import io.maestro.core.saga.definition.step.LocalStepOutcome;
import io.maestro.core.saga.definition.step.SagaStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaManagerImplTests implements SagaDefinitionDsl<SagaManagerImplTests.TestSagaData> {

    @Mock
    SagaDataGateway sagaDataGateway;
    @Mock
    CommandProducer commandProducer;
    @Mock
    ReplyConsumer replyConsumer;
    @Mock
    Saga<TestSagaData> saga;
    @Mock
    SagaInstance sagaInstance;
    @Mock
    SagaExecutionState sagaExecutionState;
    @Mock
    SagaStep<TestSagaData> firstStep;

    //@Test
    void create_ShouldCreatesSagaAndExecuteStartingSteps(){
        //given
        TestSagaData testData = new TestSagaData();
        SagaSerializedData sagaSerializedData = new SagaSerializedData("Test", "test");
        try (MockedStatic<SagaSerializedData>
                     serializer = Mockito.mockStatic(SagaSerializedData.class)){
            serializer.when(() -> SagaSerializedData.serializeSagaData(testData))
                      .thenReturn(sagaSerializedData);
        }
        when(saga.getSagaType()).thenReturn("TEST");
        when(sagaDataGateway.saveSaga(any(SagaInstance.class)))
                .thenAnswer((Answer<SagaInstance>) invocationOnMock -> sagaInstance);
        when(firstStep.execute(any(SagaInstance.class), any(TestSagaData.class)))
                .thenReturn(new LocalStepOutcome<>(true, Optional.empty()));
        List<SagaStep<TestSagaData>> startingSteps
                = Collections.singletonList(firstStep);
        doNothing().when(sagaInstance).start();
        when(saga.getNextSteps(any(SagaInstance.class))).thenReturn(startingSteps);
        when(sagaInstance.getSagaExecutionState()).thenReturn(sagaExecutionState);
        when(sagaExecutionState.getState()).thenReturn(SagaState.EXECUTING);
        doNothing().when(sagaInstance).stepUp();
        doNothing().when(sagaInstance).setSerializedData(any(SagaSerializedData.class));
        when(sagaExecutionState.getPointer()).thenReturn(1);
        when(saga.getSagaSize()).thenReturn(2);
        SagaManager<TestSagaData> sagaManager
                = new SagaManagerImpl<>(sagaDataGateway, commandProducer, replyConsumer, saga);
        //when
        SagaInstance sagaInstance = sagaManager.create(testData);
        //then
        assertTrue(true);
    }

    protected static class TestSagaData {}
}
