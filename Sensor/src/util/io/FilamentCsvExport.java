package util.io;

import core.Const;
import core.FilamentSensor;
import core.cell.CellShape;
import core.cell.DataFilaments;
import core.cell.OrientationFieldContainer;
import core.filaments.AbstractFilament;
import core.filaments.FilamentChain;
import core.filaments.SingleFilament;
import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.Export;
import core.settings.SFTracking;
import core.settings.Settings;
import tracking.filament.DataTracking;
import tracking.filament.DynamicFilament;
import util.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FilamentCsvExport {


    public static void buildGeneral(StringBuilder builder, String ls, Entry entry) {

        if (entry.getInteriorContainer().getInteriorData() == null) {
            entry.getInteriorContainer().setInteriorData(new long[]{-1, -1, -1});
        }

        CellShape cellData = entry.getShape().getSelectedArea();
        long[] m_interior_data = entry.getInteriorContainer().getInteriorData();
        DataFilaments m_filament_container = entry.getDataFilament();

        if (cellData == null || cellData.getCenter() == null) {
            //handling?
        }
        //if cellData not available (no cell found) set values to 0
        builder.append("#").
                append(FilamentSensor.NAME + "-" + FilamentSensor.VERSION).append(ls).
                append("#cell_properties").append(ls).
                append("%cell_identifier").append(ls).
                append((cellData != null) ? cellData.getIdentifier() : -1).append(ls).//add area tracking identifier to csv
                append("%center_x").append(ls).
                append((cellData != null) ? cellData.getCenter().x : 0).append(ls).
                append("%center_y").append(ls).
                append((cellData != null) ? cellData.getCenter().y : 0).append(ls).
                append("%area").append(ls).
                append((cellData != null) ? cellData.getArea() : 0).append(ls).
                append("%aspect_ratio").append(ls).
                append((cellData != null) ? cellData.getAspectRatio() / Const.MF : 0).append(ls).
                append("%long_half_axis").append(ls).
                append((cellData != null) ? cellData.getLongHalfAxis() / Const.MF : 0).append(ls).
                append("%orientation").append(ls).
                append((cellData != null) ? cellData.getOrientation() / Const.MF : 0).append(ls).
                append("%order_parameter").append(ls).
                append((cellData != null) ? cellData.getOrderParameter() / Const.MF : 0).append(ls).
                append("%new_orientation").append(ls).
                append((cellData != null) ? cellData.getNewOrientation() / Const.MF : 0).append(ls).
                append("%new_order_parameter").append(ls).
                append((cellData != null) ? cellData.getNewOrderParameter() / Const.MF : 0).append(ls).
                append("%area_interior").append(ls).
                append(m_interior_data[0]).append(ls).
                append("%mean_brightness_interior").append(ls).
                append(m_interior_data[1] / Const.MF).append(ls).
                append("%area_boundary").append(ls).
                append((cellData != null) ? (cellData.getArea() - m_interior_data[0]) : 0).append(ls).
                append("%mean_brightness_boundary").append(ls).
                append(m_interior_data[2] / Const.MF).append(ls).
                append("%excursion_ratio").append(ls).
                append((cellData != null) ? cellData.getExcursion() / Const.MF : 0).append(ls).
                append("%number_filaments").append(ls).
                append(m_filament_container.count()).append(ls);

        /*
        //if cellData not available (no cell found) set values to 0
            String out = "#" + FilamentSensor.NAME + "-" + FilamentSensor.VERSION + ls +
            "#cell_properties" + ls +
            "%center_x" + ls + ((cellData != null) ? cellData.getCenter().x : 0) + ls +
            "%center_y" + ls + ((cellData != null) ? cellData.getCenter().y : 0) + ls +
            "%area" + ls + ((cellData != null) ? cellData.getArea() : 0) + ls +
            "%aspect_ratio" + ls + ((cellData != null) ? cellData.getAspectRatio() / Const.MF : 0) + ls +
            "%long_half_axis" + ls + ((cellData != null) ? cellData.getLongHalfAxis() / Const.MF : 0) + ls +
            "%orientation" + ls + ((cellData != null) ? cellData.getOrientation() / Const.MF : 0) + ls +
            "%order_parameter" + ls + ((cellData != null) ? cellData.getOrderParameter() / Const.MF : 0) + ls +
            "%new_orientation" + ls + ((cellData != null) ? cellData.getNewOrientation() / Const.MF : 0) + ls +
            "%new_order_parameter" + ls + ((cellData != null) ? cellData.getNewOrderParameter() / Const.MF : 0) + ls +
            "%area_interior" + ls + m_interior_data[0] + ls +
            "%mean_brightness_interior" + ls + m_interior_data[1] / Const.MF + ls +
            "%area_boundary" + ls + ((cellData != null) ? (cellData.getArea() - m_interior_data[0]) : 0) + ls +
            "%mean_brightness_boundary" + ls + m_interior_data[2] / Const.MF + ls +
            "%excursion_ratio" + ls + ((cellData != null) ? cellData.getExcursion() / Const.MF : 0) + ls +
            "%number_filaments" + ls + m_filament_container.count() + ls; */

    }

    /**
     * @param builder
     * @param ls
     * @param filaments
     * @param version   version of the csv document (1 ... only basic stuff, 2 ... with verified, 3 ... single filament tracking id ...)
     */
    public static void buildFilaments(StringBuilder builder, String ls, List<AbstractFilament> filaments, int version) {
        builder.append("%filaments").append(ls).
                append("#number,center_x,center_y,length,angle,width,signed_curv,total_curv");
        if (version > 1) builder.append(",verified");
        if (version > 2) builder.append(",dynFilamentNumber");
        builder.append(ls);
        filaments.forEach(fil -> {
            builder.append(fil.getNumber()).append(",").
                    append(fil.getCenter().x / 10.0).append(",").
                    append(fil.getCenter().y / 10.0).append(",").
                    append(fil.getLength() / Const.MF).append(",").
                    append(fil.getOrientation() / Const.MF).append(",").
                    append(fil.getWidth() / Const.MF);
            if (fil instanceof SingleFilament) {
                builder.append(",").
                        append(((SingleFilament) fil).getSignedCurvature() / Const.MF).append(",").
                        append(((SingleFilament) fil).getAbsoluteCurvature() / Const.MF);
            }
            if (version > 1) builder.append(",").append(fil.isVerified());
            if (version > 2) builder.append(",").append(fil.getTrackingId());
            builder.append(ls);
        });

        /*
        if (m_filament_container.count() > 0) {
        out += "%filaments" + ls + "#number,center_x,center_y,length,angle,width,signed_curv,total_curv,verified" + ls;
        for (AbstractFilament fil : m_filament_container.getFilaments()) {
            out += fil.getNumber() + "," + fil.getCenter().x / 10.0 + "," + fil.getCenter().y / 10.0 + "," +
                    fil.getLength() / Const.MF + "," + fil.getOrientation() / Const.MF + "," +
                    fil.getWidth() / Const.MF;
            if (fil instanceof SingleFilament) {
                out += "," + ((SingleFilament) fil).getSignedCurvature() / Const.MF + "," +
                        ((SingleFilament) fil).getAbsoluteCurvature() / Const.MF;
            }
            out += "," + fil.isVerified() + ls;
        }
        }

         */

    }

    public static void buildOrientationFields(StringBuilder builder, String ls, OrientationFieldContainer orientationFieldContainer) {
        int[][][] m_orientation_fields = orientationFieldContainer.getOrientationFields();
        Map<Integer, List<AbstractFilament>> m_filaments_by_orientation_field = orientationFieldContainer.getFilamentsByOrientationField();
        int[][] m_orientation_field_shapes = orientationFieldContainer.getOrientationFieldShapes();

        for (int i = 1; i < m_orientation_fields.length; i++) {
            List<AbstractFilament> filaments = null;
            if (m_filaments_by_orientation_field != null &&
                    m_filaments_by_orientation_field.size() > i) {
                filaments = m_filaments_by_orientation_field.get(i);
            }
            final int n = i;
            buildOrientationField(ls, m_orientation_field_shapes[i],
                    m_orientation_fields[i], filaments).
                    forEach(s -> builder.append("%orientation_field_").append(n).append("_").append(s));
        }

        /*for (int i = 1; i < m_orientation_fields.length; i++) {
                List<AbstractFilament> filaments = null;
                if (m_filaments_by_orientation_field != null &&
                        m_filaments_by_orientation_field.size() > i) {
                    filaments = m_filaments_by_orientation_field.get(i);
                }
                FilamentSensor.debugMessage("Field " + i + " data: " + m_orientation_field_shapes[i]);
                for (String s : processOrientationField(m_orientation_field_shapes[i],
                        m_orientation_fields[i], countBlocks(m_orientation_fields), filaments)) {
                    out += "%orientation_field_" + i + "_" + s;
                }
            }*/

    }

    private static int countBlocks(int[][][] m_orientation_fields) {
        List<Point> points = new ArrayList<>();
        for (int[][] field : m_orientation_fields) {
            for (int[] p : field) {
                Point tmp = new Point(p[0], p[1]);
                if (!points.contains(tmp)) {
                    points.add(tmp);
                }
            }
        }
        return points.size();
    }


    public static List<String> buildOrientationField(final String ls, int[] shape_data, int[][] field,
                                                     List<AbstractFilament> filaments) {
        // Moments
        double m_o = 0, m_oo = 0, m2_o = 0, m2_oo = 0;
        double n = field.length;
        for (int[] p : field) {
            m_o += p[2];
            m_oo += p[2] * p[2];
            int alt_ori = (p[2] + 90) % 180;
            m2_o += alt_ori;
            m2_oo += alt_ori * alt_ori;
        }
        m_o /= n;
        m2_o /= n;
        m_oo = Math.sqrt(m_oo / n - m_o * m_o);
        m2_oo = Math.sqrt(m2_oo / n - m2_o * m2_o);
        List<String> out = new ArrayList<>();
        out.add("center_x" + ls + shape_data[0] + ls);
        out.add("center_y" + ls + shape_data[1] + ls);
        out.add("area" + ls + (shape_data[2] / Const.MF) + ls);
        out.add("aspect_ratio" + ls + (shape_data[3] / Const.MF) + ls);
        out.add("long_half_axis" + ls + (shape_data[4] / Const.MF) + ls);
        out.add("alignment" + ls + (shape_data[5] / Const.MF) + ls);
        if (m2_oo < m_oo) {
            m_o = (m2_o + 90.) % 180.;
            m_oo = m2_oo;
        }
        out.add("orientation_mean" + ls + (Math.round(m_o * Const.MF) / Const.MF) + ls);
        out.add("orientation_sigma" + ls + (Math.round(m_oo * Const.MF) / Const.MF) + ls);

        int n_filaments = -1;
        double total_mass = -1;
        String filaments_text = "";
        if (filaments != null) {
            n_filaments = filaments.size();
            for (AbstractFilament f : filaments) {
                total_mass += f.getMass();
                filaments_text += f.getCenter().x / 10.0 + "," + f.getCenter().y / 10.0 + "," +
                        f.getLength() / Const.MF + "," + f.getOrientation() / Const.MF +
                        "," + f.getWidth() / Const.MF + ls;
            }
        }
        out.add("number_of_filaments" + ls + n_filaments + ls);
        out.add("mass" + ls + (Math.round(total_mass * Const.MF) / Const.MF) + ls);
        out.add("filaments" + ls + filaments_text);
        return out;
    }


    public static void exportDynamicFilamentCsvGroupedByTime(DataTracking tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;
        final boolean chkMin = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
        final int minLength = dp.getValue(SFTracking.existsInMin);
        final int maxLength = dp.getValue(SFTracking.existsInMax);

        //group filaments by dynamic filaments
        List<DynamicFilament> dynamicFilaments = tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength).
                stream().filter(DynamicFilament::isKeep).collect(Collectors.toList());
        builder.append("#Dynamic Filament(grouped by time) CSV version: " + csv_version).append(ls);
        //(Time,(DynFilNumber,Filament))
        Map<Integer, List<AbstractFilament>> filaments = new HashMap<>();
        AtomicInteger cnt = new AtomicInteger(0);
        //group data
        dynamicFilaments.forEach(dynamicFilament -> {
            int tmp = cnt.getAndIncrement();
            dynamicFilament.getFilaments().keySet().forEach(time -> {
                if (!filaments.containsKey(time)) filaments.put(time, new ArrayList<>());
                ((FilamentChain) dynamicFilament.getFilaments().get(time)).getFilaments().forEach(filament -> {
                    filament.setTrackingId(tmp);
                    filaments.get(time).add(filament);
                });
            });
        });

        filaments.keySet().forEach(time -> {
            builder.append("#time ").append(time).append(ls);
            buildFilaments(builder, ls, filaments.get(time), 3);
        });

        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "dynamic_filament_time");
        IOUtils.writeFile(file, builder.toString());


    }

    /**
     * @param tracking
     * @param csvPath
     * @param dp
     */
    public static void exportDynamicFilamentCsv(DataTracking tracking, ImageWrapper source, File csvPath, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        final int csv_version = 1;

        final boolean chkMin = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
        final int minLength = dp.getValue(SFTracking.existsInMin);
        final int maxLength = dp.getValue(SFTracking.existsInMax);


        //group filaments by dynamic filaments
        List<DynamicFilament> dynamicFilaments = tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength).
                stream().filter(DynamicFilament::isKeep).collect(Collectors.toList());


        builder.append("#Dynamic Filament(grouped by DynamicFilament) CSV version: " + csv_version).
                append(ls);
        AtomicInteger cnt = new AtomicInteger(0);
        dynamicFilaments.forEach(dynamicFilament -> {
            builder.append("#dynamicFilament-number ").append(cnt).append(ls).
                    append("%birth,death,length").append(ls).
                    append(dynamicFilament.getBirth()).append(",").
                    append(dynamicFilament.getDeath()).append(",").
                    append(dynamicFilament.getLength()).append(ls);
            dynamicFilament.getFilaments().keySet().forEach(time -> {
                builder.append("#time ").append(time).append(ls);
                buildFilaments(builder, ls, ((FilamentChain) dynamicFilament.getFilaments().get(time)).getFilaments(), 2);
                builder.append(ls);
            });
            cnt.incrementAndGet();
        });
        File file = IOUtils.getOutFileFromImageFile(new File(source.getEntryList().get(0).getPath()), csvPath, ".csv", "dynamic_filament");
        IOUtils.writeFile(file, builder.toString());
    }

    public static void exportFilamentsCSVGrouped(File path, Entry entry, Settings dp) throws IOException {
        final int csv_version = 2;
        final String ls = System.lineSeparator();
        Settings clonedDp = dp.clone();

        StringBuilder builder = new StringBuilder();
        builder.append("#Filaments CSV Grouped").append(ls);
        builder.append("#Csv-Version " + csv_version).append(ls);
        builder.append("#Fibers verified multiple times").append(ls);
        //group multiple used FA's
        clonedDp.setProperty(Export.hideNonVerifiedFibers, 1);
        clonedDp.setProperty(Export.hideSingleVerifiedFibers, 1);
        clonedDp.setProperty(Export.hideMultiVerifiedFibers, 0);
        buildFilaments(builder, ls, entry.getDataFilament().getFilteredFilaments(clonedDp), csv_version);

        builder.append("#Fibers verified one time").append(ls);
        //group single used FA's
        clonedDp.setProperty(Export.hideNonVerifiedFibers, 1);
        clonedDp.setProperty(Export.hideSingleVerifiedFibers, 0);
        clonedDp.setProperty(Export.hideMultiVerifiedFibers, 1);
        buildFilaments(builder, ls, entry.getDataFilament().getFilteredFilaments(clonedDp), csv_version);

        builder.append("#Fibers not verified").append(ls);
        //group unused
        clonedDp.setProperty(Export.hideNonVerifiedFibers, 0);
        clonedDp.setProperty(Export.hideSingleVerifiedFibers, 1);
        clonedDp.setProperty(Export.hideMultiVerifiedFibers, 1);
        buildFilaments(builder, ls, entry.getDataFilament().getFilteredFilaments(clonedDp), csv_version);

        File file = IOUtils.getOutFileFromImageFile(new File(entry.getPath()), path, ".csv", "filaments_grouped");
        IOUtils.writeFile(file, builder.toString());

    }

    /**
     * Exports the list of filament chains as comma separated values.
     *
     * @param path   output directory
     * @param chains output filament-chains - obsolete?
     * @param entry  entry to export
     */
    public static void exportFilamentCSV(File path, boolean chains, Entry entry) throws IOException {
        final int csv_version = 2;//tells which data is included in the csv
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        buildGeneral(builder, ls, entry);
        if (entry.getDataFilament().count() > 0) {
            buildFilaments(builder, ls, entry.getDataFilament().getFilaments(), csv_version);
        }
        if (entry.getOrientationFieldContainer() != null && entry.getOrientationFieldContainer().getOrientationFields() != null) {
            buildOrientationFields(builder, ls, entry.getOrientationFieldContainer());
        }

        File file = IOUtils.getOutFileFromImageFile(new File(entry.getPath()), path, ".csv", chains ? "chains" : "filaments");
        IOUtils.writeFile(file, builder.toString());
    }
}
