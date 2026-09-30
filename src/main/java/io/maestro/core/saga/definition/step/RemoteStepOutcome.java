package io.maestro.core.saga.definition.step;

import io.maestro.common.command.CommandWithDestination;

/**
 * Outcome of a remote step, in one of its two distinct roles.
 *
 * <p>Invoking a remote participant produces a command to dispatch, and that is a
 * <em>success</em>: the step did its job by deciding what to send. Whether the
 * participant then succeeds is a separate event, reported later by its reply. Use
 * {@link #dispatch(CommandWithDestination)} for the first and {@link #replied(boolean)}
 * for the second.
 */
public class RemoteStepOutcome<D> implements StepOutcome<D> {

    private final boolean isSuccessful;
    private final CommandWithDestination commandToSend;

    /** A command was produced and must be dispatched; the step itself succeeded. */
    public static <D> RemoteStepOutcome<D> dispatch(CommandWithDestination commandToSend) {
        return new RemoteStepOutcome<>(commandToSend);
    }

    /** The participant replied; {@code isSuccessful} is its verdict. */
    public static <D> RemoteStepOutcome<D> replied(boolean isSuccessful) {
        return new RemoteStepOutcome<>(isSuccessful);
    }

    /**
     * @see #dispatch(CommandWithDestination)
     */
    public RemoteStepOutcome(CommandWithDestination commandToSend) {
        this.isSuccessful = true;
        this.commandToSend = commandToSend;
    }

    /**
     * @see #replied(boolean)
     */
    public RemoteStepOutcome(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
        this.commandToSend = null;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    /** The command to dispatch, or {@code null} when this outcome carries a reply verdict. */
    public CommandWithDestination getCommandToSend() {
        return commandToSend;
    }
}
