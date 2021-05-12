package core.settings;

public enum Bin implements Any {
    method, radius, sigma(10), minmean, area_significance(10),
    rod_significance(10), rm_sng_px, thicken, restrict,
    is_area_or_ext;


    //boolean value (1|0) tells if area or extarea is used for restriction

    private int scaleVal;

    Bin() {
        this(1);
    }

    Bin(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
