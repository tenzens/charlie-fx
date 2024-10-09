package com.tenzens.charliefx.Repository.Entries.Model;

import com.tenzens.charliefx.Repository.Model.Change;

public class UpstreamCommand {
    public enum Command {
        PUSH,
        FLUSH,
    }

    private final Command command;
    private final Change change;

    public UpstreamCommand(Command command, Change change) {
        this.command = command;
        this.change = change;
    }

    public Command getCommand() {
        return command;
    }

    public Change getChange() {
        return change;
    }
}
