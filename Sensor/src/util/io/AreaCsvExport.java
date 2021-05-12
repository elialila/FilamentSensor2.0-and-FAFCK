package util.io;

import core.Const;
import core.cell.CellShape;
import core.image.ImageWrapper;
import core.settings.ATracking;
import core.settings.Settings;
import tracking.area.AreaTracker;
import tracking.area.CellEvent;
import tracking.area.DynamicArea;
import util.Annotations;
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

public class AreaCsvExport {

    public static void buildArea(StringBuilder builder, String ls, @Annotations.NotNull CellShape cellData, boolean withHeadline, int version) {
        if (withHeadline) {
            builder.append("#center_x").append(",").
                    append("center_y").append(",").
                    append("area").append(",").
                    append("aspect_ratio").append(",").
                    append("long_half_axis").append(",").
                    append("orientation").append(",").
                    append("order_parameter").append(",").
                    append("new_orientation").append(",").
                    append("new_order_parameter").append(",").
                    append("excursion_ratio").append(",").
                    append("dynArea_id").append(ls);
        }
        builder.append(cellData.getCenter().x).append(",").
                append(cellData.getCenter().y).append(",").
                append(cellData.getArea()).append(",").
                append(cellData.getAspectRatio() / Const.MF).append(",").
                append(cellData.getLongHalfAxis() / Const.MF).append(",").
                append(cellData.getOrientation() / Const.MF).append(",").
                append(cellData.getOrderParameter() / Const.MF).append(",").
                append(cellData.getNewOrientation() / Const.MF).append(",").
                append(cellData.getNewOrderParameter() / Const.MF).append(",").
                append(cellData.getExcursion() / Const.MF).append(",").
                append(cellData.getIdentifier()).append(ls);
    }

    /**
     * @param tracking
     * @param csvPath
     * @param dp
     */
    public static void exportDynamicAreaCsv(AreaTracker tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;

        final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int minLength = dp.getValue(ATracking.existsInMin);
        final int maxLength = dp.getValue(ATracking.existsInMax);


        //group filaments by dynamic filaments
        List<DynamicArea> dynamicAreas = tracking.filterUniqueAreas(chkMin, minLength, chkMax, maxLength).
                stream().filter(DynamicArea::isKeep).collect(Collectors.toList());


        builder.append("#Dynamic Area(grouped by DynamicArea) CSV version: " + csv_version).
                append(ls);
        AtomicInteger cnt = new AtomicInteger(0);
        dynamicAreas.forEach(dynamicArea -> {
            builder.append("#dynamicArea-number ").append(cnt).append(ls).
                    append("#birth,death,length").append(ls).
                    append(dynamicArea.getBirth()).append(",").
                    append(dynamicArea.getDeath()).append(",").
                    append(dynamicArea.getLength()).append(ls);
            AtomicBoolean headline = new AtomicBoolean(true);
            dynamicArea.getAreas().keySet().forEach(time -> {
                builder.append("#time ").append(time).append(ls);

                CellEvent event = dynamicArea.getAreas().get(time);
                builder.append("#event: ").append(event.getClass().getSimpleName()).append(ls);


                if (event instanceof CellEvent.CellAliveEvent) {
                    CellShape shape = ((CellEvent.CellAliveEvent) event).getTarget();
                    buildArea(builder, ls, shape, headline.getAndSet(false), 1);
                } else if (event instanceof CellEvent.CellSplitEvent) {
                    ((CellEvent.CellSplitEvent) event).getTarget().forEach(shape -> buildArea(builder, ls, shape, headline.getAndSet(false), 1));
                } else if (event instanceof CellEvent.CellDeTouchEvent) {
                    ((CellEvent.CellDeTouchEvent) event).getTarget().forEach(shape -> buildArea(builder, ls, shape, headline.getAndSet(false), 1));
                } else if (event instanceof CellEvent.CellSingleSourceEvent) {//start and end event
                    CellShape shape = ((CellEvent.CellSingleSourceEvent) event).getSource();
                    buildArea(builder, ls, shape, headline.getAndSet(false), 1);
                } else if (event instanceof CellEvent.CellFusionEvent) {
                    CellShape shape = ((CellEvent.CellFusionEvent) event).getTarget();
                    buildArea(builder, ls, shape, headline.getAndSet(false), 1);
                } else if (event instanceof CellEvent.CellTouchEvent) {
                    CellShape shape = ((CellEvent.CellTouchEvent) event).getTarget();
                    buildArea(builder, ls, shape, headline.getAndSet(false), 1);
                }


                builder.append(ls);
            });
            cnt.incrementAndGet();
        });
        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "dynamic_area");
        IOUtils.writeFile(file, builder.toString());
    }

    public static void exportDynamicAreaCsvGroupedByTime(AreaTracker tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;
        final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int minLength = dp.getValue(ATracking.existsInMin);
        final int maxLength = dp.getValue(ATracking.existsInMax);

        //group area by dynamic area
        List<DynamicArea> dynamicAreas = tracking.filterUniqueAreas(chkMin, minLength, chkMax, maxLength).
                stream().filter(DynamicArea::isKeep).collect(Collectors.toList());
        builder.append("#Dynamic Area(grouped by time) CSV version: " + csv_version).append(ls);
        //(Time,(DynAreaNumber,Area))
        Map<Integer, List<CellShape>> areas = new HashMap<>();
        AtomicInteger cnt = new AtomicInteger(0);
        //group data
        dynamicAreas.forEach(dynamicArea -> {
            int tmp = cnt.getAndIncrement();
            dynamicArea.getAreas().keySet().forEach(time -> {

                if (!areas.containsKey(time)) areas.put(time, new ArrayList<>());
                CellEvent event = dynamicArea.getAreas().get(time);

                if (event instanceof CellEvent.CellAliveEvent) {
                    areas.get(time).add(((CellEvent.CellAliveEvent) event).getTarget());
                } else if (event instanceof CellEvent.CellSplitEvent) {
                    areas.get(time).addAll(((CellEvent.CellSplitEvent) event).getTarget());
                } else if (event instanceof CellEvent.CellDeTouchEvent) {
                    areas.get(time).addAll(((CellEvent.CellDeTouchEvent) event).getTarget());
                } else if (event instanceof CellEvent.CellSingleSourceEvent) {//start and end event
                    areas.get(time).add(((CellEvent.CellSingleSourceEvent) event).getSource());
                } else if (event instanceof CellEvent.CellFusionEvent) {
                    areas.get(time).add(((CellEvent.CellFusionEvent) event).getTarget());
                } else if (event instanceof CellEvent.CellTouchEvent) {
                    areas.get(time).add(((CellEvent.CellTouchEvent) event).getTarget());
                }
            });
        });

        areas.keySet().forEach(time -> {
            builder.append("#time ").append(time).append(ls);
            AtomicBoolean headline = new AtomicBoolean(true);
            areas.get(time).forEach(cell -> buildArea(builder, ls, cell, headline.getAndSet(false), 1));
        });

        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "dynamic_area_time");
        IOUtils.writeFile(file, builder.toString());


    }
}
