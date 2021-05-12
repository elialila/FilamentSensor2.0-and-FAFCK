package core.orientationfields;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OrientationField implements Serializable {

    private int orientationFieldNumber;//is it necessary?

    private List<OrientationFieldBlock> blocks;

    public OrientationField() {
        blocks = new ArrayList<>();
    }

    public OrientationField(List<OrientationFieldBlock> blocks, int orientationFieldNumber) {
        this.orientationFieldNumber = orientationFieldNumber;
        this.blocks = blocks;
    }


    public int getOrientationFieldNumber() {
        return orientationFieldNumber;
    }

    public void setOrientationFieldNumber(int orientationFieldNumber) {
        this.orientationFieldNumber = orientationFieldNumber;
    }

    public List<OrientationFieldBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<OrientationFieldBlock> blocks) {
        this.blocks = blocks;
    }
}
