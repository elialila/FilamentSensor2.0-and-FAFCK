package core.cell;


import core.image.IBinaryImage;

import java.awt.*;
import java.beans.Transient;
import java.io.Serializable;


/**
 * CellShape is the Data-Container for cell-shape specific information
 */
public class CellShape implements Serializable {


    /**
     * @todo could/should be set after area tracking(cell tracking)
     */
    private int identifier;


    // Orientation, aspect ratio, and order parameter are scaled by Const.M.
    private long orientation;
    private long newOrientation;
    private long aspectRatio;
    private long orderParameter;
    private long newOrderParameter;
    private long excursion;
    private long longHalfAxis;
    private int area;
    private Point center;

    //for less memory usage Store the former boolean[][] as BitSet[]
    //another optimization would be store it as BitSet (but calculations could make it slower?)
    //optimize step by step

    /**
     * @todo this attribute can be removed ... maybe? in filamentsensor_benjamin it's only used for updateProject (in menu its called updateOldProject)
     */
    private transient int[][] excursionImage;

    /**
     * Contains the Area as Binary Image
     */
    private transient IBinaryImage binaryImage;

    /**
     * @deprecated 2020-12-01
     * areaImage contains the actual Area as image
     */
    private transient IBinaryImage areaImage;


    /**
     * contains the bounds of the cell
     */
    private Rectangle bounds;


    /**
     * Default Constructor for Serialization
     */
    public CellShape() {
        identifier = -1;

    }

    public CellShape(IBinaryImage binaryImage) {
        this();
        this.binaryImage = binaryImage;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public long getOrientation() {
        return orientation;
    }

    public void setOrientation(long orientation) {
        this.orientation = orientation;
    }

    public long getNewOrientation() {
        return newOrientation;
    }

    public void setNewOrientation(long newOrientation) {
        this.newOrientation = newOrientation;
    }

    public long getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(long aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public long getOrderParameter() {
        return orderParameter;
    }

    public void setOrderParameter(long orderParameter) {
        this.orderParameter = orderParameter;
    }

    public long getNewOrderParameter() {
        return newOrderParameter;
    }

    public void setNewOrderParameter(long newOrderParameter) {
        this.newOrderParameter = newOrderParameter;
    }

    public long getExcursion() {
        return excursion;
    }

    public void setExcursion(long excursion) {
        this.excursion = excursion;
    }

    public int getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public long getLongHalfAxis() {
        return longHalfAxis;
    }

    public void setLongHalfAxis(long longHalfAxis) {
        this.longHalfAxis = longHalfAxis;
    }


    /**
     * the @java.beans.Transient should prevent exporting those fields to XML, with XMLEncoder
     *
     * @return
     */
    @Transient
    public int[][] getExcursionImage() {
        return excursionImage;
    }

    @Transient
    public void setExcursionImage(int[][] excursionImage) {
        this.excursionImage = excursionImage;
    }

    @Transient
    public IBinaryImage getBinaryImage() {
        return binaryImage;
    }

    @Transient
    public void setBinaryImage(IBinaryImage binaryImage) {
        this.binaryImage = binaryImage;
    }

    /**
     * @return
     * @deprecated 2020-12-01
     */
    @Transient
    public IBinaryImage getAreaImage() {
        return areaImage;
    }

    /**
     * @param areaImage
     * @deprecated 2020-12-01
     */
    @Transient
    public void setAreaImage(IBinaryImage areaImage) {
        this.areaImage = areaImage;
    }


    @Override
    public String toString() {
        return "CellShape{" +
                "center=" + center +
                ", bounds=" + bounds +
                '}';
    }
}
