package core.settings;

public enum Ori implements Any {
    min_area(10), min_filaments;
    private int scaleVal;

    Ori() {
        this(1);
    }

    Ori(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}