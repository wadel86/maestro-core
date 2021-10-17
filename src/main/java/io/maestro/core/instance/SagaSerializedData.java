package io.maestro.core.instance;

import io.maestro.core.util.JsonMapper;

public class SagaSerializedData {
    private String className;
    private String json;

    public SagaSerializedData(String className, String json) {
        this.className = className;
        this.json = json;
    }

    public static <Data> SagaSerializedData serializeSagaData(Data sagaData) {
        return new SagaSerializedData(sagaData.getClass().getName(), JsonMapper.toJson(sagaData));
    }

    public <Data> Data deserializeData() {
        Class clasz = null;
        try {
            clasz = SagaSerializedData.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
           throw new RuntimeException(e);
        }
        return (Data)JsonMapper.fromJson(json, clasz);
    }
}
