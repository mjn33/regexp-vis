package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import model.Command;
import model.RemoveStateCleanlyCommand;
import view.GraphCanvasFX;

public class RemoveUnreachableStateUICommand extends UICommand {
    private ArrayList<UICommand> commands;
    private final RemoveStateCleanlyCommand ccmd;

    public RemoveUnreachableStateUICommand(GraphCanvasFX graph,
            RemoveStateCleanlyCommand cmd) {
        super(graph, cmd);
        this.ccmd = cmd;
        this.commands = new ArrayList<>();
        for (Command c : cmd.getCommands()) {
            this.commands.add(UICommand.fromCommand(graph, c));
            c.toString();
        }
    }

    // FIXME: duplicate code (e.g. BreakdownUICommand,
    // RemoveEpsilonTransitionsUICommand, RemoveEquivalentStatesUICommand,
    // RemoveNonDeterminismCommand, RemoveUnreachableStatesUICommand), maybe a
    // new base class CompositeUICommand? CompositeUICommand as a field? Or a
    // static utility method in UICommand?
    @Override
    public void undo() {
        ListIterator<UICommand> it = this.commands
                .listIterator(this.commands.size());
        while (it.hasPrevious()) {
            UICommand c = it.previous();
            c.undo();
        }
    }

    @Override
    public void redo() {
        for (UICommand c : this.commands) {
            c.redo();
        }
    }

    public List<UICommand> getCommands() {
        return Collections.unmodifiableList(this.commands);
    }

    @Override
    public String getDescription() {
        return "Removed unreachable state " + this.ccmd.getState().toString();
    }
}
