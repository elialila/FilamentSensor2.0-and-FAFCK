package tracking.area;

import javafx.beans.property.*;
import core.cell.CellShape;
import util.IOUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
    Class wrapping up the Mapping of Areas(Cells) in time.
 */
public class DynamicArea implements Serializable {
    private Map<Integer, CellEvent> areas;//time starts at 0; and goes until maxTime-1
    //properties don't get set
    private transient BooleanProperty keep;//currently not in use
    private transient DoubleProperty length;
    private transient IntegerProperty birth;
    private transient IntegerProperty death;
    private transient BooleanProperty selected;
    private int identifier;//identifier of dynamic area, this will be given to all shapes matching to the dynamic area

    private DynamicArea related;

    public DynamicArea() {
        areas = new HashMap<>();
        keep = new SimpleBooleanProperty(true);
        length = new SimpleDoubleProperty();
        birth = new SimpleIntegerProperty();
        death = new SimpleIntegerProperty();
        selected = new SimpleBooleanProperty(false);
        identifier = -1;
    }


    public void updateIdentifiers() {
        areas.values().forEach(event -> {
            if (event instanceof CellEvent.CellAliveEvent) {
                ((CellEvent.CellAliveEvent) event).getTarget().setIdentifier(getIdentifier());
            } else if (event instanceof CellEvent.CellSplitEvent) {
                ((CellEvent.CellSplitEvent) event).getTarget().forEach(shape -> shape.setIdentifier(getIdentifier()));
            } else if (event instanceof CellEvent.CellDeTouchEvent) {
                ((CellEvent.CellDeTouchEvent) event).getTarget().forEach(shape -> shape.setIdentifier(getIdentifier()));
            } else if (event instanceof CellEvent.CellSingleSourceEvent) {//start and end event
                ((CellEvent.CellSingleSourceEvent) event).getSource().setIdentifier(getIdentifier());
            } else if (event instanceof CellEvent.CellFusionEvent) {
                ((CellEvent.CellFusionEvent) event).getTarget().setIdentifier(getIdentifier());
            } else if (event instanceof CellEvent.CellTouchEvent) {
                ((CellEvent.CellTouchEvent) event).getTarget().setIdentifier(getIdentifier());
            }
        });
    }


    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public Map<Integer, CellEvent> getAreas() {
        return areas;
    }

    public void setAreas(Map<Integer, CellEvent> areas) {
        this.areas = areas;
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


    public boolean contains(CellShape cell) {
        return getAreas().values().stream().anyMatch(e -> e.contains(cell));
    }

    public List<CellEvent> getContained(CellShape cell) {
        return getAreas().values().stream().filter(e -> e.contains(cell)).collect(Collectors.toList());
    }

    public DynamicArea getRelated() {
        return related;
    }

    public void setRelated(DynamicArea related) {
        this.related = related;
    }
}
