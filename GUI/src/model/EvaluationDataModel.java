package model;

import evaluation.EvaluationData;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;


public class EvaluationDataModel {

    private final DoubleProperty hitRate;
    private final DoubleProperty missRate;
    private final DoubleProperty fpRate;
    private final DoubleProperty fnRate;


    private final IntegerProperty whiteEval;
    private final IntegerProperty whiteTruth;

    private final IntegerProperty fnMatches;
    private final IntegerProperty fpMatches;
    private final IntegerProperty whiteMatches;
    private final IntegerProperty objectsEval;
    private final IntegerProperty objectsTruth;
    private final IntegerProperty objectsFound;
    private final IntegerProperty objectsMissed;
    private final IntegerProperty objectsFP;
    private final IntegerProperty multiMatchesOneToN;
    private final IntegerProperty multiMatchesNToOne;


    private EvaluationData source;
    private final IntegerProperty nr;

    public EvaluationDataModel() {
        hitRate = new SimpleDoubleProperty();
        missRate = new SimpleDoubleProperty();
        fpRate = new SimpleDoubleProperty();
        fnRate = new SimpleDoubleProperty();
        fnMatches = new SimpleIntegerProperty();
        fpMatches = new SimpleIntegerProperty();
        whiteMatches = new SimpleIntegerProperty();
        objectsEval = new SimpleIntegerProperty();
        objectsTruth = new SimpleIntegerProperty();
        objectsFound = new SimpleIntegerProperty();
        objectsMissed = new SimpleIntegerProperty();
        objectsFP = new SimpleIntegerProperty();
        multiMatchesOneToN = new SimpleIntegerProperty();
        multiMatchesNToOne = new SimpleIntegerProperty();
        nr = new SimpleIntegerProperty();

        whiteEval = new SimpleIntegerProperty();
        whiteTruth = new SimpleIntegerProperty();
    }

    public EvaluationDataModel(double hitRate, double missRate, double fpRate, double fnRate,
                               int fnMatches, int fpMatches, int whiteMatches, int objectsEval, int objectsTruth,
                               int objectsFound, int objectsMissed, int objectsFP, int multiMatchesOneToN, int multiMatchesNToOne, int whiteEval, int whiteTruth) {
        this();
        setHitRate(hitRate);
        setMissRate(missRate);
        setFpRate(fpRate);
        setFnRate(fnRate);
        setFnMatches(fnMatches);
        setFpMatches(fpMatches);
        setWhiteMatches(whiteMatches);
        setObjectsEval(objectsEval);
        setObjectsTruth(objectsTruth);
        setObjectsFound(objectsFound);
        setObjectsMissed(objectsMissed);
        setObjectsFP(objectsFP);
        setMultiMatchesOneToN(multiMatchesOneToN);
        setMultiMatchesNToOne(multiMatchesNToOne);
        setWhiteEval(whiteEval);
        setWhiteTruth(whiteTruth);
    }

    public EvaluationDataModel(EvaluationData data, int idx) {
        this(data.getHitRate(),
                data.getMissRate(),
                data.getFpRate(),
                data.getFnRate(),
                data.getFnMatches(),
                data.getFpMatches(),
                data.getWhiteMatches(),
                data.getObjectsEval(),
                data.getObjectsTruth(),
                data.getObjectsFound(),
                data.getObjectsMissed(),
                data.getObjectsFP(),
                data.getMultiMatchesOneToN(),
                data.getMultiMatchesNToOne(),
                data.getWhiteEval(),
                data.getWhiteTruth());
        this.source = data;
        setNr(idx);
    }

    public EvaluationData getSource() {
        return source;
    }

