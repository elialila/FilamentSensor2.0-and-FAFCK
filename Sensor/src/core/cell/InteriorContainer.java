package core.cell;

import core.image.IBinaryImage;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Arrays;

public class InteriorContainer implements Serializable {

    private long[] interiorData;
    private transient IBinaryImage interior;


    public InteriorContainer() {
    }

    public long[] getInteriorData() {
        return interiorData;
    }

    public void setInteriorData(long[] interiorData) {
        this.interiorData = interiorData;
    }

    @Transient
    public IBinaryImage getInterior() {
        return interior;
    }

    @Transient
    public void setInterior(IBinaryImage interior) {
        this.interior = interior;
    }

    @Override
    public String toString() {
        return "InteriorContainer{" +
                "interiorData=" + Arrays.toString(interiorData) +
                '}';
    }
}
