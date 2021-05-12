package core.orientationfields;

import java.io.Serializable;

public class OrientationFieldBlock implements Serializable {


    private int blockNumber;//is it necessary?

    private int centerX;
    private int centerY;
    private int orientation;

    public OrientationFieldBlock() {
    }

    public OrientationFieldBlock(int centerX, int centerY, int orientation, int blockNumber) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.orientation = orientation;
        this.blockNumber = blockNumber;
    }

    public int getCenterX() {
        return centerX;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public void setCenterY(int centerY) {
        this.centerY = centerY;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }
}
