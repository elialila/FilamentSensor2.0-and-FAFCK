package core.settings;

public enum ATracking implements Any {
    intersectTolerance(100),//tolerance in % 0.00-1 => 0-100%
    length,//length in time T_death - T_birth
    existsInMin,//the filaments must exist in >existsInMin time slots otherwise they get dropped (performance reasons)
    chkExistsInMin,//checkbox(boolean:{0,1}) if it should filter by the above
    existsInMax,//similar to above
    chkExistsInMax;//{0,1}

    private int scaleVal;

    ATracking() {
        this(1);
    }

    ATracking(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
