package core.cell;


import core.image.IBinaryImage;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * cellshape could contain a binary image in size of the cellshape and a x,y coordinate for top-left-corner
 * this way a little bit of memory could be stored again, but putting the areas together could be a lot more performance cost
 *
 * @todo implement a mechanism to select one area, for processing(only BinaryImage needed) ---> this could be based on the "areatracking"(cell event tracking)
 */
public class ShapeContainer implements Serializable {

    //contains several shapes for one image (all detected areas)

    private List<CellShape> areas;
    private List<CellShape> extAreas;

    /**
     * @deprecated 2020-05-01 not used anymore, area algorithm was made more restrictive to better match area (still a lot of improvements possible)
     */
    private List<CellShape> mechAreas;


    //another area for mech?

    private transient SoftReference<IBinaryImage> cacheAggregatedArea;
    private transient SoftReference<IBinaryImage> cacheAggregatedExtArea;
    private transient SoftReference<IBinaryImage> cacheAggregatedMechArea;


    public ShapeContainer() {
        areas = new ArrayList<>();
        extAreas = new ArrayList<>();
        mechAreas = new ArrayList<>();
        cacheAggregatedArea = new SoftReference<>(null);
        cacheAggregatedExtArea = new SoftReference<>(null);
        cacheAggregatedMechArea = new SoftReference<>(null);

    }

    public List<CellShape> getAreas() {
        return areas;
    }

    public void setAreas(List<CellShape> areas) {
        this.areas = areas;
    }

    public List<CellShape> getExtAreas() {
        return extAreas;
    }

    public void setExtAreas(List<CellShape> extAreas) {
        this.extAreas = extAreas;
    }

    public List<CellShape> getMechAreas() {
        return mechAreas;
    }

    public void setMechAreas(List<CellShape> mechAreas) {
        this.mechAreas = mechAreas;
    }

    @java.beans.Transient
    public IBinaryImage getAggregatedArea() {
        if (cacheAggregatedArea.get() == null)
            cacheAggregatedArea = new SoftReference<>(getAreas().stream().map(a -> a.getBinaryImage().clone()).reduce(
                    (a, b) -> {
                        a.or(b);
                        return a;
                    }).orElse(null));
        return cacheAggregatedArea.get();
    }

    @java.beans.Transient
    public IBinaryImage getAggregatedExtArea() {
        if (cacheAggregatedExtArea.get() == null)
            cacheAggregatedExtArea = new SoftReference<>(getExtAreas().stream().map(a -> a.getBinaryImage().clone()).reduce(
                    (a, b) -> {
                        a.or(b);
                        return a;
                    }).orElse(null));
        return cacheAggregatedExtArea.get();
    }

    @java.beans.Transient
    public IBinaryImage getAggregatedMechArea() {
        if (cacheAggregatedMechArea.get() == null)
            cacheAggregatedMechArea = new SoftReference<>(getMechAreas().stream().map(a -> a.getBinaryImage().clone()).reduce(
                    (a, b) -> {
                        a.or(b);
                        return a;
                    }).orElse(null));
        return cacheAggregatedMechArea.get();
    }

    @java.beans.Transient
    public CellShape getSelectedArea() {
        if (getAreas().size() < 1) return null;
        return getAreas().get(0);
    }

    @java.beans.Transient
    public IBinaryImage getSelectedAreaExt() {
        if (getExtAreas().size() < 1) return null;
        return getExtAreas().get(0).getBinaryImage();
    }

    @java.beans.Transient
    public IBinaryImage getSelectedAreaMech() {
        return (getMechAreas().size() > 0) ? getMechAreas().get(0).getBinaryImage() : null;
    }

    public void clearCache() {
        cacheAggregatedArea.clear();
        cacheAggregatedMechArea.clear();
        cacheAggregatedExtArea.clear();
    }


}
