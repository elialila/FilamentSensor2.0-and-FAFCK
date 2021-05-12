package core.settings;

public enum Eval implements Any {
    matchMinPixels(1000), thickenLines, createImage;

    private int scaleVal;

    Eval() {
        this(1);
    }

    Eval(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
