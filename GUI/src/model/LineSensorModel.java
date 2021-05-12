package model;

import filters.FilterAreaMask;
import filters.FilterBinarization;
import filters.FilterClosing;
import filters.FilterRemovePixels;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import core.FilterQueue;

public class LineSensorModel {


    private SimpleStringProperty binarizationMethod;
    private SimpleStringProperty wizerSamples;


    private DoubleProperty minMeanValue;
    private DoubleProperty sigma;
    private DoubleProperty minStandardDeviation;
    private DoubleProperty minFilamentLength;
    private DoubleProperty lengthStraightPieces;
    private DoubleProperty minAngleDifference;
    private DoubleProperty tolerance;
    private DoubleProperty minArea;
    private DoubleProperty minFilaments;


    private SimpleBooleanProperty restrictToAreaMask;
    private SimpleBooleanProperty thickenLines;
    private SimpleBooleanProperty removeBoundaryFilaments;
    private SimpleBooleanProperty curvedFilaments;
    private SimpleBooleanProperty asStraightPieces;
    private SimpleBooleanProperty logScale;
    private SimpleBooleanProperty areaOrExt;

    private FilterBinarization filterBinarization;
    private FilterQueue filters;

    private BooleanProperty changed;



    public LineSensorModel() {
        filters = new FilterQueue();
        changed = new SimpleBooleanProperty();

        binarizationMethod = new SimpleStringProperty();
        wizerSamples = new SimpleStringProperty();

        minMeanValue = new SimpleDoubleProperty();
        sigma = new SimpleDoubleProperty();
        minStandardDeviation = new SimpleDoubleProperty();
        minFilamentLength = new SimpleDoubleProperty();
        lengthStraightPieces = new SimpleDoubleProperty();
        minAngleDifference = new SimpleDoubleProperty();
        tolerance = new SimpleDoubleProperty();
        minArea = new SimpleDoubleProperty();
        minFilaments = new SimpleDoubleProperty();

        areaOrExt = new SimpleBooleanProperty();
        restrictToAreaMask = new SimpleBooleanProperty();
        thickenLines = new SimpleBooleanProperty();
        removeBoundaryFilaments = new SimpleBooleanProperty();
        curvedFilaments = new SimpleBooleanProperty();
        asStraightPieces = new SimpleBooleanProperty();
        logScale = new SimpleBooleanProperty();


        filterBinarization = new FilterBinarization();
        filterBinarization.methodProperty().bind(this.binarizationMethodProperty());
        filterBinarization.sigmaProperty().bind(this.sigmaProperty());
        filterBinarization.significanceProperty().bind(this.minStandardDeviationProperty());
        filterBinarization.minMeanProperty().bind(this.minMeanValueProperty());


    }


    public void initChangeListener() {
        ChangeListener<Object> changeListener = ((observable, oldValue, newValue) -> {
            if (newValue != null) changed.setValue(true);
        });

        binarizationMethodProperty().addListener(changeListener);
        wizerSamplesProperty().addListener(changeListener);
        minMeanValueProperty().addListener(changeListener);
        sigmaProperty().addListener(changeListener);
        minStandardDeviationProperty().addListener(changeListener);
        minFilamentLengthProperty().addListener(changeListener);
        lengthStraightPiecesProperty().addListener(changeListener);
        minAngleDifferenceProperty().addListener(changeListener);
        toleranceProperty().addListener(changeListener);
        minAreaProperty().addListener(changeListener);
        minFilamentsProperty().addListener(changeListener);


        areaOrExtProperty().addListener(changeListener);
        restrictToAreaMaskProperty().addListener(changeListener);
        thickenLinesProperty().addListener(changeListener);
        removeBoundaryFilamentsProperty().addListener(changeListener);
        curvedFilamentsProperty().addListener(changeListener);
        asStraightPiecesProperty().addListener(changeListener);
        logScaleProperty().addListener(changeListener);

    }


    public FilterQueue getFilterQueue() {
        //empty current queue
        filters.getFilters().clear();

        //add new filters
        filters.add(filterBinarization);
        if (isRestrictToAreaMask()) {
            FilterAreaMask fam = new FilterAreaMask();
            fam.setAreaOrExtArea(isAreaOrExt());
            filters.add(fam);
        }
        if (isThickenLines()) {
            filters.add(new FilterClosing());
        } else {
            filters.add(new FilterRemovePixels());
        }
        //filters.add(new FilterInvert());

        //this way the FilterQueue always stays the same object (same reference to it)
        return filters;
    }


    public boolean isAreaOrExt() {
        return areaOrExt.get();
    }

    public SimpleBooleanProperty areaOrExtProperty() {
        return areaOrExt;
    }

    public void setAreaOrExt(boolean areaOrExt) {
        this.areaOrExt.set(areaOrExt);
    }

    public String getBinarizationMethod() {
        return binarizationMethod.get();
    }

    public SimpleStringProperty binarizationMethodProperty() {
        return binarizationMethod;
    }

