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
package controller;

import java.util.List;

import model.AutomatonTransition;
import model.SequenceStateTransitionsCommand;
import view.GraphCanvasFX;

public class SequenceStateTransitionsUICommand extends CompositeUICommand {

    private final SequenceStateTransitionsCommand ccmd;

    public SequenceStateTransitionsUICommand(GraphCanvasFX graph,
            SequenceStateTransitionsCommand cmd) {
        super(graph, cmd);
        this.ccmd = cmd;
    }

    @Override
    public String getDescription() {
        List<AutomatonTransition> sequencedTrans = ccmd
                .getSequencedTransitions();
        String sequencedStr = StringUtils
                .transitionListToEnglish(sequencedTrans);
        String stateStr = ccmd.getState().toString();

        return String.format("Concatenated transitions going through "
                + "state %s into transition%s %s", stateStr,
                sequencedTrans.size() != 1 ? "s" : "", sequencedStr);
    }
}