    public IntegerProperty nrProperty() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr.set(nr);
    }

    public int getNr() {
        return nr.get();
    }

    public int getWhiteEval() {
        return whiteEval.get();
    }

    public IntegerProperty whiteEvalProperty() {
        return whiteEval;
    }

    public void setWhiteEval(int whiteEval) {
        this.whiteEval.set(whiteEval);
    }

    public int getWhiteTruth() {
        return whiteTruth.get();
    }

    public IntegerProperty whiteTruthProperty() {
        return whiteTruth;
    }

    public void setWhiteTruth(int whiteTruth) {
        this.whiteTruth.set(whiteTruth);
    }

    public double getHitRate() {
        return hitRate.get();
    }

    public DoubleProperty hitRateProperty() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate.set(hitRate);
    }

    public double getMissRate() {
        return missRate.get();
    }

    public DoubleProperty missRateProperty() {
        return missRate;
    }

    public void setMissRate(double missRate) {
        this.missRate.set(missRate);
    }

    public double getFpRate() {
        return fpRate.get();
    }

    public DoubleProperty fpRateProperty() {
        return fpRate;
    }

    public void setFpRate(double fpRate) {
        this.fpRate.set(fpRate);
    }

    public double getFnRate() {
        return fnRate.get();
    }

    public DoubleProperty fnRateProperty() {
        return fnRate;
    }

    public void setFnRate(double fnRate) {
        this.fnRate.set(fnRate);
    }

    public int getFnMatches() {
        return fnMatches.get();
    }

    public IntegerProperty fnMatchesProperty() {
        return fnMatches;
    }

    public void setFnMatches(int fnMatches) {
        this.fnMatches.set(fnMatches);
    }

    public int getFpMatches() {
        return fpMatches.get();
    }

    public IntegerProperty fpMatchesProperty() {
        return fpMatches;
    }

    public void setFpMatches(int fpMatches) {
        this.fpMatches.set(fpMatches);
    }

    public int getWhiteMatches() {
        return whiteMatches.get();
    }

    public IntegerProperty whiteMatchesProperty() {
        return whiteMatches;
    }

    public void setWhiteMatches(int whiteMatches) {
        this.whiteMatches.set(whiteMatches);
    }

    public int getObjectsEval() {
        return objectsEval.get();
    }

    public IntegerProperty objectsEvalProperty() {
        return objectsEval;
    }

    public void setObjectsEval(int objectsEval) {
        this.objectsEval.set(objectsEval);
    }

    public int getObjectsTruth() {
        return objectsTruth.get();
    }

    public IntegerProperty objectsTruthProperty() {
        return objectsTruth;
    }

    public void setObjectsTruth(int objectsTruth) {
        this.objectsTruth.set(objectsTruth);
    }

    public int getObjectsFound() {
        return objectsFound.get();
    }

    public IntegerProperty objectsFoundProperty() {
        return objectsFound;
    }

    public void setObjectsFound(int objectsFound) {
        this.objectsFound.set(objectsFound);
    }

    public int getObjectsMissed() {
        return objectsMissed.get();
    }

    public IntegerProperty objectsMissedProperty() {
        return objectsMissed;
    }

    public void setObjectsMissed(int objectsMissed) {
        this.objectsMissed.set(objectsMissed);
    }

    public int getObjectsFP() {
        return objectsFP.get();
    }

    public IntegerProperty objectsFPProperty() {
        return objectsFP;
    }

    public void setObjectsFP(int objectsFP) {
        this.objectsFP.set(objectsFP);
    }

    public int getMultiMatchesOneToN() {
        return multiMatchesOneToN.get();
    }

    public IntegerProperty multiMatchesOneToNProperty() {
        return multiMatchesOneToN;
    }

    public void setMultiMatchesOneToN(int multiMatchesOneToN) {
        this.multiMatchesOneToN.set(multiMatchesOneToN);
    }

    public int getMultiMatchesNToOne() {
        return multiMatchesNToOne.get();
    }

    public IntegerProperty multiMatchesNToOneProperty() {
        return multiMatchesNToOne;
    }

    public void setMultiMatchesNToOne(int multiMatchesNToOne) {
        this.multiMatchesNToOne.set(multiMatchesNToOne);
    }
}
