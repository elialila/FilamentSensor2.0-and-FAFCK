package core.settings;

public enum WiZer implements Any {
    log_view, sample_size;
    private int scaleVal;

    WiZer() {
        this(1);
    }

    WiZer(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
