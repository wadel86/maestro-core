package io.maestro.core;

import io.maestro.core.saga.Saga;
import io.maestro.core.ports.CommandProducer;
import io.maestro.core.ports.ReplyConsumer;
import io.maestro.core.ports.SagaDataGateway;

public class SagaManagerFactory {
    private final SagaDataGateway sagaDataGateway;
    private final CommandProducer commandProducer;
    private final ReplyConsumer replyConsumer;

    public SagaManagerFactory(SagaDataGateway sagaDataGateway, CommandProducer commandProducer,
                              ReplyConsumer replyConsumer) {
        this.sagaDataGateway = sagaDataGateway;
        this.commandProducer = commandProducer;
        this.replyConsumer = replyConsumer;
    }

    public <Data> io.maestro.core.SagaManager<Data> createSagaManager(Saga<Data> saga) {
        return new io.maestro.core.SagaManagerImpl<>(sagaDataGateway, commandProducer, replyConsumer, saga);
    }
}
