package core.settings;

public enum SFTracking implements Any {

    max_dist,
    factor_angle,
    factor_length,
    length,
    existsInMin,//the filaments must exist in >existsInMin time slots otherwise they get dropped (performance reasons)
    chkExistsInMin,//checkbox(boolean:{0,1}) if it should filter by the above
    existsInMax,//similar to above
    chkExistsInMax,//{0,1}
    combineMultiMatches;//{0,1}

    private int scaleVal;

    SFTracking() {
        this(1);
    }

    SFTracking(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
