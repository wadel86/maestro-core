package io.maestro.core.saga.definition.step;

import io.maestro.common.command.CommandWithDestination;

public class RemoteStepOutcome<Data> implements StepOutcome<Data> {
    private boolean isSuccessful;
    private CommandWithDestination commandToSend;

    public RemoteStepOutcome(CommandWithDestination commandToSend) {
        this.commandToSend = commandToSend;
    }

    public RemoteStepOutcome(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public CommandWithDestination getCommandToSend() {
        return commandToSend;
    }
}
