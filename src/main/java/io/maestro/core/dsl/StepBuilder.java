package io.maestro.core.dsl;

import io.maestro.core.command.CommandWithDestination;

import java.util.function.Consumer;
import java.util.function.Function;

public class StepBuilder<Data> {

    private SagaDefinitionBuilder<Data> parent;

    public StepBuilder(SagaDefinitionBuilder<Data> parentBuilder) {
        this.parent = parentBuilder;
    }

    public LocalStepBuilder<Data> invokeLocalParticipant(Consumer<Data> localFunction) {
        return new LocalStepBuilder<>(parent, localFunction);
    }

    public RemoteStepBuilder<Data> invokeRemoteParticipant(Function<Data, CommandWithDestination> remoteInvocation) {
        return new RemoteStepBuilder<>(parent, remoteInvocation);
    }
}
