package tracking.area;

import core.cell.CellShape;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public abstract class CellEvent implements Serializable {//marker

    /**
     * @param shape shape which should be checked
     * @return Returns boolean value true if the given shape is part of the cell event otherwise false
     */
    public abstract boolean contains(CellShape shape);


    //region CellEvent Hierarchy
    public static class CellSingleSourceEvent extends CellEvent {
        private CellShape source;

        public CellSingleSourceEvent() {
            super();
        }

        public CellSingleSourceEvent(CellShape source) {
            this();
            setSource(source);
        }

        public CellShape getSource() {
            return source;
        }

        public void setSource(CellShape source) {
            this.source = source;
        }


        @Override
        public boolean contains(CellShape shape) {
            return source.equals(shape);
        }
    }

    public static class CellFusionEvent extends CellEvent {
        private List<CellShape> source;
        private CellShape target;

        public CellFusionEvent() {
            super();
            source = new ArrayList<>();
        }

        public CellFusionEvent(List<CellShape> source, CellShape target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<CellShape> getSource() {
            return source;
        }

        public void setSource(List<CellShape> source) {
            this.source = source;
        }

        public CellShape getTarget() {
            return target;
        }

        public void setTarget(CellShape target) {
            this.target = target;
        }

        @Override
        public boolean contains(CellShape shape) {
            return target.equals(shape);//||source.contains(shape);
        }
    }

    public static class CellTouchEvent extends CellEvent {
        private List<CellShape> source;
        private CellShape target;

        public CellTouchEvent() {
            super();
            source = new ArrayList<>();
        }

        public CellTouchEvent(List<CellShape> source, CellShape target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<CellShape> getSource() {
            return source;
        }

        public void setSource(List<CellShape> source) {
            this.source = source;
        }

        public CellShape getTarget() {
            return target;
        }

        public void setTarget(CellShape target) {
            this.target = target;
        }

        @Override
        public boolean contains(CellShape shape) {
            return target.equals(shape);//source.contains(shape)||target.equals(shape);
        }
    }


    public static class CellSplitEvent extends CellSingleSourceEvent {
        private List<CellShape> target;

        public CellSplitEvent() {
            super();
            target = new ArrayList<>();
        }

        public CellSplitEvent(CellShape source, List<CellShape> target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<CellShape> getTarget() {
            return target;
        }

        public void setTarget(List<CellShape> target) {
            this.target = target;
        }

        @Override
        public boolean contains(CellShape shape) {
            return target.contains(shape);//getSource().equals(shape)||target.contains(shape);
        }
    }

    public static class CellDeTouchEvent extends CellSingleSourceEvent {

        private List<CellShape> target;

        public CellDeTouchEvent() {
            super();
            target = new ArrayList<>();
        }

        public CellDeTouchEvent(CellShape source, List<CellShape> target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public List<CellShape> getTarget() {
            return target;
        }

        public void setTarget(List<CellShape> target) {
            this.target = target;
        }

        @Override
        public boolean contains(CellShape shape) {
            return target.contains(shape);//getSource().equals(shape)||target.contains(shape);
        }
    }

    public static class CellAliveEvent extends CellSingleSourceEvent {
        private CellShape target;

        public CellAliveEvent() {
            super();
        }

        public CellAliveEvent(CellShape source, CellShape target) {
            this();
            setSource(source);
            setTarget(target);
        }

        public CellShape getTarget() {
            return target;
        }

        public void setTarget(CellShape target) {
            this.target = target;
        }

        @Override
        public boolean contains(CellShape shape) {

            return getTarget().equals(shape);//getSource().equals(shape)||getTarget().equals(shape);
        }
    }


    public static class CellEndEvent extends CellSingleSourceEvent {
        public CellEndEvent() {
            super();
        }

        public CellEndEvent(CellShape source) {
            super(source);
        }
    }

    public static class CellStartEvent extends CellSingleSourceEvent {
        public CellStartEvent() {
            super();
        }

        public CellStartEvent(CellShape source) {
            super(source);
        }
    }
//endregion
}



