package core.settings;

public enum FocAdh implements Any {
    minSize,//minimum size a focal adhesion has to have (in pixels)
    maxSize,//maximum size a focal adhesion is allowed to have(in pixels)
    maxClusterAmount,//maximum number of clusters(raw focal adhesion's) an image can have, if exceeded the image won't be processed further and taken as "noisy"
    bothEnds,//if set true verification will need two focal adhesion's on a filament curve for setting it true
    //if set to false the verification does only need one focal adhesion
    neighborHoodSize,//valid distance from filament to focal adhesion
    doClosing,//do closing after thresholding
    doFillHoles,//do fill holes after thresholding/[closing]
    showOnlyFoundFA;//{0,1}
    //some noisy images have a lot of small, but not small enough points

    //these restrictions are introduced through observation of test data
    //should be verified if they are useful or not

    private int scaleVal;

    FocAdh(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    FocAdh() {
        this(1);
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
