package io.maestro.core.instance;

import io.maestro.core.util.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class SagaSerializedDataTests {

    @Test
    public void serializeSagaData_shouldSetUpTheRightAttributes(){
        //given
        TestSagaData sagaData = new TestSagaData();
        //when
        try (MockedStatic<JsonMapper> utilities = Mockito.mockStatic(JsonMapper.class)) {
            utilities.when(() -> JsonMapper.toJson(sagaData))
                     .thenReturn("sagaDataJson");
            SagaSerializedData sagaSerializedData = SagaSerializedData.serializeSagaData(sagaData);
            //then
            assertNotNull(sagaSerializedData);
            assertEquals("io.maestro.core.instance.SagaSerializedDataTests$TestSagaData",
                         sagaSerializedData.getClassName());
            assertEquals("sagaDataJson", sagaSerializedData.getJson());
        }
    }

    @Test
    public void deserializeSagaData_shouldReturnsRightSagaDataObject(){
        //given
        TestSagaData sagaData = new TestSagaData();
        SagaSerializedData sagaSerializedData
                = new SagaSerializedData("io.maestro.core.instance.SagaSerializedDataTests$TestSagaData",
                                         "sagaDataJson");
        //when
        try (MockedStatic<JsonMapper> utilities = Mockito.mockStatic(JsonMapper.class)) {
            utilities.when(() -> JsonMapper.fromJson("sagaDataJson", TestSagaData.class))
                     .thenReturn(sagaData);
            TestSagaData deserializedSagaData = sagaSerializedData.deserializeSagaData();
            //then
            assertNotNull(deserializedSagaData);
            assertEquals(sagaData, deserializedSagaData);
        }
    }

    protected static class TestSagaData {}
}
