package core.settings;

public enum Pre implements Any {
    scale, minbright, min_range, contrast, black, white,
    rm_sng_px, do_laplace, lpfac(10), lpmask, do_gauss, fsigma(10),
    order, do_line_gauss, lg_with_mask, line_sigma(10), cross_corr,
    corr_mask_size, corr_zero, widths, preview,
    min_area(10000);//range of 0.01% to 100% of image size

    private int scaleVal;

    Pre() {
        this(1);
    }

    Pre(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    public int getScale() {
        return scaleVal;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}