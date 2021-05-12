package core.cell;


import core.filaments.AbstractFilament;

import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class OrientationFieldContainer implements Serializable {

    private int[][][] orientationFields;
    private int[][] orientationFieldShapes;
    private transient int[][] orientationField;//raw data from tracer

    private List<Integer> orientationFieldIds;

    private transient Map<Integer, List<AbstractFilament>> filamentsByOrientationField;

    public OrientationFieldContainer() {
        orientationFieldIds = new ArrayList<>();

    }

    public List<Integer> getOrientationFieldIds() {
        return orientationFieldIds;
    }

    public void setOrientationFieldIds(List<Integer> orientationFieldIds) {
        this.orientationFieldIds = orientationFieldIds;
    }

    public int[][][] getOrientationFields() {
        return orientationFields;
    }

    public void setOrientationFields(int[][][] orientationFields) {
        this.orientationFields = orientationFields;
    }

    public int[][] getOrientationFieldShapes() {
        return orientationFieldShapes;
    }

    public void setOrientationFieldShapes(int[][] orientationFieldShapes) {
        this.orientationFieldShapes = orientationFieldShapes;
    }

    @Transient
    public Map<Integer, List<AbstractFilament>> getFilamentsByOrientationField() {
        return filamentsByOrientationField;
    }

    @Transient
    public void setFilamentsByOrientationField(Map<Integer, List<AbstractFilament>> filamentsByOrientationField) {
        this.filamentsByOrientationField = filamentsByOrientationField;
    }

    @Transient
    public int[][] getOrientationField() {
        return orientationField;
    }

    @Transient
    public void setOrientationField(int[][] orientationField) {
        this.orientationField = orientationField;
    }
}
