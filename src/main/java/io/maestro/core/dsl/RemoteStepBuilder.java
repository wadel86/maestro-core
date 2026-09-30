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
    private Optional<Consumer<D>> compensation = Optional.empty();
    private Optional<Function<D, CommandWithDestination>> remoteCompensation = Optional.empty();

    public RemoteStepBuilder
            (SagaDefinitionBuilder<D> parent, Function<D, CommandWithDestination> remoteInvocation) {
        this.parent = parent;
        this.remoteInvocation = remoteInvocation;
    }

    public <T> RemoteStepBuilder<D> onReply(Class<T> replyClass, BiConsumer<D, T> replyHandler) {
        this.replyHandlers.put(replyClass.getName(), (data, rawReply) -> replyHandler.accept(data, (T)rawReply));
        return this;
    }

    /**
     * Undo this step with local business logic, completing immediately.
     *
     * @see #withRemoteCompensation(Function)
     */
    public RemoteStepBuilder<D> withCompensation(Consumer<D> compensation){
        this.compensation = Optional.of(compensation);
        return this;
    }

    /**
     * Undo this step by asking the participant to undo its own work: the saga sends the
     * compensating command and waits for the reply before unwinding any further, so a
     * failed undo stops the saga instead of being silently passed over.
     *
     * <p>Replies to the compensating command go through the same {@link #onReply} handlers
     * as the forward direction, keyed by their own reply type. Takes precedence over
     * {@link #withCompensation(Consumer)} if both are given.
     */
    public RemoteStepBuilder<D> withRemoteCompensation
            (Function<D, CommandWithDestination> remoteCompensation){
        this.remoteCompensation = Optional.of(remoteCompensation);
        return this;
    }

    public StepBuilder<D> step() {
        this.parent.addStep(buildStep());
        return new StepBuilder<>(this.parent);
    }

    public SagaDefinition<D> build() {
        this.parent.addStep(buildStep());
        return this.parent.build();
    }

    private RemoteStepImpl<D> buildStep() {
        return new RemoteStepImpl<>
                (this.remoteInvocation, compensation, remoteCompensation, replyHandlers);
    }

}
