package controller;

import model.AddStateCommand;
import model.AddTransitionCommand;
import model.Automaton;
import model.BreakdownCommand;
import model.Command;
import model.RemoveStateCommand;
import model.RemoveTransitionCommand;
import model.SetIsFinalCommand;
import ui.Graph;
import view.GraphCanvasFX;

/**
 * Extends {@link Command} to facilitate UI-side command history.
 * <p>
 * Calling {@link #undo()} or {@link #redo()} will apply changes to both the
 * {@link Graph} and the {@link Automaton}.
 *
 * @author sp611
 *
 */
public abstract class UICommand extends Command {
    protected final GraphCanvasFX graph;
    protected final Command cmd;

    public UICommand(GraphCanvasFX graph, Command cmd) {
        super(cmd.getAutomaton());
        this.graph = graph;
        this.cmd = cmd;
    }

    /**
     * Convert a {@link Command} into a {@link UICommand}
     *
     * @param graph
     *            the {@link Graph}
     * @param cmd
     *            the {@link Command}
     *
     * @return a new {@link UICommand} that is equivalent to the given
     *         {@link Command}
     */
    public static UICommand fromCommand(GraphCanvasFX graph, Command cmd) {
        if (cmd instanceof AddStateCommand) {
            return new AddStateUICommand(graph, (AddStateCommand) cmd, 0, 0);
        } else if (cmd instanceof AddTransitionCommand) {
            return new AddTransitionUICommand(graph, (AddTransitionCommand) cmd);
        } else if (cmd instanceof BreakdownCommand) {
            return new BreakdownUICommand(graph, (BreakdownCommand) cmd);
        } else if (cmd instanceof RemoveStateCommand) {
            return new RemoveStateUICommand(graph, (RemoveStateCommand) cmd);
        } else if (cmd instanceof RemoveTransitionCommand) {
            return new RemoveTransitionUICommand(graph,
                    (RemoveTransitionCommand) cmd);
        } else if (cmd instanceof SetIsFinalCommand) {
            return new SetIsFinalUICommand(graph, (SetIsFinalCommand) cmd);
        } else if (cmd == null) {
            return null;
        } else {
            String msg = String.format("Conversion from %s to UICommand has "
                    + "not yet been implemented.", cmd.getClass().toString());
            throw new UnsupportedOperationException(msg);
        }
    }
}
