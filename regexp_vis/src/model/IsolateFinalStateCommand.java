package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IsolateFinalStateCommand extends Command {
    protected final ArrayList<Command> mCommands;
    private final List<AutomatonState> mOldFinalStates;
    private final AutomatonState mNewFinalState;
    private final AddStateCommand mNewStateCommand;

    public static IsolateFinalStateCommand create(Automaton automaton)
    {
        boolean needsIsolation = false;
        ArrayList<AutomatonState> finalStates = new ArrayList<>();

        Iterator<Automaton.StateTransitionsPair> it = automaton.graphIterator();
        while (it.hasNext()) {
            Automaton.StateTransitionsPair pair = it.next();
            AutomatonState state = pair.getState();
            if (state.isFinal()) {
                finalStates.add(state);
            }
        }

        if (finalStates.size() > 1) {
            // Multiple final states, need to create an isolated one
            needsIsolation = true;
        } else if (finalStates.size() == 1) {
            // Single final state, only need to do something if it has outgoing
            // transitions
            AutomatonState state = finalStates.get(0);
            if (automaton.hasOutgoingTransition(state)) {
                needsIsolation = true;
            }
        } else {
            // No final states, language of the automaton is the empty set
        }

        if (!needsIsolation) {
            return null;
        }

        return new IsolateFinalStateCommand(automaton, finalStates);
    }

    private IsolateFinalStateCommand(Automaton automaton,
            List<AutomatonState> finalStates)
    {
        super(automaton);
        mCommands = new ArrayList<>();
        mOldFinalStates = finalStates;

        // Create the new final state
        mNewFinalState = automaton.createNewState();
        mNewFinalState.setFinal(true);
        mNewStateCommand = new AddStateCommand(automaton, mNewFinalState);
        mCommands.add(mNewStateCommand);

        for (AutomatonState state : finalStates) {
            // Make all the old states not final anymore
            mCommands.add(new SetIsFinalCommand(automaton, state, false));
            // Add an epsilon transition to the new state
            AutomatonTransition newTrans = automaton.createNewTransition(state,
                    mNewFinalState, BasicRegexp.EPSILON_EXPRESSION);
            mCommands.add(new AddTransitionCommand(automaton, newTrans));
        }
    }

    /**
     * @return The command which adds the new (isolated) final state.
     */
    public AddStateCommand getNewStateCommand()
    {
        return mNewStateCommand;
    }

    /**
     * @return The list of the original final states.
     */
    public List<AutomatonState> getOldFinalStates()
    {
        return Collections.unmodifiableList(mOldFinalStates);
    }

    /**
     * @return The new (isolated) final state.
     */
    public AutomatonState getNewFinalState()
    {
        return mNewFinalState;
    }

    /**
     * @return the list of commands which this command executes, as an
     * unmodifiable list
     */
    public List<Command> getCommands()
    {
        return Collections.unmodifiableList(mCommands);
    }

    @Override
    public void undo()
    {
        ListIterator<Command> it = mCommands.listIterator(mCommands.size());
        while (it.hasPrevious()) {
            Command c = it.previous();
            c.undo();
        }
    }

    @Override
    public void redo()
    {
        for (Command c : mCommands) {
            c.redo();
        }
    }

}
