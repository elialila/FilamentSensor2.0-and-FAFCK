package core.settings;

public enum Hough implements Any {
    image, orientations, sigma, minmean;
    private int scaleVal;

    Hough() {
        this(1);
    }

    Hough(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
