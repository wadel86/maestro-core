package io.maestro.core.saga.definition.step;

import io.maestro.core.instance.SagaInstance;
import io.maestro.core.reply.Message;

public interface RemoteStep<Data> extends SagaStep<Data> {
    StepOutcome<Data> handleReply
            (SagaInstance sagaInstance, Data data, Message message);
}
