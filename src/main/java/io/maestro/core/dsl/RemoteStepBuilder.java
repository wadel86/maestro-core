package io.maestro.core.dsl;

import io.maestro.common.command.CommandWithDestination;
import io.maestro.core.saga.definition.SagaDefinition;
import io.maestro.core.saga.definition.step.RemoteStepImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class RemoteStepBuilder<D> {

    private final SagaDefinitionBuilder<D> parent;
    private final Function<D, CommandWithDestination> remoteInvocation;
    private final Map<String, BiConsumer<D, Object>> replyHandlers = new HashMap<>();
    private Optional<Consumer<D>> compensation;

    public RemoteStepBuilder
            (SagaDefinitionBuilder<D> parent, Function<D, CommandWithDestination> remoteInvocation) {
        this.parent = parent;
        this.remoteInvocation = remoteInvocation;
    }

    public <T> RemoteStepBuilder<D> onReply(Class<T> replyClass, BiConsumer<D, T> replyHandler) {
        this.replyHandlers.put(replyClass.getName(), (data, rawReply) -> replyHandler.accept(data, (T)rawReply));
        return this;
    }

    public RemoteStepBuilder<D> withCompensation(Consumer<D> compensation){
        this.compensation = Optional.of(compensation);
        return this;
    }

    public StepBuilder<D> step() {
        this.parent.addStep(new RemoteStepImpl<>(this.remoteInvocation, Optional.empty(), replyHandlers));
        return new StepBuilder<>(this.parent);
    }

    public SagaDefinition<D> build() {
        this.parent.addStep(new RemoteStepImpl<>(this.remoteInvocation, Optional.empty(), replyHandlers));
        return this.parent.build();
    }

}
