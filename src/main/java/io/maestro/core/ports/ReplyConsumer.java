package io.maestro.core.ports;

import io.maestro.core.reply.MessageHandler;

public interface ReplyConsumer {
    void subscribe(String channelId, MessageHandler messageHandler);
}