    public void setBinarizationMethod(String binarizationMethod) {
        this.binarizationMethod.set(binarizationMethod);
    }

    public String getWizerSamples() {
        return wizerSamples.get();
    }

    public SimpleStringProperty wizerSamplesProperty() {
        return wizerSamples;
    }

    public void setWizerSamples(String wizerSamples) {
        this.wizerSamples.set(wizerSamples);
    }

    public Double getMinMeanValue() {
        return minMeanValue.get();
    }

    public DoubleProperty minMeanValueProperty() {
        return minMeanValue;
    }

    public void setMinMeanValue(Double minMeanValue) {
        this.minMeanValue.set(minMeanValue);
    }

    public Double getSigma() {
        return sigma.get();
    }

    public DoubleProperty sigmaProperty() {
        return sigma;
    }

    public void setSigma(Double sigma) {
        this.sigma.set(sigma);
    }

    public Double getMinStandardDeviation() {
        return minStandardDeviation.get();
    }

    public DoubleProperty minStandardDeviationProperty() {
        return minStandardDeviation;
    }

    public void setMinStandardDeviation(Double minStandardDeviation) {
        this.minStandardDeviation.set(minStandardDeviation);
    }

    public Double getMinFilamentLength() {
        return minFilamentLength.get();
    }

    public DoubleProperty minFilamentLengthProperty() {
        return minFilamentLength;
    }

    public void setMinFilamentLength(Double minFilamentLength) {
        this.minFilamentLength.set(minFilamentLength);
    }

    public Double getLengthStraightPieces() {
        return lengthStraightPieces.get();
    }

    public DoubleProperty lengthStraightPiecesProperty() {
        return lengthStraightPieces;
    }

    public void setLengthStraightPieces(Double lengthStraightPieces) {
        this.lengthStraightPieces.set(lengthStraightPieces);
    }

    public Double getMinAngleDifference() {
        return minAngleDifference.get();
    }

    public DoubleProperty minAngleDifferenceProperty() {
        return minAngleDifference;
    }

    public void setMinAngleDifference(Double minAngleDifference) {
        this.minAngleDifference.set(minAngleDifference);
    }

    public Double getTolerance() {
        return tolerance.get();
    }

    public DoubleProperty toleranceProperty() {
        return tolerance;
    }

    public void setTolerance(Double tolerance) {
        this.tolerance.set(tolerance);
    }

    public Double getMinArea() {
        return minArea.get();
    }

    public DoubleProperty minAreaProperty() {
        return minArea;
    }

    public void setMinArea(Double minArea) {
        this.minArea.set(minArea);
    }

    public Double getMinFilaments() {
        return minFilaments.get();
    }

    public DoubleProperty minFilamentsProperty() {
        return minFilaments;
    }

    public void setMinFilaments(Double minFilaments) {
        this.minFilaments.set(minFilaments);
    }

    public boolean isRestrictToAreaMask() {
        return restrictToAreaMask.get();
    }

    public SimpleBooleanProperty restrictToAreaMaskProperty() {
        return restrictToAreaMask;
    }

    public void setRestrictToAreaMask(boolean restrictToAreaMask) {
        this.restrictToAreaMask.set(restrictToAreaMask);
    }

    public boolean isThickenLines() {
        return thickenLines.get();
    }

    public SimpleBooleanProperty thickenLinesProperty() {
        return thickenLines;
    }

    public void setThickenLines(boolean thickenLines) {
        this.thickenLines.set(thickenLines);
    }

    public boolean isRemoveBoundaryFilaments() {
        return removeBoundaryFilaments.get();
    }

    public SimpleBooleanProperty removeBoundaryFilamentsProperty() {
        return removeBoundaryFilaments;
    }

    public void setRemoveBoundaryFilaments(boolean removeBoundaryFilaments) {
        this.removeBoundaryFilaments.set(removeBoundaryFilaments);
    }

    public boolean isCurvedFilaments() {
        return curvedFilaments.get();
    }

    public SimpleBooleanProperty curvedFilamentsProperty() {
        return curvedFilaments;
    }

    public void setCurvedFilaments(boolean curvedFilaments) {
        this.curvedFilaments.set(curvedFilaments);
    }

    public boolean isAsStraightPieces() {
        return asStraightPieces.get();
    }

    public SimpleBooleanProperty asStraightPiecesProperty() {
        return asStraightPieces;
    }

    public void setAsStraightPieces(boolean asStraightPieces) {
        this.asStraightPieces.set(asStraightPieces);
    }

    public boolean isLogScale() {
        return logScale.get();
    }

    public SimpleBooleanProperty logScaleProperty() {
        return logScale;
    }

    public void setLogScale(boolean logScale) {
        this.logScale.set(logScale);
    }

    public BooleanProperty changedProperty() {
        return changed;
    }

    public boolean isChanged() {
        return changed.get();
    }

    public void setChanged(boolean changed) {
        this.changed.set(changed);
    }
}
