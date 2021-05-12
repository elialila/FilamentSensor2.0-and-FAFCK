package core.settings;

public enum Export implements Any {

    hideNonVerifiedFibers,//boolean{0,1}
    hideUnusedFAs,//boolean{0,1}
    hideSingleUsedFAs,//boolean{0,1}
    hideSingleVerifiedFibers,//{0,1}
    hideMultiUsedFAs,//boolean{0,1}
    hideMultiVerifiedFibers//boolean{0,1}
    ;


    private int scaleVal;

    Export() {
        this(1);
    }

    Export(int scaleVal) {
        this.scaleVal = scaleVal;
    }

    @Override
    public int getScale() {
        return 0;
    }

    public void setScale(int scaleVal) {
        this.scaleVal = scaleVal;
    }
}
