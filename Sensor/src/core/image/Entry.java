package core.image;

import ij.process.ImageProcessor;
import core.cell.DataFilaments;
import core.cell.InteriorContainer;
import core.cell.OrientationFieldContainer;
import core.cell.ShapeContainer;

import java.beans.Transient;
import java.io.Serializable;

public class Entry implements Serializable {
    //Wrapper class for ImageWrapper - Entries

    private String path;
    //in case of clone, this should be a deep copy
    private transient ImageProcessor processor;

    //in case of clone all other attributes should be soft copied(pointing to the same object)
    private ShapeContainer shape;
    private DataFilaments dataFilament;

    private InteriorContainer interiorContainer;
    private OrientationFieldContainer orientationFieldContainer;

    private CorrelationData correlationData;

    public Entry() {
        orientationFieldContainer = new OrientationFieldContainer();
        interiorContainer = new InteriorContainer();
    }

    public Entry(String path, ImageProcessor processor, ShapeContainer shape, DataFilaments dataFilament) {
        this();
        this.path = path;
        this.processor = processor;
        this.shape = shape;
        this.dataFilament = dataFilament;

    }

    public void releaseResources() {
        interiorContainer = null;
        orientationFieldContainer = null;
        processor = null;
        shape = null;
    }

    public CorrelationData getCorrelationData() {
        return correlationData;
    }

    public void setCorrelationData(CorrelationData correlationData) {
        this.correlationData = correlationData;
    }

    public String getPath() {
        return path;
    }

    @Transient
    public ImageProcessor getProcessor() {
        return processor;
    }

    public ShapeContainer getShape() {
        return shape;
    }

    public DataFilaments getDataFilament() {
        return dataFilament;
    }


    public void setPath(String path) {
        this.path = path;
    }

    @Transient
    public void setProcessor(ImageProcessor processor) {
        this.processor = processor;
    }

    public void setShape(ShapeContainer shape) {
        this.shape = shape;
    }

    public void setDataFilament(DataFilaments dataFilament) {
        this.dataFilament = dataFilament;
    }

    public InteriorContainer getInteriorContainer() {
        return interiorContainer;
    }

    public void setInteriorContainer(InteriorContainer interiorContainer) {
        this.interiorContainer = interiorContainer;
    }

    public OrientationFieldContainer getOrientationFieldContainer() {
        return orientationFieldContainer;
    }

    public void setOrientationFieldContainer(OrientationFieldContainer orientationFieldContainer) {
        this.orientationFieldContainer = orientationFieldContainer;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "path='" + path + '\'' +
                ", processor=" + processor +
                ", shape=" + shape +
                ", dataFilament=" + dataFilament +
                ", interiorContainer=" + interiorContainer +
                ", orientationFieldContainer=" + orientationFieldContainer +
                '}';
    }
}


