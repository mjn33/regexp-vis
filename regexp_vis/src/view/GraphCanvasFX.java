package view;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

public final class GraphCanvasFX extends Canvas {
    private static class NodeEdgePair {
        final GraphNode mNode;
        /**
         * Edges which come from this node to another, different node
         */
        final ArrayList<GraphEdge> mEdges;
        /**
         * Edges which come from this node and "loop back" to this node
         */
        final ArrayList<GraphEdge> mLoopedEdges;

        NodeEdgePair(GraphNode node) {
            mNode = node;
            mEdges = new ArrayList<GraphEdge>();
            mLoopedEdges = new ArrayList<GraphEdge>();
        }

        void addEdge(GraphEdge e) {
            if (e.mFrom == e.mTo) {
                mLoopedEdges.add(e);
            } else {
                mEdges.add(e);
            }
        }

        void removeEdge(GraphEdge e) {
            if (e.mFrom == e.mTo) {
                mLoopedEdges.remove(e);
            } else {
                mEdges.remove(e);
            }
        }
    }

    /**
     * All nodes and edges
     */
    private HashMap<Integer, NodeEdgePair> mGraph;
    /**
     * Current node we are dragging, null if we aren't dragging anything
     */
    private GraphNode mDragNode;
    /**
     * Current edge we are dragging, null if we aren't dragging anything
     */
    private GraphEdge mDragEdge;
    /**
     * Coordinates of where the node was originally before we started dragging
     * it
     */
    private double mDragOrigX, mDragOrigY;
    /**
     * Coordinates of where the last onMouseDown event was
     */
    private double mDownX, mDownY;
    /**
     * The nodes with the highest x coordinate and highest y coordinate (note
     * these could be separate nodes). Used to handle placement when canvas is
     * resized too small.
     */
    private GraphNode mMaxPosXNode, mMaxPosYNode;
    /**
     * The graphics context we are using to do rendering
     */
    private GraphicsContext mGC;
    /**
     * The font we are using to render labels for edges
     */
    private Font mLabelFont;
    /**
     * The font we are using to render text on nodes
     */
    private Font mNodeFont;
    /**
     * The colour we are using to draw the background of nodes
     */
    private Color mNodeBorderColour;
    /**
     * The colour we are using to render text on nodes
     */
    private Color mNodeTextColour;
    /**
     * Event handler for when a node is clicked / double clicked
     */
    private EventHandler<GraphCanvasEvent> mNodeClickedHandler;
    /**
     * Event handler for a when an edge label is clicked / double clicked
     */
    private EventHandler<GraphCanvasEvent> mEdgeClickedHandler;
    /**
     * Event handler for a when neither an edge label or node is clicked (just
     * the background)
     */
    private EventHandler<GraphCanvasEvent> mBackgroundClickedHandler;
    /**
     * Event handler for when an edge is created in "create edge mode",
     * getTargetEdge() describes the edge added.
     */
    private EventHandler<GraphCanvasEvent> mCreatedEdgeHandler;

    /**
     * Whether "create edge mode" is active, see startCreateEdgeMode() for more
     * information.
     */
    private boolean mCreateEdgeModeActive;
    /**
     * For "create edge mode", which node is the edge we are creating coming
     * from?
     */
    private GraphNode mCreateEdgeFromNode;
    /**
     * The GraphEdge for the temporary edge used in "create edge mode", may be
     * null if the temporary edge hasn't got a destination target
     * (non-attached).
     */
    private GraphEdge mTempEdge;
    /**
     * Whether the temporary (non-attached) edge is rendered. If the line
     * doesn't have enough distance to render, this will be false.
     */
    private boolean mTempEdgeIsRendered;
    /**
     * Layout data for a non-attached temporary edge; where the line is drawn
     * from.
     */
    private Point2D mTempEdgeFrom;
    /**
     * Layout data for a non-attached temporary edge; where the base of the
     * arrow is.
     */
    private Point2D mTempEdgeArrowBase;
    /**
     * Layout data for a non-attached temporary edge; where the tip of the arrow
     * is.
     */
    private Point2D mTempEdgeTo;
    /**
     * The edge currently being hovered over.
     */
    private GraphEdge mHoverEdge;

