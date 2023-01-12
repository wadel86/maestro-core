package io.maestro.core;

import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaSerializedData;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.Saga;
import io.maestro.core.saga.definition.SagaDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SagaInstanceFactoryTests {

    @Mock
    private final SagaManagerFactory sagaManagerFactory
            = new SagaManagerFactory(null, null, null);

    @Mock
    private final SagaManager<TestSagaData> sagaManager
            = new SagaManagerImpl<>(null, null, null, null);

    @Test
    public void createSagaInstance_shouldCreateSagaInstance(){
        //given
        SagaInstanceFactory sagaInstanceFactory = new SagaInstanceFactory(sagaManagerFactory);
        Saga<TestSagaData> saga = new TestSaga();
        TestSagaData sagaData = new TestSagaData();
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(-1, SagaState.CREATED);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        when(sagaManagerFactory.createSagaManager(saga)).thenReturn(sagaManager);
        when(sagaManager.create(sagaData)).thenReturn(sagaInstance);
        SagaInstance resultInstance = sagaInstanceFactory.createSagaInstance(saga, sagaData);
        //then
        assertEquals(sagaInstance, resultInstance);
        verify(sagaManagerFactory, times(1)).createSagaManager(saga);
        verify(sagaManager, times(1)).create(sagaData);
    }

    @Test
    public void createSagaInstance_shouldNotCreateSagaManagerEverytimeASagaInstanceIsCreated(){
        //given
        SagaInstanceFactory sagaInstanceFactory = new SagaInstanceFactory(sagaManagerFactory);
        Saga<TestSagaData> saga = new TestSaga();
        TestSagaData sagaData = new TestSagaData();
        TestSagaData secondSagaData = new TestSagaData();
        SagaExecutionState sagaExecutionState
                = new SagaExecutionState(-1, SagaState.CREATED);
        SagaInstance sagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        SagaInstance secondSagaInstance
                = new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
        //when
        when(sagaManagerFactory.createSagaManager(saga)).thenReturn(sagaManager);
        when(sagaManager.create(any(TestSagaData.class))).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] == sagaData) {
                return sagaInstance;
            }
            return secondSagaInstance;
        });
        SagaInstance resultInstance = sagaInstanceFactory.createSagaInstance(saga, sagaData);
        SagaInstance secondResultInstance = sagaInstanceFactory.createSagaInstance(saga, secondSagaData);
        //then
        assertEquals(sagaInstance, resultInstance);
        assertEquals(secondSagaInstance, secondResultInstance);
        verify(sagaManagerFactory, times(1)).createSagaManager(saga);
        verify(sagaManager, times(2)).create(any(TestSagaData.class));
    }

    private static class TestSaga extends Saga<TestSagaData> {
        private final SagaSerializedData sagaSerializedData
                = new SagaSerializedData(TestSagaData.class.getName(), "test");
        private final SagaInstance sagaInstance
                = new SagaInstance
                ("id", "test-saga", new SagaExecutionState(3, SagaState.EXECUTING),
                 sagaSerializedData);
        private  SagaDefinition<TestSagaData> definition
                = step().invokeLocalParticipant(this::localParticipantAction)
                        .build();
        public TestSaga() {
            super.setDefinition(definition);
        }

        public void localParticipantAction(TestSagaData data){}
    }

    protected static class TestSagaData {}
}
