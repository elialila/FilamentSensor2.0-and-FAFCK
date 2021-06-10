package tracking.shape;

import javafx.beans.property.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeTable<T extends TrackingShape> implements Serializable {

    private Map<Integer, ShapeEvent<T>> shapes;//time starts at 0; and goes until maxTime-1
    //properties don't get set
    private final transient BooleanProperty keep;//currently not in use
    private final transient DoubleProperty length;
    private final transient IntegerProperty birth;
    private final transient IntegerProperty death;
    private final transient BooleanProperty selected;
    private int identifier;//identifier of TimeTable, this will be given to all shapes matching to the dynamic area

    private TimeTable<T> related;


    public TimeTable() {
        shapes = new HashMap<>();
        keep = new SimpleBooleanProperty(true);
        length = new SimpleDoubleProperty();
        birth = new SimpleIntegerProperty();
        death = new SimpleIntegerProperty();
        selected = new SimpleBooleanProperty(false);
        identifier = -1;
    }


    public void updateIdentifiers() {
        shapes.values().forEach(event -> {
            if (event instanceof ShapeEvent.ShapeAliveEvent) {
                ((ShapeEvent.ShapeAliveEvent<T>) event).getTarget().setIdentifier(getIdentifier());
            } else if (event instanceof ShapeEvent.ShapeSplitEvent) {
                ((ShapeEvent.ShapeSplitEvent<T>) event).getTarget().forEach(shape -> shape.setIdentifier(getIdentifier()));
            } else if (event instanceof ShapeEvent.ShapeDeTouchEvent) {
                ((ShapeEvent.ShapeDeTouchEvent<T>) event).getTarget().forEach(shape -> shape.setIdentifier(getIdentifier()));
            } else if (event instanceof ShapeEvent.ShapeSingleSourceEvent) {//start and end event
                ((ShapeEvent.ShapeSingleSourceEvent<T>) event).getSource().setIdentifier(getIdentifier());
            } else if (event instanceof ShapeEvent.ShapeFusionEvent) {
                ((ShapeEvent.ShapeFusionEvent<T>) event).getTarget().setIdentifier(getIdentifier());
            } else if (event instanceof ShapeEvent.ShapeTouchEvent) {
                ((ShapeEvent.ShapeTouchEvent<T>) event).getTarget().setIdentifier(getIdentifier());
            }
        });
    }


    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public Map<Integer, ShapeEvent<T>> getShapes() {
        return shapes;
    }

    public void setShapes(Map<Integer, ShapeEvent<T>> shapes) {
        this.shapes = shapes;
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

    public double getLength() {
        return length.get();
    }

    public DoubleProperty lengthProperty() {
        return length;
    }

    public void setLength(double length) {
        this.length.set(length);
    }

    public int getBirth() {
        return birth.get();
    }

    public IntegerProperty birthProperty() {
        return birth;
    }

    public void setBirth(int birth) {
        this.birth.set(birth);
    }

    public int getDeath() {
        return death.get();
    }

    public IntegerProperty deathProperty() {
        return death;
    }

    public void setDeath(int death) {
        this.death.set(death);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }


    public boolean contains(T cell) {
        return getShapes().values().stream().anyMatch(e -> e.contains(cell));
    }

    public List<ShapeEvent<T>> getContained(T shape) {
        return getShapes().values().stream().filter(e -> e.contains(shape)).collect(Collectors.toList());
    }

    public TimeTable<T> getRelated() {
        return related;
    }

    public void setRelated(TimeTable<T> related) {
        this.related = related;
    }

}
