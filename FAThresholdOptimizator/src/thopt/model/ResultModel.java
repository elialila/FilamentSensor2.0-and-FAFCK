package thopt.model;

import ij.process.AutoThresholder;
import javafx.beans.property.*;

public class ResultModel {

    private final StringProperty fileName;
    private final IntegerProperty thresh;
    private final DoubleProperty hitRate;
    private final DoubleProperty fPRate;
    private final DoubleProperty fNRate;
    private final ObjectProperty<AutoThresholder.Method> method;
    private final IntegerProperty autoThresh;
    private final IntegerProperty checkAgainst;

    private final DoubleProperty hitChk;
    private final DoubleProperty fPChk;

    private final DoubleProperty hitChkMan;
    private final DoubleProperty fPChkMan;

    public ResultModel() {
        fileName = new SimpleStringProperty();
        thresh = new SimpleIntegerProperty();
        hitRate = new SimpleDoubleProperty();
        fPRate = new SimpleDoubleProperty();
        fNRate = new SimpleDoubleProperty();
        method = new SimpleObjectProperty<>();
        autoThresh = new SimpleIntegerProperty();
        checkAgainst = new SimpleIntegerProperty();
        hitChk = new SimpleDoubleProperty();
        fPChk = new SimpleDoubleProperty();
        hitChkMan = new SimpleDoubleProperty();
        fPChkMan = new SimpleDoubleProperty();
    }

    public ResultModel(String fileName, int thresh, double hitRate, double fPRate, double fNRate,
                       AutoThresholder.Method method, int autoThresh) {
        this();
        setFileName(fileName);
        setThresh(thresh);
        setHitRate(hitRate);
        setfPRate(fPRate);
        setfNRate(fNRate);
        setMethod(method);
        setAutoThresh(autoThresh);
    }

    public ResultModel(String fileName, int thresh, double hitRate, double fPRate, double fNRate,
                       AutoThresholder.Method method, int autoThresh, int checkAgainst, double hitChk, double fPChk) {
        this();
        setFileName(fileName);
        setThresh(thresh);
        setHitRate(hitRate);
        setfPRate(fPRate);
        setfNRate(fNRate);
        setMethod(method);
        setAutoThresh(autoThresh);
        setCheckAgainst(checkAgainst);
        setHitChk(hitChk);
        setfPChk(fPChk);

    }

    public double getHitChkMan() {
        return hitChkMan.get();
    }

    public DoubleProperty hitChkManProperty() {
        return hitChkMan;
    }

    public void setHitChkMan(double hitChkMan) {
        this.hitChkMan.set(hitChkMan);
    }

    public double getfPChkMan() {
        return fPChkMan.get();
    }

    public DoubleProperty fPChkManProperty() {
        return fPChkMan;
    }

    public void setfPChkMan(double fPChkMan) {
        this.fPChkMan.set(fPChkMan);
    }

    public double getHitChk() {
        return hitChk.get();
    }

    public DoubleProperty hitChkProperty() {
        return hitChk;
    }

    public void setHitChk(double hitChk) {
        this.hitChk.set(hitChk);
    }

    public double getfPChk() {
        return fPChk.get();
    }

    public DoubleProperty fPChkProperty() {
        return fPChk;
    }

    public void setfPChk(double fPChk) {
        this.fPChk.set(fPChk);
    }

    public int getCheckAgainst() {
        return checkAgainst.get();
    }

    public IntegerProperty checkAgainstProperty() {
        return checkAgainst;
    }

    public void setCheckAgainst(int checkAgainst) {
        this.checkAgainst.set(checkAgainst);
    }

    public int getAutoThresh() {
        return autoThresh.get();
    }

    public IntegerProperty autoThreshProperty() {
        return autoThresh;
    }

    public void setAutoThresh(int autoThresh) {
        this.autoThresh.set(autoThresh);
    }

    public AutoThresholder.Method getMethod() {
        return method.get();
    }

    public ObjectProperty<AutoThresholder.Method> methodProperty() {
        return method;
    }

    public void setMethod(AutoThresholder.Method method) {
        this.method.set(method);
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public double getThresh() {
        return thresh.get();
    }

    public IntegerProperty threshProperty() {
        return thresh;
    }

    public void setThresh(int thresh) {
        this.thresh.set(thresh);
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

    public double getfPRate() {
        return fPRate.get();
    }

    public DoubleProperty fPRateProperty() {
        return fPRate;
    }

    public void setfPRate(double fPRate) {
        this.fPRate.set(fPRate);
    }

    public double getfNRate() {
        return fNRate.get();
    }

    public DoubleProperty fNRateProperty() {
        return fNRate;
    }

    public void setfNRate(double fNRate) {
        this.fNRate.set(fNRate);
    }
}
