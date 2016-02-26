package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class which pre-computes data regarding an NFA, for use by
 * RemoveEquivalentStatesCommand.
 */
public class RemoveEpsilonTransitionsContext {
    private final Map<AutomatonState, Set<AutomatonState>> mEpsilonClosureMap;
    private final List<Set<AutomatonState>> mEquivalentStatesList;

    /**
     * Goes through "todoStates" to create a set of states which are equivalent
     * to the first element of the list. Note: by equivalent we mean that as
     * these states have the same epsilon closure, they are essentially
     * indistinguishable apart from in-going transitions.
     *
     * @param todoStates A non empty list of states to compare, the equivalent
     * states are removed from the list
     * @param epsilonClosureMap A map from states to a set describing their
     * epsilon closure
     * @return A set of equivalent states
     */
    public static Set<AutomatonState> calcEquivalentStates(
            List<AutomatonState> todoStates,
            Map<AutomatonState, Set<AutomatonState>> epsilonClosureMap)
    {
        ArrayList<AutomatonState> toRemove = new ArrayList<>();
        Iterator<AutomatonState> it = todoStates.iterator();
        // Being passed an empty list is a programming error, so don't check
        // using hasNext()
        AutomatonState first = it.next();
        Set<AutomatonState> firstEpsilonClosure = epsilonClosureMap.get(first);
        toRemove.add(first);
        while (it.hasNext()) {
            AutomatonState s = it.next();
            Set<AutomatonState> sEpsilonClosure = epsilonClosureMap.get(s);
            if (sEpsilonClosure.equals(firstEpsilonClosure)) {
                toRemove.add(s);
            }
        }
        todoStates.removeAll(toRemove);
        return new HashSet<AutomatonState>(toRemove);
    }

    public RemoveEpsilonTransitionsContext(Automaton automaton)
    {
        mEpsilonClosureMap = new HashMap<>();
        mEquivalentStatesList = new ArrayList<>();
        precompute(automaton);
    }

    /**
     * Create the pre-computed data for this context
     * @param automaton
     */
    private void precompute(Automaton automaton)
    {
        ArrayList<AutomatonState> todoStates = new ArrayList<>();
        Iterator<Automaton.StateTransitionsPair> it = automaton.graphIterator();
        while (it.hasNext()) {
            Automaton.StateTransitionsPair pair = it.next();
            Set<AutomatonState> epsilonClosure = TranslationTools
                    .calcEpsilonReachableStates(automaton, pair.getState());
            mEpsilonClosureMap.put(pair.getState(), epsilonClosure);
            todoStates.add(pair.getState());
        }

        while (!todoStates.isEmpty()) {
            Set<AutomatonState> equivalentStates = calcEquivalentStates(
                    todoStates, mEpsilonClosureMap);
            mEquivalentStatesList.add(equivalentStates);
        }
        // All sets of equivalent states calculated
    }

    /**
     *
     * @param state The state in question
     * @return The set of states equivalent to the given state, including the
     * given state
     */
    public Set<AutomatonState> getEquivalentStates(AutomatonState state)
    {
        for (Set<AutomatonState> equivalentStates : mEquivalentStatesList) {
            if (equivalentStates.contains(state)) {
                return Collections.unmodifiableSet(equivalentStates);
            }
        }
        return null;
    }

    /**
     * @param automaton The automaton for the given state
     * @param state The state in question
     * @return True if any equivalent states for the given state exist in the
     * given automaton, false otherwise
     */
    public boolean equivalentStatesExist(Automaton automaton,
            AutomatonState state)
    {
        Set<AutomatonState> equivalentStates = getEquivalentStates(state);
        for (AutomatonState eqivalentState : equivalentStates) {
            if (eqivalentState != state
                    && automaton.stateExists(eqivalentState)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param a The first state to compare
     * @param b The second state to compare
     * @return True if the two states are equivalent, false otherwise
     */
    public boolean areStatesEquivalent(AutomatonState a, AutomatonState b)
    {
        for (Set<AutomatonState> equivalentStates : mEquivalentStatesList) {
            boolean containsA = equivalentStates.contains(a);
            boolean containsB = equivalentStates.contains(b);
            if (containsA != containsB) {
                // We know "a" and "b" are in different sets at this point
                // so return false.
                return false;
            } else if (containsA && containsB) {
                return true;
            }
        }
        return false;
    }
}
