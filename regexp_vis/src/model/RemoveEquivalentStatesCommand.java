package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Command which removes the other equivalent states for a state. Assumes the
 * epsilon transitions for the state have been removed (by
 * RemoveEpsilonTransitionsCommand). For the NFA to DFA conversion process.
 */
public class RemoveEquivalentStatesCommand extends Command {
    protected final ArrayList<Command> mCommands;
    private final RemoveEpsilonTransitionsContext mCtx;
    private final AutomatonState mTargetState;

    public RemoveEquivalentStatesCommand(Automaton automaton,
            RemoveEpsilonTransitionsContext ctx, AutomatonState state)
    {
        super(automaton);
        mCommands = new ArrayList<Command>();
        mCtx = ctx;
        mTargetState = state;

        Set<AutomatonState> equivalentStates = ctx
                .getEquivalentStates(mTargetState);

        // Transitions in-going to any one of these equivalent states
        ArrayList<AutomatonTransition> trans = new ArrayList<>();

        Iterator<Automaton.StateTransitionsPair> it = automaton.graphIterator();
        while (it.hasNext()) {
            Automaton.StateTransitionsPair pair = it.next();
            for (AutomatonTransition t : automaton.getStateTransitions(pair
                    .getState())) {
                if (equivalentStates.contains(t.getTo())) {
                    trans.add(t);
                }
            }
        }

        // Sort as the ordering for BasicRegexp defines but with additional
        // comparisons:
        //   * First: sort by which state the transitions come from
        //   * If the BasicRegexp's of two transitions are equal then the
        //     transitions which go to the original state are ordered first.
        //   * This is such that when we remove duplicates, we don't remove the
        //     transitions already going to the target state
        Collections.sort(trans, new Comparator<AutomatonTransition>() {
            @Override
            public int compare(AutomatonTransition t1, AutomatonTransition t2) {
                //return t1.getData().compareTo(t2.getData());
                // Sort based on the "from" state first
                if (t1.getFrom().getId() < t2.getFrom().getId()) {
                    return -1;
                } else if (t1.getFrom().getId() > t2.getFrom().getId()) {
                    return 1;
                }
                int result = t1.getData().compareTo(t2.getData());

                if (result == 0 && t1.getTo() != t2.getTo()) {
                    if (t1.getTo() == mTargetState) {
                        return -1;
                    } else if (t2.getTo() == mTargetState) {
                        return 1;
                    } else {
                        return 0;
                    }
                } else {
                    return result;
                }
            }
        });

        // Create commands to remove the in-going transitions to the states we
        // will remove
        for (AutomatonTransition t : trans) {
            if (t.getTo() != mTargetState
                    && equivalentStates.contains(t.getTo())) {
                mCommands.add(new RemoveTransitionCommand(automaton, t));
            }
        }

        // Create commands to remove the out-going transitions from the states
        // we will remove
        for (AutomatonState s2 : equivalentStates) {
            if (s2 == mTargetState) {
                continue;
            }
            for (AutomatonTransition t : automaton.getStateTransitions(s2)) {
                // Note: this condition is to filter out states we may have
                // already created a RemoveTransitionCommand for
                if (!(t.getTo() != mTargetState
                        && equivalentStates.contains(t.getTo()))) {
                    mCommands.add(new RemoveTransitionCommand(automaton, t));
                }
            }
        }

        // Actually remove the states
        for (AutomatonState s2 : equivalentStates) {
            if (s2 != mTargetState) {
                mCommands.add(new RemoveStateCommand(automaton, s2));
            }
        }

        // Remove duplicate transitions, keeping the transitions which already
        // previous went to the target state
        Iterator<AutomatonTransition> it2 = trans.iterator();
        AutomatonTransition prev = null;
        while (it2.hasNext()) {
            AutomatonTransition t = it2.next();
            // Remove transitions coming from the equivalent states we just
            // removed
            if (t.getFrom() != mTargetState
                    && equivalentStates.contains(t.getFrom())) {
                it2.remove();
            }
            // Read: previous transition has the same transition expression and
            // target state, and doesn't go to "mTargetState"
            else if (prev != null && prev.getFrom() == t.getFrom()
                    && prev.getData().equals(t.getData())
                    && t.getTo() != mTargetState) {
                it2.remove();
            } else {
                prev = t;
            }
        }

        // Remove transitions already going to "mTargetState", otherwise we
        // would be adding duplicates
        it2 = trans.iterator();
        while (it2.hasNext()) {
            AutomatonTransition t = it2.next();
            if (t.getTo() == mTargetState) {
                it2.remove();
            }
        }

        // Create the transitions, but now always to the target state
        for (AutomatonTransition t : trans) {
            AutomatonTransition newTrans = automaton.createNewTransition(
                    t.getFrom(), mTargetState, t.getData());
            mCommands.add(new AddTransitionCommand(automaton, newTrans));
        }
    }

    /**
     * @return The RemoveEpsilonTransitionsContext for this command.
     */
    public RemoveEpsilonTransitionsContext getContext()
    {
        return mCtx;
    }

    /**
     * @return The state of which we are removing the equivalents of.
     */
    public AutomatonState getTargetState()
    {
        return mTargetState;
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
