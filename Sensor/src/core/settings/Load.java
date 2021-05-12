package core.settings;

public enum Load implements Any {
    keepBitRange;//boolean value 0 or 1

    private int scaleVal;

    Load() {
        this(1);
    }

    Load(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
