package test.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import model.AddStateCommand;
import model.AddTransitionCommand;
import model.Automaton;
import model.CommandHistory;
import model.SetIsFinalCommand;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.model.CommandHistoryTest;
import ui.AddStateUICommand;
import ui.AddTransitionUICommand;
import ui.Graph;
import ui.SetIsFinalUICommand;
import ui.SetStartStateUICommand;
import ui.UICommand;

/**
 * Tests {@link CommandHistory} when using instances of {@link UICommand}. Based
 * heavily on {@link CommandHistoryTest}.
 * 
 * @author sp611
 *
 */
public class UICommandHistoryTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testGraphHistoryComplex() {
        final Automaton automaton = new Automaton();
        final Graph graph = new Graph();
        final CommandHistory history = new CommandHistory();

        // Create the states we are going to use
        AddStateCommand s = new AddStateCommand(automaton,
                automaton.getStartState());
        AddStateCommand b = new AddStateCommand(automaton,
                automaton.createNewState());
        AddStateCommand c = new AddStateCommand(automaton,
                automaton.createNewState());
        AddStateCommand d = new AddStateCommand(automaton,
                automaton.createNewState());
        AddStateCommand e = new AddStateCommand(automaton,
                automaton.createNewState());
        AddStateCommand f = new AddStateCommand(automaton,
                automaton.createNewState());

        // Create the transitions we are going to use
        AddTransitionCommand s_b_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(s.getState(), b.getState(), "1"));
        AddTransitionCommand b_c_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(b.getState(), c.getState(), "2"));
        AddTransitionCommand c_d_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(c.getState(), d.getState(), "3"));
        AddTransitionCommand d_e_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(d.getState(), e.getState(), "4"));
        AddTransitionCommand e_b_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(e.getState(), b.getState(), "5"));
        AddTransitionCommand c_f_0 = new AddTransitionCommand(automaton,
                automaton.createNewTransition(c.getState(), f.getState(), "!"));

        // Build the graph we want through a series of commands
        history.executeNewCommand(new SetStartStateUICommand(graph, s
                .getState()));
        history.executeNewCommand(new AddStateUICommand(graph, b));
        history.executeNewCommand(new AddTransitionUICommand(graph, s_b_0));

        history.executeNewCommand(new AddStateUICommand(graph, c));
        history.executeNewCommand(new AddTransitionUICommand(graph, b_c_0));

        history.executeNewCommand(new AddStateUICommand(graph, d));
        history.executeNewCommand(new AddStateUICommand(graph, f));
        history.executeNewCommand(new AddTransitionUICommand(graph, c_d_0));
        history.executeNewCommand(new AddTransitionUICommand(graph, c_f_0));
        history.executeNewCommand(new SetIsFinalUICommand(graph,
                new SetIsFinalCommand(automaton, f.getState(), true)));

        history.executeNewCommand(new AddStateUICommand(graph, e));
        history.executeNewCommand(new AddTransitionUICommand(graph, d_e_0));
        history.executeNewCommand(new AddTransitionUICommand(graph, e_b_0));

        // Test that all the states we expect to see exist
        assertTrue(graph.containsState(s.getState()));
        assertTrue(graph.containsState(b.getState()));
        assertTrue(graph.containsState(c.getState()));
        assertTrue(graph.containsState(d.getState()));
        assertTrue(graph.containsState(e.getState()));
        assertTrue(graph.containsState(f.getState()));

        // Test that state "f" is final
        assertTrue(f.getState().isFinal());

        // Test undo, transition shouldn't exist after
        assertTrue(graph.containsTransition(e_b_0.getTransition()));
        history.prev();
        assertFalse(graph.containsTransition(e_b_0.getTransition()));

        // Test undo to after first command executed
        history.seekIdx(2); // +1 because of necessary SetStartStateUICommand
        assertTrue(graph.containsState(s.getState()));
        assertTrue(graph.containsState(b.getState()));
        assertFalse(graph.containsState(c.getState()));
        assertFalse(graph.containsState(d.getState()));
        assertFalse(graph.containsState(e.getState()));
        assertFalse(graph.containsState(f.getState()));
        assertEquals(graph.getNumStateTransitions(s.getState()), 0);
        assertEquals(graph.getNumStateTransitions(b.getState()), 0);

        // Test only start state exists
        history.prev();
        assertTrue(graph.containsState(s.getState()));
        assertFalse(graph.containsState(b.getState()));
        assertFalse(graph.containsState(c.getState()));
        assertFalse(graph.containsState(d.getState()));
        assertFalse(graph.containsState(e.getState()));
        assertFalse(graph.containsState(f.getState()));
        assertEquals(graph.getNumStateTransitions(s.getState()), 0);

        // Finally, test we can replay to the final command
        history.seekIdx(history.getHistorySize());
        assertTrue(graph.containsState(s.getState()));
        assertTrue(graph.containsState(b.getState()));
        assertTrue(graph.containsState(c.getState()));
        assertTrue(graph.containsState(d.getState()));
        assertTrue(graph.containsState(e.getState()));
        assertTrue(graph.containsState(f.getState()));
        assertTrue(graph.containsTransition(e_b_0.getTransition()));
    }

}
