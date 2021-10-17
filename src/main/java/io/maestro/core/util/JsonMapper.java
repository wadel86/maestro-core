package io.maestro.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object object) {
        try{
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

    public static <Data> Data fromJson(String json, Class<Data> targetType) {
        try{
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
