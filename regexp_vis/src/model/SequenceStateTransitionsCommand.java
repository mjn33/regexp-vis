package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to remove a state and sequence the in-going transitions for that
 * state with the out-going transitions for that state. Part of the NFA to
 * Regexp translation.
 */
public class SequenceStateTransitionsCommand extends CompositeCommand {

    private final AutomatonState mState;
    private final ArrayList<AutomatonTransition> mNewTransitions;

    public SequenceStateTransitionsCommand(Automaton automaton,
            AutomatonState state)
    {
        super(automaton);
        mState = state;
        mNewTransitions = new ArrayList<>();

        List<AutomatonTransition> ingoingTrans = automaton
                .getIngoingTransition(mState);
        List<AutomatonTransition> outgoingTrans = automaton
                .getStateTransitions(mState);

        // For every combination of pairing in-going with out-going transitions
        // for this state: combine the regexps of the transitions using sequence
        // via a transition which bypasses this state.
        for (AutomatonTransition t1 : ingoingTrans) {
            for (AutomatonTransition t2 : outgoingTrans) {
                ArrayList<BasicRegexp> operands = new ArrayList<>();
                operands.add(t1.getData());
                operands.add(t2.getData());
                BasicRegexp newRe = new BasicRegexp(operands,
                        BasicRegexp.RegexpOperator.SEQUENCE);
                // Do very low depth optimisation
                newRe = newRe.optimise(BasicRegexp.OPTIMISE_ALL, 1);
                AutomatonTransition newTrans = automaton
                        .createNewTransition(t1.getFrom(), t2.getTo(), newRe);
                super.commands.add(new AddTransitionCommand(automaton, newTrans));
                mNewTransitions.add(newTrans);
            }
        }

        // Remove all in-going transitions
        for (AutomatonTransition t : ingoingTrans) {
            super.commands.add(new RemoveTransitionCommand(automaton, t));
        }
        // Remove all out-going transitions
        for (AutomatonTransition t : outgoingTrans) {
            super.commands.add(new RemoveTransitionCommand(automaton, t));
        }
        // Remove the state itself
        super.commands.add(new RemoveStateCommand(automaton, mState));
    }

    /**
     * @return The list of new transitions from sequencing the in-going and
     * out-going transitions of the target state.
     */
    public List<AutomatonTransition> getSequencedTransitions()
    {
        return mNewTransitions;
    }

    /**
     * @return The state which we are removing.
     */
    public AutomatonState getState()
    {
        return mState;
    }

}
