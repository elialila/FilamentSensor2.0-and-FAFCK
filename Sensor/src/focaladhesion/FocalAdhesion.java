package focaladhesion;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import core.image.IBinaryImage;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;

public class FocalAdhesion implements Serializable {

    //XML Serializer does not support javafx.util.Pair (no std constructor, no set) --> not a bean
    private transient BooleanProperty keep;//delete flag
    private transient BooleanProperty selected;//indicator if focal adhesion is selected, mainly used for UI purpose


    private int number;//unique number for each focal adhesion to create relations between filament<->focal adhesion

    private Point2D mainAxisStart;//start point of the x-axis (main axis)
    private Point2D mainAxisEnd;//end point of the x-axis

    private Point2D sideAxisStart;//start point of the y-axis (side axis)
    private Point2D sideAxisEnd;//end point of the y-axis


    private List<Point2D> convexHull;//points which represent the convex hull of the object

    //Oval Shape, Axis Length, Aspect Ratio, Orientation
    private Point2D center;
    private double lengthMainAxis;
    private double lengthSideAxis;

    private double aspectRatio;//ratio between the two axis (x and y axis of the ellipse)
    private double orientation;//rotation-angle is stored in RAD

    private long area;//contains the pixelCount of the focal adhesion (real area not the area of the elliptic shape)
    //22.11.2019 15:10 fA area was too high (likely the inverted area)
    //22.11.2019 15:12 fixed area with addition of if(Prefs.blackBackground) tmpWhite invert (DataExtractor.java)

    private IBinaryImage pixelArea;//2020.10.07 store pixel information in fa


    //add id which is unique over several images (relate focal adhesion's to each other over images)
    //could be done with the AreaTracker


    public FocalAdhesion() {
        keep = new SimpleBooleanProperty(true);
        selected = new SimpleBooleanProperty();
    }

    @java.beans.Transient
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isKeep() {
        return keep.get();
    }

    @java.beans.Transient
    public BooleanProperty keepProperty() {
        return keep;
    }

    public void setKeep(boolean keep) {
        this.keep.set(keep);
    }

    public List<Point2D> getConvexHull() {
        return convexHull;
    }

    public void setConvexHull(List<Point2D> convexHull) {
        this.convexHull = convexHull;
    }

    public Point2D getCenter() {
        return center;
    }

    public void setCenter(Point2D center) {
        this.center = center;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public long getArea() {
        return area;
    }

    public void setArea(long area) {
        this.area = area;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public double getOrientation() {
        return orientation;
    }

    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public Point2D getMainAxisStart() {
        return mainAxisStart;
    }

    public void setMainAxisStart(Point2D mainAxisStart) {
        this.mainAxisStart = mainAxisStart;
    }

    public Point2D getMainAxisEnd() {
        return mainAxisEnd;
    }

    public void setMainAxisEnd(Point2D mainAxisEnd) {
        this.mainAxisEnd = mainAxisEnd;
    }

    public Point2D getSideAxisStart() {
        return sideAxisStart;
    }

    public void setSideAxisStart(Point2D sideAxisStart) {
        this.sideAxisStart = sideAxisStart;
    }

    public Point2D getSideAxisEnd() {
        return sideAxisEnd;
    }

    public void setSideAxisEnd(Point2D sideAxisEnd) {
        this.sideAxisEnd = sideAxisEnd;
    }

    public double getLengthMainAxis() {
        return lengthMainAxis;
    }

    public void setLengthMainAxis(double lengthMainAxis) {
        this.lengthMainAxis = lengthMainAxis;
    }

    public double getLengthSideAxis() {
        return lengthSideAxis;
    }

    public void setLengthSideAxis(double lengthSideAxis) {
        this.lengthSideAxis = lengthSideAxis;
    }

    public IBinaryImage getPixelArea() {
        return pixelArea;
    }

    public void setPixelArea(IBinaryImage pixelArea) {
        this.pixelArea = pixelArea;
    }

    @Override
    public String toString() {
        return "FocalAdhesion{" +
                "center=" + center +
                ", xAxisLength=" + lengthMainAxis +
                ", yAxisLength=" + lengthSideAxis +
                ", aspectRatio=" + aspectRatio +
                ", orientation=" + orientation +
                ", area=" + area +
                ",areaEllipse=" + Math.PI * (lengthMainAxis / 2) * (lengthSideAxis / 2) +
                '}';
    }
}
