package core.settings;

public enum Trace implements Any {
    curve, split, minlen, minangle, tolerance, step, no_boundary;
    private int scaleVal;

    Trace() {
        this(1);
    }

    Trace(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}