package core.settings;

public enum Elog implements Any {
    input, image, tolerance, zero_crossings, threshold, contrast,
    orientations, sigma_x, sigma_y;

    private int scaleVal;

    Elog() {
        this(1);
    }

    Elog(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