    public GraphCanvasFX() {
        super();
        /// mNodes = new ArrayList<>();
        // mEdges = new ArrayList<>();
        mGraph = new HashMap<Integer, NodeEdgePair>();
        mGC = getGraphicsContext2D();
        mLabelFont = Font.font("Consolas", 16.0);
        mNodeFont = Font.font("Consolas", 16.0);
        mNodeBorderColour = Color.BLACK;
        mNodeTextColour = Color.BLACK;

        // Delegate event handling to class methods
        setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseMoved(event);
            }
        });

        setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseDragged(event);
            }
        });

        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMousePressed(event);
            }
        });

        setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseReleased(event);
            }
        });

        setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseClicked(event);
            }
        });
    }

    // Input event handling registration methods, tried to keep the same
    // convention as JavaFX's Node class

    public final void setOnNodeClicked(EventHandler<GraphCanvasEvent> handler) {
        mNodeClickedHandler = handler;
    }

    public final void setOnEdgeClicked(EventHandler<GraphCanvasEvent> handler) {
        mEdgeClickedHandler = handler;
    }

    public final void setOnBackgroundClicked(
            EventHandler<GraphCanvasEvent> handler) {
        mBackgroundClickedHandler = handler;
    }

    public final void setOnCreatedEdge(EventHandler<GraphCanvasEvent> handler) {
        mCreatedEdgeHandler = handler;
    }

    public GraphNode lookupNode(int id) {
        NodeEdgePair pair = mGraph.get(id);
        if (pair == null) {
            return null;
        } else {
            return pair.mNode;
        }
    }

    public GraphEdge lookupEdge(int id) {
        // TODO: Not too efficient, might be a good idea to store the edges in a
        // map as well
        for (NodeEdgePair pair : mGraph.values()) {
            for (GraphEdge e : pair.mEdges) {
                if (e.mId == id) {
                    return e;
                }
            }
            for (GraphEdge e : pair.mLoopedEdges) {
                if (e.mId == id) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Reposition a node in the bounds it's allowed to be in.
     * 
     * @param n The node to reposition
     */
    private void repositionNode(GraphNode n) {
        // Prevent placing the node outside the canvas, preventing the user from
        // ever dragging it again
        double x = n.mX, y = n.mY;
        // The checks are ordered this way, such that we will preferentially put
        // the starting state arrow out of the canvas, over putting the node
        // outside the canvas (in case of too little space)
        if (n.mUseStartStateStyle) {
            x = Math.max(n.mRadius + INITIAL_STATE_LINE_LENGTH + ARROW_LENGTH, x);
        } else {
            x = Math.max(n.mRadius, x);
        }
        x = Math.min(getWidth() - n.mRadius, x);

        y = Math.min(getHeight() - n.mRadius, y);
        y = Math.max(n.mRadius, y);

        n.mX = x;
        n.mY = y;
    }

    /**
     * Updates which nodes have the maximum x coordinate and the maximum y
     * coordinate. For handling a shrinking canvas.
     */
    private void updateMaxPosNodes() {
        mMaxPosXNode = null;
        mMaxPosYNode = null;
        for (NodeEdgePair pair : mGraph.values()) {
            if (mMaxPosXNode == null || mMaxPosXNode.mX < pair.mNode.mX) {
                mMaxPosXNode = pair.mNode;
            }
            if (mMaxPosYNode == null || mMaxPosYNode.mY < pair.mNode.mY) {
                mMaxPosYNode = pair.mNode;
            }
        }
    }

    /**
     * Add a node of a given id, to the canvas.
     *
     * @param id The ID of the node
     * @param x The x position of the centre of the node
     * @param y The y position of the centre of the node
     * @throws RuntimeException If a node with that ID already exists
     * @throws RuntimeException If a negative node ID was passed
     */
    public GraphNode addNode(int id, double x, double y) {
        // Need to check the ID, otherwise we would overwrite the previous value
        if (mGraph.containsKey(id)) {
            throw new RuntimeException("Node ID already exists");
        }

        if (id < 0) {
            throw new RuntimeException(
                    "Negative node IDs are reserved for internal use");
        }

        double radius = DEFAULT_NODE_RADIUS;

        GraphNode n = new GraphNode(id, x, y, radius, false, false,
                DEFAULT_NODE_BACKGROUND_COLOUR);
        repositionNode(n);
        updateMaxPosNodes();

        NodeEdgePair pair = new NodeEdgePair(n);
        mGraph.put(id, pair);

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
        return n;
    }

    /**
     * Set a given GraphNode to use the "final state" style, i.e. with a double
     * border.
     *
     * @param n The graph node in question
     * @param value Whether to use the "final state" style or not
     */
    public void setNodeUseFinalStyle(GraphNode n, boolean value) {
        // No layout data update required (for now at least)
        n.mUseFinalStateStyle = value;
    }

    /**
     * Set a given GraphNode to use the "start state" style, i.e. with a leading
     * arrow from the left
     *
     * @param n The graph node in question
     * @param value Whether to use the "start state" style or not
     */
    public void setNodeUseStartStyle(GraphNode n, boolean value) {
        // No layout data update required (for now at least)
        n.mUseStartStateStyle = value;
        if (value) {
            // May need to reposition node now that we have an arrow
            repositionNode(n);
            updateMaxPosNodes();
        }
    }

    /**
     * Set the background colour of a node
     *
     * @param n The graph node in question
     * @param colour The background colour
     */
    public void setNodeBackgroundColour(GraphNode n, Color colour) {
        n.mBackgroundColour = colour;
    }

    /**
     * Change the label text for a given edge
     *
     * @param edge The graph edge in question
     * @param text The new text for the label
     */
    public void setEdgeLabelText(GraphEdge edge, String text) {
        edge.mText = text;
        // Text metrics need to be recalculated
        edge.mTextWidth = -1.0;
        edge.mTextHeight = -1.0;
    }

    /**
     * Removes all nodes (and thus edges) from the graph.
     */
    public void removeAllNodes() {
        mGraph.clear();
    }

    /**
     * Removes the node with the given id, out-going edges will also be removed
     *
     * @param id The ID of the node to remove
     */
    public void removeNode(int id) {
        // FIXME: handle in-coming edges
        NodeEdgePair pair = mGraph.get(id);
        if (pair == null) {
            throw new RuntimeException("Node ID doesn't exist");
        }

        mGraph.remove(id);
    }

    /**
     * Add an edge between two nodes, with the given label text.
     *
     * @param id The ID of the new edge
     * @param from The GraphNode from which the edge comes from
     * @param to The GraphNode for which the edge goes to
     * @param text The text for the label
     * @return The newly created GraphEdge
     * @throws RuntimeException If the given GraphNode doesn't belong to this
     * canvas, or if an edge with the given ID already exists
     * @throws RuntimeException If a negative edge ID was passed
     */
    public GraphEdge addEdge(int id, GraphNode from, GraphNode to,
            String text) {
        NodeEdgePair pair = mGraph.get(from.mId);
        if (pair == null) {
            throw new RuntimeException("Node ID doesn't exist");
        }

        if (id < 0) {
            throw new RuntimeException(
                    "Negative edge IDs are reserved for internal use");
        }

        // Check a transition doesn't already exist
        GraphEdge oldEdge = lookupEdge(id);
        if (oldEdge != null) {
            throw new RuntimeException("Edge ID already exists");
        }

        GraphEdge e = new GraphEdge(id, from, to, text, DEFAULT_EDGE_LINE_COLOUR);
        pair.addEdge(e);

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
        return e;
    }

    public void removeEdge(int id) {
        // Find the edge
        GraphEdge oldEdge = lookupEdge(id);
        if (oldEdge == null) {
            throw new RuntimeException("Edge ID doesn't exist");
        }

        // And remove it
        NodeEdgePair pair = mGraph.get(oldEdge.mFrom.getId());
        pair.removeEdge(oldEdge);
    }

    /**
     * Starts "Create Edge Mode". Create edge mode disables moving of nodes, and
     * only allows drawing of edges between nodes. The mode doesn't create the
     * edges, merely trigger a callback (which should create an edge)
     *
     * @param fromNode The node from which the edge will be created
     */
    public void startCreateEdgeMode(GraphNode fromNode) {
        mCreateEdgeModeActive = true;
        mCreateEdgeFromNode = fromNode;
    }

    public void stopCreateEdgeMode() {
        if (mTempEdge != null) {
            NodeEdgePair pair = mGraph.get(mTempEdge.mFrom.getId());
            pair.removeEdge(mTempEdge);
            mTempEdge = null;
        }
        mCreateEdgeModeActive = false;
        mCreateEdgeFromNode = null;
    }

    /**
     * The default colour we are using to render the background of nodes
     */
    private final static Color DEFAULT_NODE_BACKGROUND_COLOUR = Color.WHITE;
    /**
     * The default colour we are using to render the lines/arcs for edges
     */
    private final static Color DEFAULT_EDGE_LINE_COLOUR = Color.BLACK;
    /**
     * The colour we are using to render the lines/arcs for "temporary" edges,
     * when in "create edge mode". "Temporary" since they are not a permanent
     * addition to the graph.
     */
    private final static Color TEMPORARY_EDGE_LINE_COLOUR = Color.GREY;
    /**
     * The default colour we are using to render the labels for edges
     */
    private final static Color DEFAULT_EDGE_LABEL_COLOUR = Color.BLACK;
    /**
     * the color for hovering a selected edge
     */
    private final static Color HOVERED_EDGE_LABEL_COLOUR = Color.BLUE;
    /**
     * The ID value we use for the temporary edge, negative since we don't want
     * it to conflict with externally added edges.
     */
    private final static int TEMPORARY_EDGE_ID = -1;
    /**
     * The default radius for nodes
     */
    private final static double DEFAULT_NODE_RADIUS = 20;
    /**
     * The distance between the two lines for the double border style of final
     * states
     */
    private final static double FINAL_STATE_BORDER_GAP = 3;
    /**
     * How long the line pointing to the initial node should be (not including
     * the arrow)
     */
    private final static double INITIAL_STATE_LINE_LENGTH = 50;
    /**
     * How long arrows should be
     */
    private final static double ARROW_LENGTH = 20;
    /**
     * How wide a base arrows should have
     */
    private final static double ARROW_WIDTH = 10;
    /**
     * How high the text should be rendered above an arc (centre point)
     */
    private final static double TEXT_POS_HEIGHT = 10;
    /**
     * The base distance looping edges should be from the node itself
     */
    private final static double ARC_LOOP_BASE_DISTANCE = 30;
    /**
     * The desired gap between arcs
     */
    private final static double ARC_GAP_SIZE = 30;
    /**
     * For looped edges, the cosine of half the angle of the arcs
     */
    private final static double ARC_LOOP_COS_HALF_ANGLE = Math.sqrt(3) * 0.5;
    /**
     * When we resize and discover the canvas is too small, multiply all x / y
     * coordinates by this factor.
     */
    private final static double RESIZE_SHRINK_FACTOR = 0.90;

    /**
     * Update the layout data for an edge that is a line, which the drawing code
     * uses later. The idea is to do more computationally expensive operations 
     * here, an only re-perform them when necessary, opposed to doing that for 
     * each time we redraw. E.g. moving one node will not change the layout data
     * for many other edges. 
     *
     * @param edge The edge to update the layout data for
     * @param textAngle The calculated angle the edge label text should be
     * rotated by
     */
    private void updateEdgeLineLayoutData(GraphEdge edge, double textAngle) {
        // Set "mIsRendered" here so we can exit the method early to avoid
        // rendering the line if problems arise
        edge.mIsRendered = false;

        GraphNode from = edge.mFrom;
        GraphNode to = edge.mTo;
        double x1 = from.mX;
        double y1 = from.mY;
        double x2 = to.mX;
        double y2 = to.mY;
        double lengthSqr = GraphUtils.vecLengthSqr(x2 - x1, y2 - y1);
        double l = Math.sqrt(lengthSqr);
        double midPointX = (x1 + x2) * 0.5;
        double midPointY = (y1 + y2) * 0.5;
        double S_E_gradientVecX = x2 - x1;
        double S_E_gradientVecY = y2 - y1;

        // Check distance is large enough to draw arrow
        if (l < from.mRadius + to.mRadius + ARROW_LENGTH) {
            System.out
                    .println("DEBUG: couldn't draw line, too little distance");
            return;
        }

        double invVecLength = 1
                / GraphUtils.vecLength(S_E_gradientVecX, S_E_gradientVecY);
        S_E_gradientVecX *= invVecLength;
        S_E_gradientVecY *= invVecLength;

        double G_C_gradientVecX = -S_E_gradientVecY;
        double G_C_gradientVecY = S_E_gradientVecX;

        double newX1, newY1, newX2, newY2, arrowX, arrowY;
        newX1 = x1 + S_E_gradientVecX * from.mRadius;
        newY1 = y1 + S_E_gradientVecY * from.mRadius;
        newX2 = x2 - S_E_gradientVecX * to.mRadius;
        newY2 = y2 - S_E_gradientVecY * to.mRadius;
        arrowX = newX2 - S_E_gradientVecX * ARROW_LENGTH;
        arrowY = newY2 - S_E_gradientVecY * ARROW_LENGTH;

        // Draw label
        double textX = midPointX - TEXT_POS_HEIGHT * G_C_gradientVecX;
        double textY = midPointY - TEXT_POS_HEIGHT * G_C_gradientVecY;
        updateEdgeLabelHitTestData(edge, textX, textY, S_E_gradientVecX,
                S_E_gradientVecY, G_C_gradientVecX, G_C_gradientVecY);

        edge.mArrowBaseX = arrowX;
        edge.mArrowBaseY = arrowY;
        edge.mStartPointX = newX1;
        edge.mStartPointY = newY1;
        edge.mArrowTipX = newX2;
        edge.mArrowTipY = newY2;

        edge.mIsRendered = true;
        edge.mIsLine = true;

        edge.mTextX = textX;
        edge.mTextY = textY;
        edge.mTextAngle = textAngle;

        edge.mMiddlePoint = new Point2D(midPointX, midPointY);
    }

    /**
     * Update the layout data for an edge that is an arc. See 
     * updateEdgeLineLayoutData() for more information
     *
     * @param edge The edge to update the layout data for
     * @param height How high the arc should bend at the midpoint. Negative 
     * values allowed, in which case the arc bends in the opposite direction
     * @param textAngle The calculated angle the edge label text should be
     * rotated by
     */
    private void updateEdgeArcLayoutData(GraphEdge edge, double height,
            double textAngle) {
        // Set "mIsRendered" here so we can exit the method early to avoid
        // rendering the arc if problems arise
        edge.mIsRendered = false;

        GraphNode from = edge.mFrom;
        GraphNode to = edge.mTo;
        double x1 = from.mX;
        double y1 = from.mY;
        double x2 = to.mX;
        double y2 = to.mY;
        double absHeight = Math.abs(height);
        double lengthSqr = GraphUtils.vecLengthSqr(x2 - x1, y2 - y1);
        double midPointX = (x1 + x2) * 0.5;
        double midPointY = (y1 + y2) * 0.5;
        double radius = (0.25 * lengthSqr + height * height) / (2 * absHeight);

        double S_E_gradientVecX = x2 - x1;
        double S_E_gradientVecY = y2 - y1;
        // Normalise to get a unit vector (so it is easy to move a given
        // distance along the line)
        double invVecLength = 1
                / GraphUtils.vecLength(S_E_gradientVecX, S_E_gradientVecY);
        S_E_gradientVecX *= invVecLength;
        S_E_gradientVecY *= invVecLength;
        double G_C_gradientVecX = -S_E_gradientVecY;
        double G_C_gradientVecY = S_E_gradientVecX;
        // Handle negative height, arc goes in the opposite direction
        if (height < 0) {
            G_C_gradientVecX = S_E_gradientVecY;
            G_C_gradientVecY = -S_E_gradientVecX;
        }

        // TODO: document: work around: assumption that radius >= height
        double circleX = midPointX + (radius - absHeight) * G_C_gradientVecX;
        double circleY = midPointY + (radius - absHeight) * G_C_gradientVecY;

        double tmpGradientX = y2 - y1;
        double tmpGradientY = x1 - x2;
        // Handle negative height, arc goes in the opposite direction
        if (height < 0) {
            tmpGradientX = y1 - y2;
            tmpGradientY = x2 - x1;
        }
        double[] results = GraphUtils.calcCircleIntersectionPoints(radius,
                x1 - circleX, y1 - circleY, from.mRadius);
        results = GraphUtils.filterArcIntersectionPoint(results, x1 - circleX,
                y1 - circleY, tmpGradientX, tmpGradientY);
        double newX1, newY1;
        if (results == null) {
            System.out.println(
                    "DEBUG: couldn't draw arc, probably too little distance (1)");
            return;
        } else {
            newX1 = results[0] + circleX;
            newY1 = results[1] + circleY;
        }

        tmpGradientX = y2 - newY1;
        tmpGradientY = newX1 - x2;
        // Handle negative height, arc goes in the opposite direction
        if (height < 0) {
            tmpGradientX = newY1 - y2;
            tmpGradientY = x2 - newX1;
        }
        results = GraphUtils.calcCircleIntersectionPoints(radius, x2 - circleX,
                y2 - circleY, to.mRadius);
        results = GraphUtils.filterArcIntersectionPoint(results, newX1
                - circleX, newY1 - circleY, tmpGradientX, tmpGradientY);
        double newX2, newY2;
        if (results == null) {
            System.out.println(
                    "DEBUG: couldn't draw arc, probably too little distance (2)");
            return;
        } else {
            newX2 = results[0] + circleX;
            newY2 = results[1] + circleY;
        }

        tmpGradientX = newY2 - newY1;
        tmpGradientY = newX1 - newX2;
        // Handle negative height, arc goes in the opposite direction
        if (height < 0) {
            tmpGradientX = newY1 - newY2;
            tmpGradientY = newX2 - newX1;
        }
        results = GraphUtils.calcCircleIntersectionPoints(radius,
                newX2 - circleX, newY2 - circleY, ARROW_LENGTH);
        results = GraphUtils.filterArcIntersectionPoint(results, newX1
                - circleX, newY1 - circleY, tmpGradientX, tmpGradientY);
        double arrowX, arrowY;
        if (results == null) {
            System.out.println(
                    "DEBUG: couldn't draw arc, probably too little distance (3)");
            return;
        } else {
            arrowX = results[0] + circleX;
            arrowY = results[1] + circleY;
        }

        // Draw label
        double peakX = midPointX - absHeight * G_C_gradientVecX;
        double peakY = midPointY - absHeight * G_C_gradientVecY;
        double textX = midPointX
                - (absHeight + TEXT_POS_HEIGHT) * G_C_gradientVecX;
        double textY = midPointY
                - (absHeight + TEXT_POS_HEIGHT) * G_C_gradientVecY;
        updateEdgeLabelHitTestData(edge, textX, textY, S_E_gradientVecX,
                S_E_gradientVecY, G_C_gradientVecX, G_C_gradientVecY);

        double startAngle = GraphUtils.calcAngleOnCircle(newX1 - circleX,
                newY1 - circleY, radius);
        double endAngle = GraphUtils.calcAngleOnCircle(arrowX - circleX,
                arrowY - circleY, radius);
        double arcExtent = GraphUtils.arcCalcArcExtent(startAngle, endAngle);

        edge.mArrowBaseX = arrowX;
        edge.mArrowBaseY = arrowY;
        edge.mStartPointX = newX1;
        edge.mStartPointY = newY1;
        edge.mArrowTipX = newX2;
        edge.mArrowTipY = newY2;

        edge.mIsRendered = true;
        edge.mIsLine = false;

        edge.mTextX = textX;
        edge.mTextY = textY;
        edge.mTextAngle = textAngle;

        edge.mArcRadius = radius;
        edge.mArcCenterX = circleX;
        edge.mArcCenterY = circleY;
        edge.mArcStartAngle = startAngle;
        edge.mArcExtent = arcExtent;

        edge.mMiddlePoint = new Point2D(peakX, peakY);
    }

    /**
     * Update the layout data for the looped edge of a node (node from == node 
     * to). See updateEdgeLineLayoutData() for more information.
     *
     * @param n The node for which the edges belong to
     * @param edges The edges to update the layout data for
     */
    private void updateEdgesLoopedLayoutData(GraphNode n,
            ArrayList<GraphEdge> edges) {
        double[] results = GraphUtils.vectorsAround(n.mLoopDirVecX,
                n.mLoopDirVecY, ARC_LOOP_COS_HALF_ANGLE);
        double startVecX = results[0];
        double startVecY = results[1];
        double endVecX = results[2];
        double endVecY = results[3];

        double startAngle = GraphUtils.calcAngleOnCircle(startVecX, startVecY,
                1);
        double endAngle = GraphUtils.calcAngleOnCircle(endVecX, endVecY, 1);

        n.mStartAngle = startAngle;
        n.mArcExtent = GraphUtils.arcCalcArcExtent(startAngle, endAngle);

        n.mStartLineP1 = new Point2D(n.mX + startVecX * n.mRadius,
                n.mY + startVecY * n.mRadius);
        n.mStartLineDir = new Point2D(startVecX, startVecY);

        n.mEndLineP1 = new Point2D(n.mX + endVecX * n.mRadius,
                n.mY + endVecY * n.mRadius);
        n.mEndLineDir = new Point2D(endVecX, endVecY);

        double loopDirNormalVecX = -n.mLoopDirVecY;
        double loopDirNormalVecY = n.mLoopDirVecX;

        double textAngle = GraphUtils.calcTextAngle(loopDirNormalVecX,
                loopDirNormalVecY, 1);// GraphUtils.calcAngleOnCircle(n.mLoopDirNormalVecX,
                                      // n.mLoopDirNormalVecY, 1);
        double tmpRadius = n.mRadius + ARC_LOOP_BASE_DISTANCE;
        for (GraphEdge edge : edges) {
            double textX = n.mX
                    + (tmpRadius + TEXT_POS_HEIGHT) * n.mLoopDirVecX;
            double textY = n.mY
                    + (tmpRadius + TEXT_POS_HEIGHT) * n.mLoopDirVecY;

            edge.mIsLine = false;
            edge.mIsRendered = true;

            edge.mTextX = textX;
            edge.mTextY = textY;
            edge.mTextAngle = textAngle;

            edge.mArcRadius = tmpRadius;
            edge.mArcCenterX = n.mX;
            edge.mArcCenterY = n.mY;

            double peakX = n.mX + tmpRadius * n.mLoopDirVecX;
            double peakY = n.mY + tmpRadius * n.mLoopDirVecY;
            edge.mMiddlePoint = new Point2D(peakX, peakY);

            updateEdgeLabelHitTestData(edge, textX, textY, loopDirNormalVecX,
                    loopDirNormalVecY, n.mLoopDirVecX, n.mLoopDirVecY);
            tmpRadius += ARC_GAP_SIZE;
        }
    }

    /**
     * Update the layout data for all edges between two nodes, in both 
     * directions. See updateEdgeLineLayoutData() for more information.
     *
     * @param pair1 The node-edge pair for a node
     * @param pair2 The node-edge pair for another node
     */
    private void updateConnectionLayoutData(NodeEdgePair pair1,
            NodeEdgePair pair2) {
        GraphNode n1 = pair1.mNode;
        GraphNode n2 = pair2.mNode;

        // Count the number of edges between these two nodes
        int count = 0;
        for (GraphEdge e : pair1.mEdges) {
            if (e.mTo == n2) {
                count++;
            }
        }
        for (GraphEdge e : pair2.mEdges) {
            if (e.mTo == n1) {
                count++;
            }
        }

        // Mid edge index refers to the middle edge which should be rendered as
        // a straight line, remains -1 for an even number of connections
        int midEdgeIdx = -1;
        if ((count % 2) != 0) {
            // Odd number of connections
            midEdgeIdx = count / 2;
        }

        double height = (count - 1) * ARC_GAP_SIZE * 0.5;
        double length = GraphUtils.vecLength(n2.mX - n1.mX, n2.mY - n1.mY);
        double textAngle = GraphUtils.calcTextAngle(n2.mX - n1.mX,
                n2.mY - n1.mY, length);
        int i = 0;

        for (GraphEdge e : pair1.mEdges) {
            if (e.mTo != n2) {
                continue;
            }
            if (i != midEdgeIdx) {
                updateEdgeArcLayoutData(e, height - i * ARC_GAP_SIZE,
                        textAngle);
            } else {
                updateEdgeLineLayoutData(e, textAngle);
            }
            i++;
        }
        for (GraphEdge e : pair2.mEdges) {
            if (e.mTo != n1) {
                continue;
            }

            if (i != midEdgeIdx) {
                // Negative since the arc is going in the opposite direction
                updateEdgeArcLayoutData(e, -(height - i * ARC_GAP_SIZE),
                        textAngle);
            } else {
                updateEdgeLineLayoutData(e, textAngle);
            }
            i++;
        }
    }

    /**
     * Update temporary edge (non-attached) layout data.
     */
    private void updateTempEdgeLayoutData() {
        if (!mCreateEdgeModeActive) {
            return;
        }

        mTempEdgeIsRendered = true;
        GraphNode n = mCreateEdgeFromNode;
        Point2D nodePos = new Point2D(n.mX, n.mY);
        Point2D lineDir = mTempEdgeTo.subtract(nodePos);
        if (lineDir.distance(0, 0) < n.mRadius + ARROW_LENGTH) {
            // Not enough room to draw this
            mTempEdgeIsRendered = false;
        }

        lineDir = lineDir.normalize();
        mTempEdgeFrom = nodePos.add(lineDir.multiply(n.mRadius));
        mTempEdgeArrowBase = mTempEdgeTo
                .subtract(lineDir.multiply(ARROW_LENGTH));
    }

    /**
     * Update all layout data, usually this is not needed but for simplicity 
     * sake / debugging this can be used to ensure layout data gets updated 
     * properly. See updateEdgeLineLayoutData() for more information.
     */
    private void updateAllLayoutData() {
        for (NodeEdgePair pair : mGraph.values()) {
            GraphNode n = pair.mNode;

            updateEdgesLoopedLayoutData(n, pair.mLoopedEdges);
        }

        Object[] tmpPairs = mGraph.values().toArray();

        for (int i = 0; i < tmpPairs.length; i++) {
            for (int j = i + 1; j < tmpPairs.length; j++) {
                updateConnectionLayoutData((NodeEdgePair) tmpPairs[i],
                        (NodeEdgePair) tmpPairs[j]);
            }
        }

        updateTempEdgeLayoutData();
    }

    /**
     * Draw an edge which is a line, layout data calculated in 
     * updateEdgeLineLayoutData().
     *
     * @param edge The edge to draw
     */
    private void drawEdgeLine(GraphEdge edge) {
        if (edge.mIsHoveredOver) {
            mGC.setFill(HOVERED_EDGE_LABEL_COLOUR);
        } else {
            mGC.setFill(DEFAULT_EDGE_LABEL_COLOUR);
        }
        mGC.setFontSmoothingType(FontSmoothingType.LCD);
        mGC.setFont(mLabelFont);
        mGC.setTextAlign(TextAlignment.CENTER);
        mGC.setTextBaseline(VPos.CENTER);

        if (edge.mText != null) {
            GraphUtils.setGcRotation(mGC, -edge.mTextAngle, edge.mTextX,
                    edge.mTextY);
            mGC.fillText(edge.mText, edge.mTextX, edge.mTextY);
            // Turn off rotation, identity transformation
            mGC.setTransform(new Affine());
        }

        mGC.setStroke(edge.mLineColour);
        mGC.setFill(edge.mLineColour);
        mGC.strokeLine(edge.mStartPointX, edge.mStartPointY, edge.mArrowBaseX,
                edge.mArrowBaseY);

        GraphUtils.fillArrowHead(mGC, edge.mArrowBaseX, edge.mArrowBaseY,
                edge.mArrowTipX, edge.mArrowTipY, ARROW_WIDTH);
    }

    /**
     * Draw an edge which is an arc, layout data calculated in 
     * updateEdgeArcLayoutData().
     *
     * @param edge The edge to draw
     */
    private void drawEdgeArc(GraphEdge edge) {
        if (edge.mIsHoveredOver) {
            mGC.setFill(HOVERED_EDGE_LABEL_COLOUR);
        } else {
            mGC.setFill(DEFAULT_EDGE_LABEL_COLOUR);
        }
        mGC.setFontSmoothingType(FontSmoothingType.LCD);
        mGC.setFont(mLabelFont);
        mGC.setTextAlign(TextAlignment.CENTER);
        mGC.setTextBaseline(VPos.CENTER);

        if (edge.mText != null) {
            GraphUtils.setGcRotation(mGC, -edge.mTextAngle, edge.mTextX,
                    edge.mTextY);
            mGC.fillText(edge.mText, edge.mTextX, edge.mTextY);
            // Turn off rotation, identity transformation
            mGC.setTransform(new Affine());
        }

        mGC.setStroke(edge.mLineColour);
        mGC.setFill(edge.mLineColour);
        mGC.strokeArc(edge.mArcCenterX - edge.mArcRadius,
                edge.mArcCenterY - edge.mArcRadius, edge.mArcRadius * 2,
                edge.mArcRadius * 2, edge.mArcStartAngle, edge.mArcExtent,
                ArcType.OPEN);

        GraphUtils.fillArrowHead(mGC, edge.mArrowBaseX, edge.mArrowBaseY,
                edge.mArrowTipX, edge.mArrowTipY, ARROW_WIDTH);
    }

    /**
     * Draw an edge, checks the type of the edge to call the correct drawing 
     * method.
     *
     * @param edge The edge to draw
     */
    private void drawEdge(GraphEdge edge) {
        if (!edge.mIsRendered) {
            return;
        }

        if (edge.mIsLine) {
            drawEdgeLine(edge);
        } else {
            drawEdgeArc(edge);
        }
    }

    /**
     * Draw the looped edges for a GraphNode, layout data calculated in 
     * updateEdgesLoopedLayoutData().
     *
     * @param n The graph node the edges belong to
     * @param edges The looped edges of the graph node to draw
     */
    private void drawEdgesLooped(GraphNode n, ArrayList<GraphEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return;
        }

        GraphEdge prevEdge = null;
        for (GraphEdge edge : edges) {
            mGC.setStroke(edge.mLineColour);
            Point2D fromStart, toStart, fromEnd, toEnd;

            if (prevEdge == null) {
                fromStart = n.mStartLineP1;
                fromEnd = n.mEndLineP1;
            } else {
                fromStart = n.mStartLineP1.add(n.mStartLineDir
                        .multiply(prevEdge.mArcRadius - n.mRadius));
                fromEnd = n.mEndLineP1.add(n.mEndLineDir
                        .multiply(prevEdge.mArcRadius - n.mRadius));
            }

            toStart = n.mStartLineP1
                    .add(n.mStartLineDir.multiply(edge.mArcRadius - n.mRadius));
            toEnd = n.mEndLineP1
                    .add(n.mEndLineDir.multiply(edge.mArcRadius - n.mRadius));

            mGC.strokeLine(fromStart.getX(), fromStart.getY(), toStart.getX(),
                    toStart.getY());
            mGC.strokeLine(fromEnd.getX(), fromEnd.getY(), toEnd.getX(),
                    toEnd.getY());
            mGC.strokeArc(n.mX - edge.mArcRadius, n.mY - edge.mArcRadius,
                    edge.mArcRadius * 2, edge.mArcRadius * 2, n.mStartAngle,
                    n.mArcExtent, ArcType.OPEN);

            prevEdge = edge;
        }

        mGC.setFontSmoothingType(FontSmoothingType.LCD);
        mGC.setFont(mLabelFont);
        mGC.setTextAlign(TextAlignment.CENTER);
        mGC.setTextBaseline(VPos.CENTER);

        for (GraphEdge edge : edges) {
            if (edge.mText != null) {
                GraphUtils.setGcRotation(mGC, -edge.mTextAngle, edge.mTextX,
                        edge.mTextY);
                if (edge.mIsHoveredOver) {
                    mGC.setFill(HOVERED_EDGE_LABEL_COLOUR);
                } else {
                    mGC.setFill(DEFAULT_EDGE_LABEL_COLOUR);
                }
                mGC.fillText(edge.mText, edge.mTextX, edge.mTextY);
            }
        }

        mGC.setTransform(new Affine());
    }

    /**
     * Draw temporary edge (if not attached to another node).
     */
    private void drawTemporaryEdge() {
        if (!mCreateEdgeModeActive || mTempEdge != null
                || !mTempEdgeIsRendered) {
            return;
        }

        mGC.setFill(TEMPORARY_EDGE_LINE_COLOUR);
        mGC.setStroke(TEMPORARY_EDGE_LINE_COLOUR);

        mGC.strokeLine(mTempEdgeFrom.getX(), mTempEdgeFrom.getY(),
                mTempEdgeArrowBase.getX(), mTempEdgeArrowBase.getY());

        GraphUtils.fillArrowHead(mGC, mTempEdgeArrowBase.getX(),
                mTempEdgeArrowBase.getY(), mTempEdgeTo.getX(),
                mTempEdgeTo.getY(), ARROW_WIDTH);
    }

    /**
     * Draw a node, no corresponding function to update layout data, as non is 
     * needed yet.
     *
     * @param n The node to draw
     */
    void drawNode(GraphNode n) {
        mGC.setFill(n.mBackgroundColour);
        mGC.setStroke(mNodeBorderColour);

        GraphUtils.fillCircleCentred(mGC, n.mX, n.mY, n.mRadius);
        GraphUtils.strokeCircleCentred(mGC, n.mX, n.mY, n.mRadius);

        if (n.mUseFinalStateStyle) {
            GraphUtils.strokeCircleCentred(mGC, n.mX, n.mY,
                    n.mRadius - FINAL_STATE_BORDER_GAP);
        }

        if (n.mUseStartStateStyle) {
            double startX = n.mX - n.mRadius - ARROW_LENGTH
                    - INITIAL_STATE_LINE_LENGTH;
            double arrowBaseX = n.mX - n.mRadius - ARROW_LENGTH;
            double arrowTipX = n.mX - n.mRadius;

            mGC.setFill(DEFAULT_EDGE_LABEL_COLOUR);
            mGC.setStroke(DEFAULT_EDGE_LABEL_COLOUR);
            mGC.strokeLine(startX, n.mY, arrowBaseX, n.mY);
            GraphUtils.fillArrowHead(mGC, arrowBaseX, n.mY, arrowTipX, n.mY,
                    ARROW_WIDTH);
        }

        mGC.setFill(mNodeTextColour);
        mGC.setFontSmoothingType(FontSmoothingType.LCD);
        mGC.setFont(mNodeFont);
        mGC.setTextAlign(TextAlignment.CENTER);
        mGC.setTextBaseline(VPos.CENTER);
        mGC.fillText(Integer.toString(n.mId), n.mX, n.mY);
    }

    private void updateEdgeLabelHitTestData(GraphEdge edge, double textX,
            double textY, double textDirVecX, double textDirVecY,
            double textNormalVecX, double textNormalVecY) {
        if (edge.mTextWidth < 0.0 && edge.mText != null) {
            // http://stackoverflow.com/a/13020490
            Text text = new Text(edge.mText);
            text.setFont(mLabelFont);
            new Scene(new Group(text));

            text.applyCss();

            Bounds bounds = text.getLayoutBounds();
            edge.mTextWidth = bounds.getWidth();
            edge.mTextHeight = bounds.getHeight();

        }

        edge.mLabelTestX1 = textX + textDirVecX * edge.mTextWidth * 0.5
                - textNormalVecX * edge.mTextHeight * 0.5;
        edge.mLabelTestY1 = textY + textDirVecY * edge.mTextWidth * 0.5
                - textNormalVecY * edge.mTextHeight * 0.5;

        edge.mLabelTestX2 = textX - textDirVecX * edge.mTextWidth * 0.5
                + textNormalVecX * edge.mTextHeight * 0.5;
        edge.mLabelTestY2 = textY - textDirVecY * edge.mTextWidth * 0.5
                + textNormalVecY * edge.mTextHeight * 0.5;

        edge.mLabelTestBisectX = -textDirVecX + textNormalVecX;
        edge.mLabelTestBisectY = -textDirVecY + textNormalVecY;

        double invVecLength = 1 / GraphUtils.vecLength(edge.mLabelTestBisectX,
                edge.mLabelTestBisectY);
        edge.mLabelTestBisectX *= invVecLength;
        edge.mLabelTestBisectY *= invVecLength;
    }

    /**
     * Updates temporary edge from mouse moved event
     *
     * @param event The mouse event
     */
    private void handleTemporaryEdge(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        mTempEdgeTo = new Point2D(x, y);
        GraphNode n = findNodeHit(x, y);
        NodeEdgePair pair = mGraph.get(mCreateEdgeFromNode.getId());

        if (mTempEdge != null && mTempEdge.mTo != n) {
            // Remove old edge
            pair.removeEdge(mTempEdge);
            mTempEdge = null;
        }

        if (mTempEdge == null && n != null) {
            // Add new edge
            mTempEdge = new GraphEdge(TEMPORARY_EDGE_ID, mCreateEdgeFromNode, n,
                    null, TEMPORARY_EDGE_LINE_COLOUR);
            pair.addEdge(mTempEdge);
        }

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
    }

    private void onMouseMoved(MouseEvent event) {
        if (mCreateEdgeModeActive) {
            handleTemporaryEdge(event);
        } else {
            // Update the currently hovered over edge
            if (mHoverEdge != null){
                mHoverEdge.mIsHoveredOver = false;
            }
            GraphEdge e = null;
            double x = event.getX();
            double y = event.getY();
            boolean redraw = false;

            // Nodes block edge highlighting
            GraphNode n = findNodeHit(x, y);
            if (n == null) {
                e = findEdgeLabelHit(x, y);
            }
            if (e != mHoverEdge) {
                // Edge has changed, need to redraw
                redraw = true;
            }

            mHoverEdge = e;
            if (mHoverEdge != null) {
                mHoverEdge.mIsHoveredOver = true;
            }
            if (redraw) {
                doRedraw();
            }

        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (mDragNode != null) {
            double newX = (event.getX() - mDownX) + mDragOrigX;
            double newY = (event.getY() - mDownY) + mDragOrigY;

            mDragNode.mX = newX;
            mDragNode.mY = newY;
            repositionNode(mDragNode);
            updateMaxPosNodes();
        } else if (mDragEdge != null && mDragEdge.mFrom == mDragEdge.mTo) {
            // Trying to drag a looped edge, update loop direction vector
            System.out.println("Dragging edge");
            GraphNode n = mDragEdge.mFrom;
            n.mLoopDirVecX = event.getX() - n.mX;
            n.mLoopDirVecY = event.getY() - n.mY;
            double invVecLength = 1
                    / GraphUtils.vecLength(n.mLoopDirVecX, n.mLoopDirVecY);
            n.mLoopDirVecX *= invVecLength;
            n.mLoopDirVecY *= invVecLength;
        }

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
    }

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            // Left-click to drag
            return;
        }
        requestFocus();

        mDownX = event.getX();
        mDownY = event.getY();

        if (mCreateEdgeModeActive) {
            // Disallow moving nodes / edges in this mode
            return;
        }

        // Check for a hit on a node first, they take preference
        GraphNode n = findNodeHit(mDownX, mDownY);
        GraphEdge e = findEdgeLabelHit(mDownX, mDownY);
        if (n != null) {
            mDragNode = n;
            mDragOrigX = n.mX;
            mDragOrigY = n.mY;
            System.out.println("onMousePressed, hit NODE " + n);
        } else if (e != null) {
            mDragEdge = e;
            System.out.println("onMousePressed, hit EDGE " + e + " id = "
                    + e.getId() + " label text = \"" + e.mText + "\"");
        }

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
    }

    private void onMouseReleased(MouseEvent event) {
        System.out.println(
                "onMouseUp, X = " + event.getX() + ", Y = " + event.getY());
        mDragNode = null;
        mDragEdge = null;

        // TODO: be smarter about this (do minimum amount of layout
        // recalculation necessary)
        updateAllLayoutData();
        doRedraw();
    }

    private void onMouseClicked(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        System.out.println("onMouseClicked, X = " + x + ", Y = "
                + y + ", clickCount = " + event.getClickCount());

        if (mCreateEdgeModeActive) {
            if (mTempEdge != null) {
                NodeEdgePair pair = mGraph.get(mTempEdge.mFrom.getId());
                pair.removeEdge(mTempEdge);
                if (mCreatedEdgeHandler != null) {
                    mCreatedEdgeHandler.handle(
                            new GraphCanvasEvent(event, null, mTempEdge));
                }
                mTempEdge = null;
            }
            mCreateEdgeModeActive = false;
            mCreateEdgeFromNode = null;
            // TODO: be smarter about this (do minimum amount of layout
            // recalculation necessary)
            updateAllLayoutData();
            doRedraw();
            return;
        }

        // Check for a hit on a node first, they take preference
        GraphNode n = findNodeHit(x, y);
        GraphEdge e = findEdgeLabelHit(x, y);
        if (n != null) {
            if (mNodeClickedHandler != null) {
                mNodeClickedHandler
                        .handle(new GraphCanvasEvent(event, n, null));
            }
        } else if (e != null) {
            if (mEdgeClickedHandler != null) {
                mEdgeClickedHandler
                        .handle(new GraphCanvasEvent(event, null, e));
            }
        } else {
            if (mBackgroundClickedHandler != null) {
                mNodeClickedHandler
                        .handle(new GraphCanvasEvent(event, null, null));
            }
        }
    }

    /**
     * Finds which node has been hit at the given coordinates.
     *
     * @param x The x coordinate of the hit test
     * @param y The y coordinate of the hit test
     * @return The node which was hit, or null if no node was hit
     */
    private GraphNode findNodeHit(double x, double y) {
        for (NodeEdgePair pair : mGraph.values()) {
            if (nodeHitTest(pair.mNode, x, y)) {
                return pair.mNode;
            }
        }
        return null;
    }

    /**
     * Finds which edge has been hit at the given coordinates.
     *
     * @param x The x coordinate of the hit test
     * @param y The y coordinate of the hit test
     * @return The edge which was hit, or null if no edge was hit
     */
    private GraphEdge findEdgeLabelHit(double x, double y) {
        for (NodeEdgePair pair : mGraph.values()) {
            for (GraphEdge e : pair.mLoopedEdges) {
                if (edgeLabelHitTest(e, x, y)) {
                    return e;
                }
            }
            for (GraphEdge e : pair.mEdges) {
                if (edgeLabelHitTest(e, x, y)) {
                    return e;
                }
            }
        }
        return null;
    }

    private boolean nodeHitTest(GraphNode n, double x, double y) {
        double distSqr = (x - n.mX) * (x - n.mX) + (y - n.mY) * (y - n.mY);
        if (distSqr <= n.mRadius * n.mRadius) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Cosine of 45 degrees, used by edgeLabelHitTest
     */
    private static final double COS_45_DEG = Math.sqrt(2) * 0.5;

    private boolean edgeLabelHitTest(GraphEdge e, double x, double y) {
        if (!e.mIsRendered || e.mText == null) {
            // Can't hit an edge we aren't rendering
            // Can't hit a label with no text
            return false;
        }

        double x1 = x - e.mLabelTestX1;
        double y1 = y - e.mLabelTestY1;
        double cosAlpha = x1 * e.mLabelTestBisectX + y1 * e.mLabelTestBisectY;
        cosAlpha /= GraphUtils.vecLength(x1, y1);
        if (cosAlpha < COS_45_DEG) {
            return false;
        }

        x1 = x - e.mLabelTestX2;
        y1 = y - e.mLabelTestY2;
        // Negative since we want mLabelTestBisect{X,Y} vector to be in the
        // opposite direction
        cosAlpha = -(x1 * e.mLabelTestBisectX + y1 * e.mLabelTestBisectY);
        cosAlpha /= GraphUtils.vecLength(x1, y1);
        if (cosAlpha < COS_45_DEG) {
            return false;
        }

        return true;
    }

    /**
     * Main redrawing routine, doesn't update layout data for efficiency.
     */
    public void doRedraw() {
        mGC.clearRect(0, 0, getWidth(), getHeight());
        mGC.setFill(Color.WHITE);
        mGC.setStroke(Color.BLACK);

        // Remember, drawing coordinates has the origin in the
        // top left corner, with y increasing going downward, the maths in
        // places accounts for this (where it matters)

        // Draw nodes first
        for (NodeEdgePair pair : mGraph.values()) {
            drawNode(pair.mNode);
        }

        // Draw looped edges
        for (NodeEdgePair pair : mGraph.values()) {
            drawEdgesLooped(pair.mNode, pair.mLoopedEdges);
        }

        // Draw edges
        for (NodeEdgePair pair : mGraph.values()) {
            for (GraphEdge e : pair.mEdges) {
                drawEdge(e);
            }
        }

        drawTemporaryEdge();
    }

    // Overrides required for automatic resizing
    // See stackoverflow: http://stackoverflow.com/a/34263646

    @Override
    public double minHeight(double width) {
        return 0;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double minWidth(double height) {
        return 0;
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        super.setWidth(width);
        super.setHeight(height);

        // Need to recalc layout data if we move everything
        boolean recalcLayoutData = false;
        if (mMaxPosXNode != null && mMaxPosXNode.mX > width) {
            // Scale all node x coordinates by RESIZE_SHRINK_FACTOR
            for (NodeEdgePair pair : mGraph.values()) {
                pair.mNode.mX *= RESIZE_SHRINK_FACTOR;
                repositionNode(pair.mNode);
            }
            recalcLayoutData = true;
        }
        if (mMaxPosYNode != null && mMaxPosYNode.mY > height) {
            // Scale all node y coordinates by RESIZE_SHRINK_FACTOR
            for (NodeEdgePair pair : mGraph.values()) {
                pair.mNode.mY *= RESIZE_SHRINK_FACTOR;
                repositionNode(pair.mNode);
            }
            recalcLayoutData = true;
        }

        if (recalcLayoutData) {
            // This is why updateMaxPosNodes() isn't in repositionNode(), otherwise
            // we would have n^2 time complexity to the number of nodes
            updateMaxPosNodes();
            updateAllLayoutData();
        }

        doRedraw();
    }
}
