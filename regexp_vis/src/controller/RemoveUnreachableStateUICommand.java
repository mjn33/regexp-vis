package controller;

import model.RemoveStateCleanlyCommand;
import view.GraphCanvasFX;

/**
 * TODO: Inconsistent naming {@link RemoveUnreachableStateUICommand} and
 * {@link RemoveStateCleanlyCommand}
 */
public class RemoveUnreachableStateUICommand extends CompositeUICommand {

    private final RemoveStateCleanlyCommand ccmd;

    public RemoveUnreachableStateUICommand(GraphCanvasFX graph,
            RemoveStateCleanlyCommand cmd) {
        super(graph, cmd);
        this.ccmd = cmd;
    }

    @Override
    public String getDescription() {
        return "Removed unreachable state " + this.ccmd.getState().toString();
    }

}
