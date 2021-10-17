package io.maestro.core.reply;

import java.util.Map;

public class Message {
    private String sagaType;
    private Map<String, String> headers;
    private String payload;

    public String getSagaType() {
        return sagaType;
    }

    public String getHeader(String header) {
        return headers.get(header);
    }

    public String getPayload() {
        return payload;
    }
}
