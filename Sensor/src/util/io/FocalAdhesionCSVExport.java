package util.io;

import core.image.CorrelationData;
import core.image.Entry;
import core.settings.Export;
import core.settings.Settings;
import focaladhesion.FocalAdhesion;
import focaladhesion.FocalAdhesionContainer;
import util.IOUtils;

import java.io.File;
import java.io.IOException;

public class FocalAdhesionCSVExport {

    public static void buildGeneral(StringBuilder builder, String ls, String info) {
        builder.append("#FocalAdhesion's CSV ").append(info).append(ls).
                append("#number,center_x,center_y,length_x,length_y,area,").
                append("aspect_ratio,orientation,").
                append("main_axis_start_x,main_axis_start_y,main_axis_end_x,main_axis_end_y,").
                append("side_axis_start_x,side_axis_start_y,side_axis_end_x,side_axis_end_y,keep").
                append(ls);
    }

    /**
     * Builds one line of the FA CSV
     *
     * @param focalAdhesion
     * @param builder
     */
    public static void buildFACsvLine(FocalAdhesion focalAdhesion, StringBuilder builder, String ls) {
        builder.append(focalAdhesion.getNumber()).
                append(",").
                append(focalAdhesion.getCenter().getX()).
                append(",").
                append(focalAdhesion.getCenter().getY()).
                append(",").
                append(focalAdhesion.getLengthMainAxis()).
                append(",").
                append(focalAdhesion.getLengthSideAxis()).
                append(",").
                append(focalAdhesion.getArea()).
                append(",").
                append(focalAdhesion.getAspectRatio()).
                append(",").
                append(focalAdhesion.getOrientation()).
                append(",").
                append(focalAdhesion.getMainAxisStart().getX()).
                append(",").
                append(focalAdhesion.getMainAxisStart().getY()).
                append(",").
                append(focalAdhesion.getMainAxisEnd().getX()).
                append(",").
                append(focalAdhesion.getMainAxisEnd().getY()).
                append(",").
                append(focalAdhesion.getSideAxisStart().getX()).
                append(",").
                append(focalAdhesion.getSideAxisStart().getY()).
                append(",").
                append(focalAdhesion.getSideAxisEnd().getX()).
                append(",").
                append(focalAdhesion.getSideAxisEnd().getY()).
                append(",").
                append(focalAdhesion.isKeep() ? 1 : 0).
                append(ls);

    }

    public static void exportFocalAdhesionCSVGrouped(File path, Entry entry, Settings dp) throws IOException {
        String ls = System.lineSeparator();

        StringBuilder builder = new StringBuilder("");

        CorrelationData data = entry.getCorrelationData();
        Settings clonedDp = dp.clone();
        if (data == null) return;
        if (!(data instanceof FocalAdhesionContainer)) return;

        buildGeneral(builder, ls, "Grouped");

        builder.append("#FA's verify multiple fibers").append(ls);
        //group multiple used FA's
        clonedDp.setProperty(Export.hideUnusedFAs, 1);
        clonedDp.setProperty(Export.hideSingleUsedFAs, 1);
        clonedDp.setProperty(Export.hideMultiUsedFAs, 0);
        ((FocalAdhesionContainer) data).
                getFilteredData(entry.getDataFilament(), clonedDp).
                forEach(focalAdhesion -> buildFACsvLine(focalAdhesion, builder, ls));

        builder.append("#FA's verify single fibers").append(ls);
        //group single used FA's
        clonedDp.setProperty(Export.hideUnusedFAs, 1);
        clonedDp.setProperty(Export.hideSingleUsedFAs, 0);
        clonedDp.setProperty(Export.hideMultiUsedFAs, 1);
        ((FocalAdhesionContainer) data).
                getFilteredData(entry.getDataFilament(), clonedDp).
                forEach(focalAdhesion -> buildFACsvLine(focalAdhesion, builder, ls));

        builder.append("#FA's unused").append(ls);
        //group unused
        clonedDp.setProperty(Export.hideUnusedFAs, 0);
        clonedDp.setProperty(Export.hideSingleUsedFAs, 1);
        clonedDp.setProperty(Export.hideMultiUsedFAs, 1);
        ((FocalAdhesionContainer) data).
                getFilteredData(entry.getDataFilament(), clonedDp).
                forEach(focalAdhesion -> buildFACsvLine(focalAdhesion, builder, ls));

        File file = IOUtils.getOutFileFromImageFile(new File(entry.getPath()), path, ".csv", "adhesion_grouped");
        IOUtils.writeFile(file, builder.toString());
    }

    /**
     * @param path
     * @param entry
     * @param dp
     * @throws IOException
     */
    public static void exportFocalAdhesionCSV(File path, Entry entry, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder("");
        CorrelationData data = entry.getCorrelationData();
        if (data == null) return;
        if (!(data instanceof FocalAdhesionContainer)) return;

        buildGeneral(builder, ls, "");

        ((FocalAdhesionContainer) data).
                getFilteredData(entry.getDataFilament(), dp).
                forEach(focalAdhesion -> buildFACsvLine(focalAdhesion, builder, ls));
        File file = IOUtils.getOutFileFromImageFile(new File(entry.getPath()), path, ".csv", "adhesion");
        IOUtils.writeFile(file, builder.toString());
    }
}
