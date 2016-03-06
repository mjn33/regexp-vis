package controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import model.Automaton;
import model.AutomatonTransition;
import model.BreakdownCommand;
import model.TranslationTools;
import view.GraphCanvasEvent;
import view.GraphCanvasFX;
import view.GraphEdge;

/**
 *
 * @author sp611
 *
 */
public class RegexpBreakdownActivity extends Activity {

    private static final Logger LOGGER = Logger
            .getLogger(RegexpBreakdownActivity.class.toString());

    public RegexpBreakdownActivity(GraphCanvasFX canvas, Automaton automaton) {
        super(canvas, automaton);
    }

    @Override
    public void onNodeClicked(GraphCanvasEvent event) {

    }

    @Override
    public void onEdgeClicked(GraphCanvasEvent event) {
        if (event.getMouseEvent().getClickCount() == 2) {
            onEdgeDoubleClick(event);
        }
    }

    @Override
    public void onBackgroundClicked(GraphCanvasEvent event) {

    }

    @Override
    public void onContextMenuRequested(ContextMenuEvent event) {

    }

    @Override
    public void onHideContextMenu(MouseEvent event) {

    }

    private void onEdgeDoubleClick(GraphCanvasEvent event) {
        if (this.history.getHistoryIdx() != this.history.getHistorySize()
                && !this.history.isClobbered()) {
            LOGGER.log(Level.FINE, "Ignoring breakdown event as we are not at "
                    + "the end of the history list.");
            return;
        }

        GraphEdge edge = event.getTargetEdge();
        if (edge == null) {
            return;
        }

        AutomatonTransition trans = this.automaton
                .getAutomatonTransitionById(edge.getId());

        if (trans == null) {
            LOGGER.log(Level.WARNING, "Could not find an edge with id " + edge.getId());
            return;
        }

        BreakdownCommand cmd = TranslationTools
                .createBreakdownCommand(this.automaton, trans);
        super.executeNewCommand(cmd);
    }
}
