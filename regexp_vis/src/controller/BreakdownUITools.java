package controller;

import java.util.LinkedList;
import java.util.List;

import javafx.geometry.Point2D;
import model.AddStateCommand;
import model.Automaton;
import model.AutomatonState;
import model.AutomatonTransition;
import model.BreakdownCommand;
import model.BreakdownIterationCommand;
import model.BreakdownSequenceCommand;
import model.Command;
import model.RemoveStateCommand;
import view.GraphCanvasFX;
import view.GraphNode;

/**
 * 
 * @author sp611
 *
 */
public class BreakdownUITools {

    /**
     * The height of the triangles, rectangles, and curves on which new nodes
     * are placed when a breakdown creates new states.
     */
    public static final double BREAKDOWN_HEIGHT_PX = 4
            * GraphCanvasFX.DEFAULT_NODE_RADIUS;

    /**
     * Calculate the third point of triangle ABC, where points A and B lie on
     * the base of the triangle.
     * 
     * @param ax
     *            x coordinate of A
     * @param ay
     *            y coordinate of A
     * @param bx
     *            x coordinate of B
     * @param by
     *            y coordinate of B
     * @param above
     *            whether to find the y > midY or the y < midY point
     * @return {@link Point2D} C
     */
    private static Point2D computeThirdPoint(double ax, double ay, double bx,
            double by, boolean above) {
        final int sign = (ay < by) ? 1 : -1; // Dictates where to place point C
        final double dx = Math.abs(ax - bx);
        final double dy = Math.abs(ay - by) * sign;
        final double theta = Math.atan(dy / dx) * sign; // Slope of line AB
        final double mx = (ax + (dx / 2)), my = (ay + (dy / 2)); // Midpoint
        final double w = (BREAKDOWN_HEIGHT_PX * Math.sin(theta));
        final double n = sign * (Math
                .sqrt(Math.pow(BREAKDOWN_HEIGHT_PX, 2) - Math.pow(w, 2)));

        if (above) {
            final double cx = (mx + w);
            final double cy = (my - n);
            return new Point2D(cx, cy);
        } else {
            final double cx = (mx - w);
            final double cy = (my + n);
            return new Point2D(cx, cy);
        }
    }

    private static Point2D[] computeRectanglePoints(double ax, double ay,
            double bx, double by, boolean above) {
        final int sign = (ay < by) ? 1 : -1;
        final double deltaX = Math.abs(ax - bx);
        final double deltaY = Math.abs(ay - by) * sign;
        final double theta = Math.atan(deltaY / deltaX) * sign;
        final double w = (BREAKDOWN_HEIGHT_PX * Math.sin(theta));
        final double n = sign * (Math
                .sqrt(Math.pow(BREAKDOWN_HEIGHT_PX, 2) - Math.pow(w, 2)));

        if (above) {
            final double cx = (ax - w), cy = (ay + n);
            final double dx = (bx - w), dy = (by + n);
            return new Point2D[] { new Point2D(cx, cy), new Point2D(dx, dy) };
        } else {
            final double cx = (ax - w), cy = (ay + n);
            final double dx = (bx - w), dy = (by + n);
            return new Point2D[] { new Point2D(cx, cy), new Point2D(dx, dy) };
        }
    }

