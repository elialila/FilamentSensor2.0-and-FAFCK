package fa.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.shape.Polyline;

import java.awt.geom.Point2D;
import java.util.List;

public class SeparatorModel {

    private List<Point2D> points;//not shown on ui
    private IntegerProperty id;//fake id
    private IntegerProperty image;

    private Polyline line;

    private static int current = 0;

    public SeparatorModel() {
        id = new SimpleIntegerProperty();
        image = new SimpleIntegerProperty();
    }

    public SeparatorModel(List<Point2D> points, Polyline line, int image) {
        this();
        this.points = points;
        id.set(current);
        setImage(image);
        this.line = line;
        current++;
    }


    public void setLine(Polyline line) {
        this.line = line;
    }

    public Polyline getLine() {
        return line;
    }

    public List<Point2D> getPoints() {
        return points;
    }

    public void setPoints(List<Point2D> points) {
        this.points = points;
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

    public int getImage() {
        return image.get();
    }

    public IntegerProperty imageProperty() {
        return image;
    }

    public void setImage(int image) {
        this.image.set(image);
    }

    //resets counter
    public static void reset() {
        current = 0;
    }
}
