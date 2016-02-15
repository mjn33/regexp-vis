package view;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class GraphNode {
    /**
     * The ID for this node
     */
    final int mId;
    /**
     * Coordinates of the centre of this node
     */
    double mX, mY;
    /**
     * The radius this node will be rendered with
     */
    double mRadius;
    boolean mUseStartStateStyle;
    boolean mUseFinalStateStyle;

    double mLoopDirVecX;
    double mLoopDirVecY;
    //double mLoopDirNormalVecX;
    //double mLoopDirNormalVecY;

    Point2D mStartLineP1;
    Point2D mStartLineDir;
    Point2D mEndLineP1;
    Point2D mEndLineDir;

    double mStartAngle;
    double mArcExtent;

    Color mBackgroundColour;

    public GraphNode(int id, double x, double y, double r, boolean startStyle,
            boolean finalStyle, Color backgroundColour)
    {
        mId = id;
        mX = x;
        mY = y;
        mRadius = r;
        mUseStartStateStyle = startStyle;
        mUseFinalStateStyle = finalStyle;

        // Loops are on top by default
        mLoopDirVecX = 0;
        mLoopDirVecY = -1;
        //mLoopDirNormalVecX = -mLoopDirVecY;
        //mLoopDirNormalVecY = mLoopDirVecX;

        mBackgroundColour = backgroundColour;
    }

    public double getX()
    {
        return mX;
    }

    public double getY()
    {
        return mY;
    }
    
    public int getId()
    {
        return mId;
    }
}
