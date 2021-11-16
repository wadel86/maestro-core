package io.maestro.core.saga.definition.step;

import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaInstance;

public interface RemoteStep<Data> extends SagaStep<Data> {
    StepOutcome<Data> handleReply
            (SagaInstance sagaInstance, Data data, Message message);
}
