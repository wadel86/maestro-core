package io.maestro.core;

import io.maestro.common.saga.instance.SagaExecutionState;
import io.maestro.common.saga.instance.SagaInstance;
import io.maestro.common.saga.instance.SagaState;
import io.maestro.core.saga.Saga;
import io.maestro.core.saga.definition.SagaDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaInstanceFactoryTests {

    @Mock
    private SagaManagerFactory sagaManagerFactory;

    @Mock
    private SagaManager<TestSagaData> sagaManager;

    private final SagaExecutionState sagaExecutionState
            = new SagaExecutionState(-1, SagaState.CREATED);

    private SagaInstance anInstance() {
        return new SagaInstance("saga-id", "saga-type", sagaExecutionState, null);
    }

    @Test
    void constructor_shouldRegisterAManagerForEverySagaUpFront(){
        //given
        Saga<TestSagaData> firstSaga = new TestSaga();
        Saga<TestSagaData> secondSaga = new TestSaga();
        //when
        new SagaInstanceFactory(sagaManagerFactory, List.of(firstSaga, secondSaga));
        //then
        //managers subscribe to their reply channel when created, so they have to exist
        //before any saga runs, not on first use
        verify(sagaManagerFactory, times(1)).createSagaManager(firstSaga);
        verify(sagaManagerFactory, times(1)).createSagaManager(secondSaga);
    }

    @Test
    void createSagaInstance_shouldCreateSagaInstance(){
        //given
        Saga<TestSagaData> saga = new TestSaga();
        TestSagaData sagaData = new TestSagaData();
        SagaInstance sagaInstance = anInstance();
        when(sagaManagerFactory.createSagaManager(saga)).thenReturn(sagaManager);
        when(sagaManager.create(sagaData)).thenReturn(sagaInstance);
        SagaInstanceFactory sagaInstanceFactory
                = new SagaInstanceFactory(sagaManagerFactory, List.of(saga));
        //when
        SagaInstance resultInstance = sagaInstanceFactory.createSagaInstance(saga, sagaData);
        //then
        assertEquals(sagaInstance, resultInstance);
        verify(sagaManagerFactory, times(1)).createSagaManager(saga);
        verify(sagaManager, times(1)).create(sagaData);
    }

    @Test
    void createSagaInstance_shouldNotCreateSagaManagerEverytimeASagaInstanceIsCreated(){
        //given
        Saga<TestSagaData> saga = new TestSaga();
        TestSagaData sagaData = new TestSagaData();
        TestSagaData secondSagaData = new TestSagaData();
        SagaInstance sagaInstance = anInstance();
        SagaInstance secondSagaInstance = anInstance();
        when(sagaManagerFactory.createSagaManager(saga)).thenReturn(sagaManager);
        when(sagaManager.create(any(TestSagaData.class))).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] == sagaData) {
                return sagaInstance;
            }
            return secondSagaInstance;
        });
        SagaInstanceFactory sagaInstanceFactory
                = new SagaInstanceFactory(sagaManagerFactory, List.of(saga));
        //when
        SagaInstance resultInstance = sagaInstanceFactory.createSagaInstance(saga, sagaData);
        SagaInstance secondResultInstance = sagaInstanceFactory.createSagaInstance(saga, secondSagaData);
        //then
        assertEquals(sagaInstance, resultInstance);
        assertEquals(secondSagaInstance, secondResultInstance);
        verify(sagaManagerFactory, times(1)).createSagaManager(saga);
        verify(sagaManager, times(2)).create(any(TestSagaData.class));
    }

    @Test
    @SuppressWarnings("deprecation")
    void createSagaInstance_whenTheSagaWasNotRegisteredUpFront_shouldRegisterItOnFirstUse(){
        //given
        Saga<TestSagaData> saga = new TestSaga();
        TestSagaData sagaData = new TestSagaData();
        SagaInstance sagaInstance = anInstance();
        when(sagaManagerFactory.createSagaManager(saga)).thenReturn(sagaManager);
        when(sagaManager.create(sagaData)).thenReturn(sagaInstance);
        SagaInstanceFactory sagaInstanceFactory = new SagaInstanceFactory(sagaManagerFactory);
        //when
        SagaInstance resultInstance = sagaInstanceFactory.createSagaInstance(saga, sagaData);
        //then
        assertEquals(sagaInstance, resultInstance);
        verify(sagaManagerFactory, times(1)).createSagaManager(saga);
    }

    private static class TestSaga extends Saga<TestSagaData> {
        private final SagaDefinition<TestSagaData> definition
                = step().invokeLocalParticipant(this::localParticipantAction)
                        .build();
        public TestSaga() {
            super.setSagaType("test-saga");
            super.setDefinition(definition);
        }

        public void localParticipantAction(TestSagaData data){}
    }

    protected static class TestSagaData {}
}
