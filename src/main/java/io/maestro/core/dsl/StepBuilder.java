package io.maestro.core.dsl;

import io.maestro.common.command.CommandWithDestination;

import java.util.function.Consumer;
import java.util.function.Function;

public class StepBuilder<D> {

    private SagaDefinitionBuilder<D> parent;

    public StepBuilder(SagaDefinitionBuilder<D> parentBuilder) {
        this.parent = parentBuilder;
    }

    public LocalStepBuilder<D> invokeLocalParticipant(Consumer<D> localFunction) {
        return new LocalStepBuilder<>(parent, localFunction);
    }

    public RemoteStepBuilder<D> invokeRemoteParticipant(Function<D, CommandWithDestination> remoteInvocation) {
        return new RemoteStepBuilder<>(parent, remoteInvocation);
    }
}
