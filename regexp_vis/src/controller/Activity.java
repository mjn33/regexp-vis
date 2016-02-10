package controller;

import java.util.LinkedList;

import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import model.Automaton;
import model.AutomatonState;
import model.AutomatonTransition;
import model.BasicRegexp;
import model.Command;
import model.CommandHistory;
import model.InvalidRegexpException;
import view.GraphCanvasFX;
import view.GraphEdge;
import view.GraphNode;

/**
 * 
 * @author sp611
 *
 */
public abstract class Activity<T extends Event> {

    enum ActivityType {
        ACTIVITY_REGEXP_BREAKDOWN("Breakdown Regular Expression to FSA"),
        ACTIVITY_NFA_TO_REGEXP("Convert NFA to Regular Expression"),
        ACTIVITY_NFA_TO_DFA("Convert NFA to DFA");

        private final String text;

        private ActivityType(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }
    }

    protected final GraphCanvasFX canvas;
    protected final Automaton automaton;
    protected final CommandHistory history;

    Activity(GraphCanvasFX canvas, Automaton automaton) {
        super();
        this.canvas = canvas;
        this.automaton = automaton;
        this.history = new CommandHistory();
    }

    public void onEnteredRegexp(String text) {
        System.out.printf("Entered regexp: %s%n", text);
        BasicRegexp re = null;
        try {
            re = BasicRegexp.parseRegexp(text);
            // BasicRegexp.debugPrintBasicRegexp(0, re);
        } catch (InvalidRegexpException e1) {
            Alert alert = new Alert(AlertType.ERROR,
                    "Error: invalid regexp entered. Details: \n\n"
                            + e1.getMessage());
            alert.showAndWait();
            return;
        }

        this.canvas.removeAllNodes();
        this.automaton.clear();
        // TODO: Add the following to history
        AutomatonState startState = this.automaton.getStartState();
        AutomatonState finalState = this.automaton.createNewState();
        AutomatonTransition trans = this.automaton
                .createNewTransition(startState, finalState, re);
        finalState.setFinal(true);
        this.automaton.addStateWithTransitions(finalState,
                new LinkedList<AutomatonTransition>());
        this.automaton.addTransition(trans);

        GraphNode startNode = this.canvas.addNode(startState.getId(), 50.0,
                50.0);
        this.canvas.setNodeUseStartStyle(startNode, true);
        GraphNode endNode = this.canvas.addNode(finalState.getId(),
                this.canvas.getWidth() - 50.0, this.canvas.getHeight() - 50.0);
        this.canvas.setNodeUseFinalStyle(endNode, true);
        GraphEdge edge = this.canvas.addEdge(trans.getId(), startNode, endNode,
                re.toString());
    }

    public abstract void processEvent(T event);

    protected void executeNewCommand(Command cmd) {
        if (cmd instanceof UICommand) {
            // TODO: This doesn't test for any different subclass types, but it
            // should be okay...
            throw new IllegalArgumentException(
                    "Argument should be of type Command, not UICommand.");
        }

        UICommand uiCmd = UICommand.fromCommand(this.canvas, cmd);
        if (uiCmd != null) {
            this.history.executeNewCommand(uiCmd);
        }
    }

    // Expose CommandHistory methods, except for executeNewCommand()
    void historyPrev() {
        this.history.prev();
    }

    void historyNext() {
        this.history.next();
    }

    void historySeek(int idx) {
        this.history.seekIdx(idx);
    }

    void historyStart() {
        this.history.seekIdx(0);
    }

    void historyEnd() {
        this.history.seekIdx(this.history.getHistorySize());
    }

}
