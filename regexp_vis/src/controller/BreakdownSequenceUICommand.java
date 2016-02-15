package controller;

import model.AddStateCommand;
import model.BreakdownSequenceCommand;
import model.Command;
import view.GraphCanvasFX;
import view.GraphNode;

public class BreakdownSequenceUICommand extends BreakdownUICommand {
    private final BreakdownSequenceCommand ccmd;

    public BreakdownSequenceUICommand(GraphCanvasFX graph,
            BreakdownSequenceCommand cmd) {
        super(graph, cmd);
        this.ccmd = cmd;

        /*
         * TODO: Case where transCount is too high, and states are too close
         * together for edges to be rendered must be handled differenty.
         */
        int transCount = this.ccmd.getNewTransitionsCount();
        GraphNode fromNode = graph.lookupNode(this.ccmd.getOriginalTransition()
                .getFrom().getId());
        GraphNode toNode = graph.lookupNode(this.ccmd.getOriginalTransition()
                .getTo().getId());
        final boolean fromChoice = BreakdownUITools.wasChoiceTransition(cmd);

        double dxPerNode = (toNode.getX() - fromNode.getX()) / transCount;
        double dyPerNode = (toNode.getY() - fromNode.getY()) / transCount;

        double curX = fromNode.getX() + dxPerNode;
        double curY = fromNode.getY() + dyPerNode;

        for (Command tmpCmd : this.ccmd.getCommands()) {
            if (tmpCmd instanceof AddStateCommand) {
                if (fromChoice) {
                    // TODO
                    AddStateCommand oldCommand = (AddStateCommand) tmpCmd;
                    AddStateUICommand newCommand = new AddStateUICommand(graph,
                            oldCommand, curX, curY);
                    this.commands.add(newCommand);
                    curX += dxPerNode;
                    curY += dyPerNode;
                } else {
                    AddStateCommand oldCommand = (AddStateCommand) tmpCmd;
                    AddStateUICommand newCommand = new AddStateUICommand(graph,
                            oldCommand, curX, curY);
                    this.commands.add(newCommand);
                    curX += dxPerNode;
                    curY += dyPerNode;
                }
            } else {
                this.commands.add(UICommand.fromCommand(graph, tmpCmd));
            }
        }
    }
}
