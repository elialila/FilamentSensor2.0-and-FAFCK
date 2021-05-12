package core.settings;

public enum Chain implements Any {
    dist_max, dist_max_gap, skew_max, gap_skew_max;
    private int scaleVal;

    Chain() {
        this(1);
    }

    Chain(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }

}