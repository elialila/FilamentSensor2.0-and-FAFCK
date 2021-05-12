package filters.supplier;

import core.Const;

import java.util.*;

public class LaPlaceSupplier implements CBSupplier<Integer> {

    public LaPlaceSupplier() {
    }


    @Override
    public Integer getValue(String toMap) {
        return Const.makeNeighborHoodMap().get(toMap);
    }

    @Override
    public String get(Object value) {
        return Const.makeNeighborHoodMap().entrySet().stream().filter(e -> Objects.equals(e.getValue(), value)).map(Map.Entry::getKey).findAny().orElse(null);
    }

    @Override
    public List<String> get() {
        return new ArrayList<>(Const.makeNeighborHoodMap().keySet());
    }
}
