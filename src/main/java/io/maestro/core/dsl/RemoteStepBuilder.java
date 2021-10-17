package io.maestro.core.dsl;

import io.maestro.core.command.CommandWithDestination;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.RemoteStepImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RemoteStepBuilder<Data> {

    private final SagaDefinitionBuilder<Data> parent;
    private final Function<Data, CommandWithDestination> remoteInvocation;
    private final Map<String, BiConsumer<Data, Object>> replyHandlers = new HashMap<>();
    private Optional<Consumer<Data>> compensation;

    public RemoteStepBuilder
            (SagaDefinitionBuilder<Data> parent, Function<Data, CommandWithDestination> remoteInvocation) {
        this.parent = parent;
        this.remoteInvocation = remoteInvocation;
    }

    public <T> RemoteStepBuilder<Data> onReply(Class<T> replyClass, BiConsumer<Data, T> replyHandler) {
        this.replyHandlers.put(replyClass.getName(), (data, rawReply) -> replyHandler.accept(data, (T)rawReply));
        return this;
    }

    public RemoteStepBuilder<Data> withCompensation(Consumer<Data> compensation){
        this.compensation = Optional.of(compensation);
        return this;
    }

    public StepBuilder<Data> step() {
        this.parent.addStep(new RemoteStepImpl<>(this.remoteInvocation, replyHandlers, Optional.empty()));
        return new StepBuilder<>(this.parent);
    }

    public SagaDefinition<Data> build() {
        this.parent.addStep(new RemoteStepImpl<>(this.remoteInvocation, replyHandlers, Optional.empty()));
        return this.parent.build();
    }

}
