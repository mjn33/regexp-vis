package view;

public class GraphEdge {
    final int mId;
    final GraphNode mFrom;
    final GraphNode mTo;
    String mText;

    double mLabelTestX1;
    double mLabelTestY1;
    double mLabelTestX2;
    double mLabelTestY2;

    double mLabelTestBisectX;
    double mLabelTestBisectY;

    /**
     * Whether or not this node is being rendered (set by layout 
     * calculation), due to too little space. Could in theory be replaced 
     * with an enumeration if more complex handling of too little space is 
     * implemented at a later date.
     */
    boolean mIsRendered;
    boolean mIsLine;

    double mArrowBaseX;
    double mArrowBaseY;
    double mStartPointX;
    double mStartPointY;
    double mArrowTipX;
    double mArrowTipY;

    double mTextX;
    double mTextY;
    double mTextAngle;
    /**
     * Calculated width of the text, negative if this hasn't been calculated yet
     */
    double mTextWidth;
    double mTextHeight;

    double mArcRadius;
    double mArcCenterX;
    double mArcCenterY;
    double mArcStartAngle;
    double mArcExtent;

    public GraphEdge(int id, GraphNode from, GraphNode to, String text)
    {
        mId = id;
        mFrom = from;
        mTo = to;
        mText = text;
        mTextWidth = -1.0;
        mTextHeight = -1.0;
    }
    
    public int getId()
    {
        return mId;
    }
    
}