package core;


import filters.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import core.cell.ShapeContainer;
import core.image.Entry;
import core.image.IBinaryImage;
import core.image.ImageWrapper;
import util.Annotations.NotNull;
import util.Annotations.Nullable;
import util.MixedUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class FilterQueue implements Serializable {

    private List<IFilter> filters;
    private BooleanProperty changed;


    private ChangeListener<Object> listener = ((observable, oldValue, newValue) -> {
        changed.setValue(true);
    });


    //some interface type for filters
    public FilterQueue() {
        filters = new ArrayList<>();
        changed = new SimpleBooleanProperty();
        //changed.addListener((observable, oldValue, newValue) -> {System.out.println("FilterQueue::construct --changed value changed;");});

    }

    public boolean isChanged() {
        return changed.get();
    }

    public BooleanProperty changedProperty() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed.set(changed);
    }

    public void setFilters(List<IFilter> filters) {
        this.filters = filters;
    }

    public void add(IFilter filter) {
        filters.add(filter);
        if (filter instanceof IFilterObservable) {
            ((IFilterObservable) filter).addListener(listener);
        }

    }


    public void remove(IFilter filter) {
        filters.remove(filter);
        if (filter instanceof IFilterObservable) {
            ((IFilterObservable) filter).removeListener(listener);
        }

    }

    public void remove(int idx) {
        IFilter filter = filters.get(idx);
        remove(filter);
    }

    public void clear() {
        filters.stream().filter(f -> f instanceof IFilterObservable).forEach(f -> ((IFilterObservable) f).removeListener(listener));
        filters.clear();
    }


    public void rotateFilters(IFilter a, IFilter b) {
        int sourceIndex = getFilters().indexOf(a);
        int targetIndex = getFilters().indexOf(b);
        List<IFilter> nodes = new ArrayList<>(getFilters());
        if (sourceIndex < targetIndex) {
            Collections.rotate(
                    nodes.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(
                    nodes.subList(targetIndex, sourceIndex + 1), 1);
        }
        getFilters().clear();
        getFilters().addAll(nodes);
    }


    public List<IFilter> getFilters() {
        return filters;
    }

    /**
     * Filters single image from image stack
     *
     * @param wrapper          source image(s) for FilterQueue
     * @param idx              index of image which the filters should be applied on
     * @param progressReporter progress reporter ...
     */
    public void run(@NotNull ImageWrapper wrapper, int idx, @Nullable Consumer<Float> progressReporter) {
        Objects.requireNonNull(wrapper, "FilterQueue::run() --- imageWrapper is null");
        filters.parallelStream().filter(filter -> filter instanceof IFilterPrecalc).forEach(filter -> ((IFilterPrecalc) filter).preCalc());
        if (progressReporter != null) progressReporter.accept(0.1f);
        final float singlePercent = (1f - 0.1f) / (filters.size() + wrapper.getSize());
        List<Entry> entryList = wrapper.getEntryList();
        filters.forEach(filter -> {//has to be sequential
            if (filter instanceof IImageFilterAreaDependant) {
                Entry entry = wrapper.getEntryList().get(idx);
                ShapeContainer shape = entry.getShape();
                IBinaryImage binArea = null;
                if (shape != null) {
                    if (((IImageFilterAreaDependant) filter).isAreaOrExtArea() && shape.getAggregatedArea() != null) {
                        binArea = shape.getAggregatedArea();
                    } else if (shape.getAggregatedExtArea() != null) {
                        binArea = shape.getAggregatedExtArea();
                    }
                    if (binArea != null && binArea.isInMemoryState()) {
                        binArea = binArea.clone();
                        binArea.exitMemoryState();
                    }
                }
                try {
                    ((IImageFilterAreaDependant) filter).run(entry.getProcessor(), binArea);
                } catch (NullPointerException np) {
                    FilamentSensor.debugError(np.getMessage());
                }
                if (progressReporter != null) progressReporter.accept(singlePercent);

            } else if (filter instanceof IImageFilter) {
                Entry entry = wrapper.getEntryList().get(idx);
                ((IImageFilter) filter).run(entry.getProcessor());
                if (progressReporter != null) progressReporter.accept(singlePercent);
            }
        });

    }


    /**
     * @param imageWrapper
     * @param progressReporter reports every single progress, not the sum of current progress(for example: 0.1,0.1,0.1 instead of 0.1,0.2,0.3)
     */
    public void run(@NotNull ImageWrapper imageWrapper, @Nullable Consumer<Float> progressReporter) {
        Objects.requireNonNull(imageWrapper, "FilterQueue::run() --- imageWrapper is null");
        //preCalculate stuff before running the filters
        filters.parallelStream().filter(filter -> filter instanceof IFilterPrecalc).forEach(filter -> ((IFilterPrecalc) filter).preCalc());
        if (progressReporter != null) progressReporter.accept(0.1f);
        final float singlePercent = (1f - 0.1f) / (filters.size() + imageWrapper.getSize());
        List<Entry> entryList = imageWrapper.getEntryList();
        filters.forEach(filter -> {//has to be sequential
            if (filter instanceof IStackFilter) {
                ((IStackFilter) filter).run(imageWrapper.getImage());
            } else if (filter instanceof IImageFilterAreaDependant) {

                MixedUtils.getStream(entryList, false).forEach(entry -> {
                    ShapeContainer shape = entry.getShape();
                    IBinaryImage binArea = null;
                    if (shape != null) {
                        if (((IImageFilterAreaDependant) filter).isAreaOrExtArea() && shape.getAggregatedArea() != null) {
                            binArea = shape.getAggregatedArea();
                        } else if (shape.getAggregatedExtArea() != null) {
                            binArea = shape.getAggregatedExtArea();
                        }
                        if (binArea != null && binArea.isInMemoryState()) {
                            binArea = binArea.clone();
                            binArea.exitMemoryState();
                        }
                    }
                    try {
                        ((IImageFilterAreaDependant) filter).run(entry.getProcessor(), binArea);
                    } catch (NullPointerException np) {
                        FilamentSensor.debugError(np.getMessage());
                    }
                    if (progressReporter != null) progressReporter.accept(singlePercent);
                });
            } else if (filter instanceof IImageFilter) {
                MixedUtils.getStream(entryList, false).forEach(entry -> {
                    ((IImageFilter) filter).run(entry.getProcessor());
                    if (progressReporter != null) progressReporter.accept(singlePercent);
                });

            }
        });

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        filters.forEach(f -> sb.append(f).append(","));

        return sb.toString();
    }


}
