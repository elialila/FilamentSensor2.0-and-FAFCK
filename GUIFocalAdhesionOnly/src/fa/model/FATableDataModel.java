package fa.model;

import focaladhesion.FocalAdhesion;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.scene.shape.Ellipse;

public class FATableDataModel {

    private IntegerProperty id;
    private BooleanProperty keep;
    private DoubleProperty centerX;
    private DoubleProperty centerY;
    private DoubleProperty lengthMain;
    private DoubleProperty lengthSide;
    private DoubleProperty angle;
    private DoubleProperty area;
    private DoubleProperty areaEllipse;

    private FocalAdhesion data;

    private Ellipse ellipse;

    public FATableDataModel() {
        id = new SimpleIntegerProperty();
        keep = new SimpleBooleanProperty();
        centerX = new SimpleDoubleProperty();
        centerY = new SimpleDoubleProperty();
        lengthMain = new SimpleDoubleProperty();
        lengthSide = new SimpleDoubleProperty();
        angle = new SimpleDoubleProperty();
        area = new SimpleDoubleProperty();
        areaEllipse = new SimpleDoubleProperty();
        areaEllipse.bind(Bindings.multiply(Math.PI, Bindings.multiply(lengthMain.divide(2), lengthSide.divide(2))));
    }

    public FATableDataModel(FocalAdhesion focalAdhesion, Ellipse ellipse) {
        this();
        setId(focalAdhesion.getNumber());
        this.keep = focalAdhesion.keepProperty();
        setAngle(focalAdhesion.getOrientation());
        setArea(focalAdhesion.getArea());
        setCenterX(focalAdhesion.getCenter().getX());
        setCenterY(focalAdhesion.getCenter().getY());
        setLengthMain(focalAdhesion.getLengthMainAxis());
        setLengthSide(focalAdhesion.getLengthSideAxis());
        setData(focalAdhesion);
        setEllipse(ellipse);
    }

    public void setEllipse(Ellipse ellipse) {
        this.ellipse = ellipse;
    }

    public Ellipse getEllipse() {
        return ellipse;
    }

    public FocalAdhesion getData() {
        return data;
    }

    public void setData(FocalAdhesion data) {
        this.data = data;
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public boolean isKeep() {
        return keep.get();
    }

    public BooleanProperty keepProperty() {
        return keep;
    }

    public void setKeep(boolean keep) {
        this.keep.set(keep);
    }

    public double getCenterX() {
        return centerX.get();
    }

    public DoubleProperty centerXProperty() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX.set(centerX);
    }

    public double getCenterY() {
        return centerY.get();
    }

    public DoubleProperty centerYProperty() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY.set(centerY);
    }

    public double getLengthMain() {
        return lengthMain.get();
    }

    public DoubleProperty lengthMainProperty() {
        return lengthMain;
    }

    public void setLengthMain(double lengthMain) {
        this.lengthMain.set(lengthMain);
    }

    public double getAngle() {
        return angle.get();
    }

    public DoubleProperty angleProperty() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle.set(angle);
    }

    public double getLengthSide() {
        return lengthSide.get();
    }

    public DoubleProperty lengthSideProperty() {
        return lengthSide;
    }

    public void setLengthSide(double lengthSide) {
        this.lengthSide.set(lengthSide);
    }

    public double getArea() {
        return area.get();
    }

    public DoubleProperty areaProperty() {
        return area;
    }

    public void setArea(double area) {
        this.area.set(area);
    }

    public double getAreaEllipse() {
        return areaEllipse.get();
    }

    public DoubleProperty areaEllipseProperty() {
        return areaEllipse;
    }
}