    private static Point2D[] computePoints(double ax, double ay, double bx,
            double by, int numPoints, boolean above) {
        if (numPoints < 1) {
            throw new IllegalArgumentException();
        } else if (numPoints == 1) {
            return new Point2D[] { computeThirdPoint(ax, ay, bx, by, above) };
        }
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * @return The number of occurrences of {@link AddStateCommand} in the given
     *         {@link Command}.
     */
    public static int getNumAddedStates(BreakdownCommand cmd) {
        int count = 0;
        for (Command c : cmd.getCommands()) {
            if (c instanceof AddStateCommand) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return The number of occurrences of {@link RemoveStateCommand} in the
     *         given {@link Command}.
     */
    public static int getNumRemovedStates(BreakdownCommand cmd) {
        int count = 0;
        for (Command c : cmd.getCommands()) {
            if (c instanceof RemoveStateCommand) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks whether the original transition of the given
     * {@link BreakdownCommand} was one of a number of choices that go from
     * transition.from to transition.to.
     * 
     * @param cmd
     *            the command
     * @return true if the transition was part of a choice
     */
    public static boolean wasChoiceTransition(BreakdownCommand cmd) {
        final AutomatonTransition transition = cmd.getOriginalTransition();
        final List<AutomatonTransition> transitions = getTransitionsBetween(
                cmd.getAutomaton(), transition.getFrom(), transition.getTo());
        return (transitions.size() > 1 && transitions.contains(transition));
    }

    /**
     * Returns a list of all {@link AutomatonTransition} that go directly
     * between two given {@link AutomatonState}.
     * 
     * @param automaton
     *            the {@link Automaton} that the states and transitions belong
     *            to
     * @param from
     *            the 'from' state
     * @param to
     *            the 'to' state
     * @return the list
     */
    public static List<AutomatonTransition> getTransitionsBetween(
            Automaton automaton, AutomatonState from, AutomatonState to) {
        final List<AutomatonTransition> fromTrs = automaton
                .getStateTransitions(from);
        final List<AutomatonTransition> toTrs = automaton
                .getIngoingTransition(to);

        final List<AutomatonTransition> between = new LinkedList<>();
        for (AutomatonTransition f : fromTrs) {
            if (toTrs.contains(f)) {
                between.add(f);
            }
        }

        return between;
    }

    /**
     * Intelligently place the nodes created by the given
     * {@link BreakdownIterationCommand} along a line or curve between the two
     * existing nodes from which this {@link BreakdownIterationCommand} was
     * generated.
     * 
     * @param graph
     *            the {@link GraphCanvasFX}
     * @param cmd
     *            the {@link BreakdownIterationCommand}
     * @return a {@link Point2D} array of the locations to place the new nodes,
     *         in order starting from the "from" end.
     */
    public static Point2D[] placeNodes(GraphCanvasFX graph,
            BreakdownIterationCommand cmd) {
        final AutomatonTransition transition = cmd.getOriginalTransition();
        final AutomatonState fromState = transition.getFrom();
        final AutomatonState toState = transition.getTo();
        final int numAddedStates = BreakdownUITools.getNumAddedStates(cmd);
        final boolean fromChoice = BreakdownUITools.wasChoiceTransition(cmd);
        final GraphNode fromNode = graph.lookupNode(fromState.getId());
        final GraphNode toNode = graph.lookupNode(toState.getId());

        Point2D[] points = new Point2D[numAddedStates];
        boolean above = (numAddedStates == 1); // Some arbitrary default value

        if (fromChoice) {
            Point2D edgeMid = graph
                    .lookupEdge(cmd.getOriginalTransition().getId())
                    .getEdgeMiddlePoint();
            final double dy = Math.abs(fromNode.getY() - toNode.getY());
            System.out.printf("PLACENODES: dy = %f  my = %f%n", dy,
                    edgeMid.getY());
            above = (edgeMid.getY() > dy);
        }

        if (numAddedStates == 1) {
            /* Form a triangle */
            points[0] = computeThirdPoint(fromNode.getX(), fromNode.getY(),
                    toNode.getX(), toNode.getY(), above);
        } else if (numAddedStates == 2) {
            /* Form a rectangle */
            points = computeRectanglePoints(fromNode.getX(), fromNode.getY(),
                    toNode.getX(), toNode.getY(), above);
        } else {
            for (int i = 0; i < numAddedStates; i++) {
                points[i] = randomPoint();
            }
        }

        return points;
    }

    /**
     * Intelligently place the nodes created by the given
     * {@link BreakdownSequenceCommand} along a line or curve between the two
     * existing nodes from which this {@link BreakdownSequenceCommand} was
     * generated.
     * 
     * @param graph
     *            the {@link GraphCanvasFX}
     * @param cmd
     *            the {@link BreakdownSequenceCommand}
     * @return a {@link Point2D} array of the locations to place the new nodes,
     *         in order starting from the "from" end.
     */
    public static Point2D[] placeNodes(GraphCanvasFX graph,
            BreakdownSequenceCommand cmd) {
        /*
         * TODO: Case where transCount is too high, and states are too close
         * together for edges to be rendered must be handled differenty.
         */
        final AutomatonTransition transition = cmd.getOriginalTransition();
        final GraphNode fromNode = graph
                .lookupNode(transition.getFrom().getId());
        final GraphNode toNode = graph.lookupNode(transition.getTo().getId());
        final int transCount = cmd.getNewTransitionsCount();
        final boolean fromChoice = BreakdownUITools.wasChoiceTransition(cmd);

        Point2D[] points = new Point2D[transCount];

        if (fromChoice) {
            /*
             * The nodes should be placed roughly along the curve that
             * previously existed.
             */
            Point2D edgeMid = graph
                    .lookupEdge(cmd.getOriginalTransition().getId())
                    .getEdgeMiddlePoint();
            // TODO
            for (int i = 0; i < transCount; i++) {
                points[i] = randomPoint();
            }
        } else {
            /* Didn't breakdown from a choice, just place along a line */
            double dxPerNode = (toNode.getX() - fromNode.getX()) / transCount;
            double dyPerNode = (toNode.getY() - fromNode.getY()) / transCount;
            double curX = fromNode.getX() + dxPerNode;
            double curY = fromNode.getY() + dyPerNode;

            for (int i = 0; i < transCount; i++) {
                points[i] = new Point2D(curX, curY);
                curX += dxPerNode;
                curY += dyPerNode;
            }
        }
        return points;
    }

    private static Point2D randomPoint() {
        return new Point2D(Math.random() * 800, Math.random() * 600);
    }

}
