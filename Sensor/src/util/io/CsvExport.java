package util.io;

import core.image.ImageWrapper;
import core.settings.ATracking;
import core.settings.Settings;
import tracking.shape.ShapeEvent;
import tracking.shape.ShapeTracker;
import tracking.shape.TimeTable;
import tracking.shape.TrackingShape;
import util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CsvExport {


    /**
     * @param tracking
     * @param csvPath
     * @param dp
     */
    public static <T extends TrackingShape> void exportTimeTableCsvGroupedByShape(ShapeTracker<T> tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;

        final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int minLength = dp.getValue(ATracking.existsInMin);
        final int maxLength = dp.getValue(ATracking.existsInMax);


        //group filaments by dynamic filaments
        List<TimeTable<T>> filteredTimeTables = tracking.filterUniqueShapes(chkMin, minLength, chkMax, maxLength).stream().filter(TimeTable::isKeep).collect(Collectors.toList());


        builder.append("#Time Table(grouped by Shape) CSV version: " + csv_version).
                append(ls);
        AtomicInteger cnt = new AtomicInteger(0);
        filteredTimeTables.forEach(timeTable -> {
            builder.append("#time-table-number ").append(cnt).append(ls).
                    append("#birth,death,length").append(ls).
                    append(timeTable.getBirth()).append(",").
                    append(timeTable.getDeath()).append(",").
                    append(timeTable.getLength()).append(ls);
            AtomicBoolean headline = new AtomicBoolean(true);
            timeTable.getShapes().keySet().forEach(time -> {
                builder.append("#time ").append(time).append(ls);

                ShapeEvent<T> event = timeTable.getShapes().get(time);
                builder.append("#event: ").append(event.getClass().getSimpleName()).append(ls);


                //@todo implement a general buildArea method or add it to the tracking shape interface "getCsv"
                //getCsv(StringBuilder,LineSeparator,headlineSwitch,csvVersion)
                //check how the csv export would be done in focaladhesion


                if (event instanceof ShapeEvent.ShapeAliveEvent) {
                    T shape = ((ShapeEvent.ShapeAliveEvent<T>) event).getTarget();
                    shape.buildCsv(builder, headline.getAndSet(false), 1);
                } else if (event instanceof ShapeEvent.ShapeSplitEvent) {
                    ((ShapeEvent.ShapeSplitEvent<T>) event).getTarget().forEach(shape -> shape.buildCsv(builder, headline.getAndSet(false), 1));
                } else if (event instanceof ShapeEvent.ShapeDeTouchEvent) {
                    ((ShapeEvent.ShapeDeTouchEvent<T>) event).getTarget().forEach(shape -> shape.buildCsv(builder, headline.getAndSet(false), 1));
                } else if (event instanceof ShapeEvent.ShapeSingleSourceEvent) {//start and end event
                    T shape = ((ShapeEvent.ShapeSingleSourceEvent<T>) event).getSource();
                    shape.buildCsv(builder, headline.getAndSet(false), 1);
                } else if (event instanceof ShapeEvent.ShapeFusionEvent) {
                    T shape = ((ShapeEvent.ShapeFusionEvent<T>) event).getTarget();
                    shape.buildCsv(builder, headline.getAndSet(false), 1);
                } else if (event instanceof ShapeEvent.ShapeTouchEvent) {
                    T shape = ((ShapeEvent.ShapeTouchEvent<T>) event).getTarget();
                    shape.buildCsv(builder, headline.getAndSet(false), 1);
                }


                builder.append(ls);
            });
            cnt.incrementAndGet();
        });
        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "time_table_shape");
        IOUtils.writeFile(file, builder.toString());
    }

    public static <T extends TrackingShape> void exportTimeTableCsvGroupedByTime(ShapeTracker<T> tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;
        final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int minLength = dp.getValue(ATracking.existsInMin);
        final int maxLength = dp.getValue(ATracking.existsInMax);

        //group area by dynamic area
        List<TimeTable<T>> filteredTimeTables = tracking.filterUniqueShapes(chkMin, minLength, chkMax, maxLength).
                stream().filter(TimeTable::isKeep).collect(Collectors.toList());
        builder.append("#Time Table(grouped by time) CSV version: " + csv_version).append(ls);
        //(Time,(DynAreaNumber,Area))
        Map<Integer, List<T>> areas = new HashMap<>();
        //group data
        filteredTimeTables.forEach(timeTable -> timeTable.getShapes().keySet().forEach(time -> {
            if (!areas.containsKey(time)) areas.put(time, new ArrayList<>());
            ShapeEvent<T> event = timeTable.getShapes().get(time);
            if (event instanceof ShapeEvent.ShapeAliveEvent) {
                areas.get(time).add(((ShapeEvent.ShapeAliveEvent<T>) event).getTarget());
            } else if (event instanceof ShapeEvent.ShapeSplitEvent) {
                areas.get(time).addAll(((ShapeEvent.ShapeSplitEvent<T>) event).getTarget());
            } else if (event instanceof ShapeEvent.ShapeDeTouchEvent) {
                areas.get(time).addAll(((ShapeEvent.ShapeDeTouchEvent<T>) event).getTarget());
            } else if (event instanceof ShapeEvent.ShapeSingleSourceEvent) {//start and end event
                areas.get(time).add(((ShapeEvent.ShapeSingleSourceEvent<T>) event).getSource());
            } else if (event instanceof ShapeEvent.ShapeFusionEvent) {
                areas.get(time).add(((ShapeEvent.ShapeFusionEvent<T>) event).getTarget());
            } else if (event instanceof ShapeEvent.ShapeTouchEvent) {
                areas.get(time).add(((ShapeEvent.ShapeTouchEvent<T>) event).getTarget());
            }
        }));

        areas.keySet().forEach(time -> {
            builder.append("#time ").append(time).append(ls);
            AtomicBoolean headline = new AtomicBoolean(true);
            areas.get(time).forEach(cell -> cell.buildCsv(builder, headline.getAndSet(false), 1));
        });

        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "time_table_time");
        IOUtils.writeFile(file, builder.toString());


    }


}
