package tracking.shape;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class ShapeEvent<T extends TrackingShape> implements Serializable {

    /**
     * @param shape shape which should be checked
     * @return Returns boolean value true if the given shape is part of the cell event otherwise false
     */
    public abstract boolean contains(T shape);


    //region CellEvent Hierarchy
    public static class ShapeSingleSourceEvent<T extends TrackingShape> extends ShapeEvent<T> {
        private T source;

        public ShapeSingleSourceEvent() {
            super();
        }

        public ShapeSingleSourceEvent(T source) {
            this();
            setSource(source);
        }

        public T getSource() {
            return source;
        }

        public void setSource(T source) {
            this.source = source;
        }

        @Override
        public boolean contains(T shape) {
            return source.equals(shape);
        }
    }

    public static class ShapeFusionEvent<T extends TrackingShape> extends ShapeEvent<T> {
        private List<T> source;
        private T target;

        public ShapeFusionEvent() {
            super();
            source = new ArrayList<>();
        }

        public ShapeFusionEvent(List<T> source, T target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<T> getSource() {
            return source;
        }

        public void setSource(List<T> source) {
            this.source = source;
        }

        public T getTarget() {
            return target;
        }

        public void setTarget(T target) {
            this.target = target;
        }

        @Override
        public boolean contains(T shape) {
            return target.equals(shape);//||source.contains(shape);
        }
    }

    public static class ShapeTouchEvent<T extends TrackingShape> extends ShapeEvent<T> {
        private List<T> source;
        private T target;

        public ShapeTouchEvent() {
            super();
            source = new ArrayList<>();
        }

        public ShapeTouchEvent(List<T> source, T target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<T> getSource() {
            return source;
        }

        public void setSource(List<T> source) {
            this.source = source;
        }

        public T getTarget() {
            return target;
        }

        public void setTarget(T target) {
            this.target = target;
        }

        @Override
        public boolean contains(T shape) {
            return target.equals(shape);//source.contains(shape)||target.equals(shape);
        }
    }

    public static class ShapeSplitEvent<T extends TrackingShape> extends ShapeEvent.ShapeSingleSourceEvent<T> {
        private List<T> target;

        public ShapeSplitEvent() {
            super();
            target = new ArrayList<>();
        }

        public ShapeSplitEvent(T source, List<T> target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<T> getTarget() {
            return target;
        }

        public void setTarget(List<T> target) {
            this.target = target;
        }

        @Override
        public boolean contains(T shape) {
            return target.contains(shape);//getSource().equals(shape)||target.contains(shape);
        }
    }

    public static class ShapeDeTouchEvent<T extends TrackingShape> extends ShapeEvent.ShapeSingleSourceEvent<T> {
        private List<T> target;

        public ShapeDeTouchEvent() {
            super();
            target = new ArrayList<>();
        }

        public ShapeDeTouchEvent(T source, List<T> target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<T> getTarget() {
            return target;
        }

        public void setTarget(List<T> target) {
            this.target = target;
        }

        @Override
        public boolean contains(T shape) {
            return target.contains(shape);//getSource().equals(shape)||target.contains(shape);
        }
    }

    public static class ShapeAliveEvent<T extends TrackingShape> extends ShapeEvent.ShapeSingleSourceEvent<T> {
        private T target;

        public ShapeAliveEvent() {
            super();
        }

        public ShapeAliveEvent(T source, T target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public T getTarget() {
            return target;
        }

        public void setTarget(T target) {
            this.target = target;
        }

        @Override
        public boolean contains(T shape) {
            return getTarget().equals(shape);//getSource().equals(shape)||getTarget().equals(shape);
        }
    }


    public static class ShapeEndEvent<T extends TrackingShape> extends ShapeEvent.ShapeSingleSourceEvent<T> {
        public ShapeEndEvent() {
            super();
        }

        public ShapeEndEvent(T source) {
            super(source);
        }
    }

    public static class ShapeStartEvent<T extends TrackingShape> extends ShapeEvent.ShapeSingleSourceEvent<T> {
        public ShapeStartEvent() {
            super();
        }

        public ShapeStartEvent(T source) {
            super(source);
        }
    }

}
