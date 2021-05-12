package core.settings;

public enum Batch implements Any {

    determineOrientationFields,// determine orientation fields
    postProcessOrientationFields,// post-process  orientation fields as time series?
    saveOrientationFieldImages,// save orientation  field images?
    restrictAreaChanges,// only small changes in cell shape at each step?
    calculateFingerprints,//  calculate fingerprints?
    saveExcursionImages,//  save excursion images?
    color,//color for filaments, encoded as rgb integer
    doSingleFilamentTracking;//boolean value{1,0}

    private int scaleVal;

    Batch() {
        this(1);
    }

    Batch(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }


}
