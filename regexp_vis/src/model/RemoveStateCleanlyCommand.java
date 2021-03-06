/*
 * Copyright (c) 2015, 2016 Matthew J. Nicholls, Samuel Pengelly,
 * Parham Ghassemi, William R. Dix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Similar to RemoveStateCommand, however this command also handles in-going
 * transitions. Note because of this, similarly to BreakdownIterationCommand for
 * example, this command should be executed before any other commands are
 * executed. For example, adding another in-going transition afterwards would
 * result in that transition not being removed/added correctly.
 */
public class RemoveStateCleanlyCommand extends CompositeCommand {
    private final AutomatonState mState;

    public RemoveStateCleanlyCommand(Automaton automaton, AutomatonState state)
    {
        super(automaton);
        mState = state;

        // Find in-going transitions and create commands to remove them
        Iterator<Automaton.StateTransitionsPair> it = automaton.graphIterator();
        while (it.hasNext()) {
            Automaton.StateTransitionsPair pair = it.next();
            List<AutomatonTransition> trans = pair.getTransitions();
            for (AutomatonTransition t : trans) {
                if (t.getTo() == state) {
                    super.commands.add(new RemoveTransitionCommand(
                            automaton, t));
                }
            }
        }

        super.commands.add(new RemoveStateCommand(automaton, state));
    }

    /**
     * @return The state which is to be removed
     */
    public AutomatonState getState()
    {
        return mState;
    }

}
