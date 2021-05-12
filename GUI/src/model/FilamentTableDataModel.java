package model;

import core.filaments.AbstractFilament;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;

public class FilamentTableDataModel {

    private IntegerProperty number;
    private BooleanProperty keep;
    private DoubleProperty centerX;
    private DoubleProperty centerY;
    private DoubleProperty length;
    private DoubleProperty angle;
    private DoubleProperty width;
    private int originalRowNumber;

    private AbstractFilament filament;

    private Polyline polyline;
    private Paint prevStroke;


    public FilamentTableDataModel() {
        keep = new SimpleBooleanProperty();
        centerX = new SimpleDoubleProperty();
        centerY = new SimpleDoubleProperty();
        length = new SimpleDoubleProperty();
        angle = new SimpleDoubleProperty();
        width = new SimpleDoubleProperty();
        number = new SimpleIntegerProperty();
    }

    public FilamentTableDataModel(AbstractFilament filament, Polyline polyline) {
        this();
        setNumber(filament.getNumber());
        setCenterX((double) filament.getCenter().x / 10);
        setCenterY((double) filament.getCenter().y / 10);
        setLength(Math.round(filament.getLength() / 1000.0) / 1000.0);
        setAngle(Math.round(filament.getOrientation() / 1000.0) / 1000.0);
        setWidth(Math.round(filament.getWidth() / 1000.0) / 1000.0);
        this.keep = filament.keepProperty();
        this.filament = filament;
        this.polyline = polyline;
        if (polyline != null) prevStroke = polyline.getStroke();
    }

    public Paint getPrevStroke() {
        return prevStroke;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public int getNumber() {
        return number.get();
    }

    public IntegerProperty numberProperty() {
        return number;
    }

    public void setNumber(int number) {
        this.number.set(number);
    }

    public int getOriginalRowNumber() {
        return originalRowNumber;
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

    public double getLength() {
        return length.get();
    }

    public DoubleProperty lengthProperty() {
        return length;
    }

    public void setLength(double length) {
        this.length.set(length);
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

    public double getWidth() {
        return width.get();
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public void setWidth(double width) {
        this.width.set(width);
    }

    public AbstractFilament getFilament() {
        return filament;
    }

    /*

       case 0:
            return fil.getKeep();
        case 1:
            return fil.getCenter().x / 10.0;
        case 2:
            return fil.getCenter().y / 10.0;
        case 3:
            return Math.round(fil.getLength() / 1000.0) / 1000.0;
        case 4:
            return Math.round(fil.getOrientation() / 1000.0) / 1000.0;
        case 5:
            return Math.round(fil.getWidth() / 1000.0) / 1000.0;

     */

}
