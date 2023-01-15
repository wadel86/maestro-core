package io.maestro.core.saga.definition.step;

import io.maestro.common.reply.Message;
import io.maestro.common.saga.instance.SagaInstance;

public interface RemoteStep<D> extends SagaStep<D> {
    StepOutcome<D> handleReply
            (SagaInstance sagaInstance, D data, Message message);
}
