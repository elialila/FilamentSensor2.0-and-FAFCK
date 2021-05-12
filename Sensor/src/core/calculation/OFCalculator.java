package core.calculation;

import core.*;
import core.filaments.AbstractFilament;
import core.settings.Settings;
import core.image.Entry;
import core.image.IBinaryImage;
import core.FilamentSensor;
import util.ImageFactory;
import util.ImageExporter;
import util.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The name is obviously only temporary
 * <p>
 * In this package the whole calculation of orderparameter, orientationfields
 * should be collected for now
 */
public class OFCalculator {


//region OrientationField stuff - should be moved somewhere else


    public static List<BufferedImage> calculateOrientationFields(Entry entry, IBinaryImage extMask, Settings dp, boolean show_images) throws IOException {
        if (extMask != null && extMask.isInMemoryState()) {
            extMask = extMask.clone();
            extMask.exitMemoryState();
        }

        int[][] orientation_field = entry.getOrientationFieldContainer().getOrientationField();
        OrientationFields calculator = new OrientationFields((entry.getShape().getSelectedArea() != null) ? entry.getShape().getSelectedArea().getArea() : 0, extMask, orientation_field, entry.getDataFilament().getFilaments());
        Pair<Misc.Int3D, Map<Integer, List<AbstractFilament>>> pair = calculator.label(dp);
        if (pair == null) {
            return null;
        }

        entry.getOrientationFieldContainer().setOrientationFields(pair.getKey().the);
        entry.getOrientationFieldContainer().setOrientationFieldIds(new ArrayList<>(pair.getValue().keySet()));
        entry.getOrientationFieldContainer().setFilamentsByOrientationField(pair.getValue());
        int[][][] m_orientation_fields = pair.getKey().the;

        //only debug stuff, remove it?
        Map<Integer, List<AbstractFilament>> m_filaments_by_orientation_field = pair.getValue();
        for (Integer key : m_filaments_by_orientation_field.keySet()) {
            FilamentSensor.debugMessage("Field " + key + " has " +
                    m_filaments_by_orientation_field.get(key).size() + " filaments.");
        }

        determineOrientationFieldShapes(entry);
        if (show_images) {
            List<BufferedImage> images = new ArrayList<>();

            for (int i = 1; i < m_orientation_fields.length; i++) {
                images.add(ImageFactory.makeColorImage(calculator.makeImage(m_orientation_fields[i]), true));
            }
            return images;
        }
        return null;
    }

    private static void determineOrientationFieldShapes(Entry entry) throws IOException {
        int[][][] m_orientation_fields = entry.getOrientationFieldContainer().getOrientationFields();
        Map<Integer, List<AbstractFilament>> filamentsByOrientationField = entry.getOrientationFieldContainer().getFilamentsByOrientationField();

        int[][] m_orientation_field_shapes = new int[m_orientation_fields.length][];

        if (entry.getShape().getSelectedArea() == null) {
            entry.getOrientationFieldContainer().setOrientationFieldShapes(m_orientation_field_shapes);
            return;
        }
        IBinaryImage area = entry.getShape().getSelectedArea().getBinaryImage();
        if (area.isInMemoryState()) {
            area = area.clone();
            area.exitMemoryState();
        }


        final int diameter = (area.getWidth() >= area.getHeight()) ? (int) Math.ceil(((double) area.getWidth() / 13)) : (int) Math.ceil(((double) area.getHeight() / 13));
        for (int i = 1; i < m_orientation_fields.length; i++) {
            boolean[][] shape = new boolean[area.getWidth()][area.getHeight()];
            int width = 0;
            for (AbstractFilament filament : filamentsByOrientationField.get(i)) {
                filament.drawToImage(shape);
                int this_width = (int) ((filament.getWidth() - 1) / Const.M) + 1; // Ceiling
                if (this_width > width) {
                    width = this_width;
                }
            }
            //dilate mask creates problem on small pictures, choose the masksize dependant to image size
            //Include mask in morphological function to speed up.
            //changed from 100+with | 100 to diameter+width | diameter
            shape = Calc.dilate(shape, Calc.circleMask(diameter + width));
            shape = Calc.erode(shape, Calc.circleMask(diameter));
            //shape = Calc.dilateAndErode(shape, Calc.circleMask(100 + width), Calc.circleMask(100));
            for (int x = 0; x < shape.length; x++) {
                for (int y = 0; y < shape[0].length; y++) {
                    shape[x][y] = shape[x][y] && area.getPixel(x, y);
                }
            }

            m_orientation_field_shapes[i] = shapeMoments(shape, entry.getShape().getSelectedArea().getArea());

            if (Arrays.stream(m_orientation_field_shapes[i]).filter(item -> item != -1).count() == 0) {//write cell where no shape was found
                FilamentSensor.debugError("Current(Image):" + entry.getPath());
            }


            FilamentSensor.debugMessage("Field " + i + " data: " + m_orientation_field_shapes[i]);
        }
        entry.getOrientationFieldContainer().setOrientationFieldShapes(m_orientation_field_shapes);
    }


    private static void setOrientationFields(int[][][] set, Entry entry) throws IOException {

        Pair<Misc.Int3D, Map<Integer, List<AbstractFilament>>> pair = null;
        pair = OrientationFields.sortFilaments(set, entry.getDataFilament().getFilaments());
        entry.getOrientationFieldContainer().setOrientationFields(pair.getKey().the);
        entry.getOrientationFieldContainer().setFilamentsByOrientationField(pair.getValue());
        determineOrientationFieldShapes(entry);
    }

    private static boolean orientationFieldPostProcessingInnerLoop(OrientationFieldPostProcessing post, int[][][] set1, int[][][] set2, Entry entry1, Entry entry2) {
        if (post.sort(set1, set2)) {
            try {
                setOrientationFields(post.get(false), entry1);
                setOrientationFields(post.get(true), entry2);
                return true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static void orientationFieldPostProcessing(List<Entry> entryList) {
        FilamentSensor.debugMessage("Start orientation field post-processing.");
        boolean changed = true;
        OrientationFieldPostProcessing post = new OrientationFieldPostProcessing();
        while (changed) {
            changed = false;

            for (int i = 1; i < entryList.size(); i++) {
                int[][][] set1 = entryList.get(i - 1).getOrientationFieldContainer().getOrientationFields(),
                        set2 = entryList.get(i).getOrientationFieldContainer().getOrientationFields();
                changed = orientationFieldPostProcessingInnerLoop(post, set1, set2, entryList.get(i - 1), entryList.get(i));
            }
            for (int i = entryList.size() - 1; i > 0; i--) {
                int[][][] set1 = entryList.get(i - 1).getOrientationFieldContainer().getOrientationFields(),
                        set2 = entryList.get(i).getOrientationFieldContainer().getOrientationFields();
                changed = orientationFieldPostProcessingInnerLoop(post, set1, set2, entryList.get(i - 1), entryList.get(i));
            }
            if (changed) {
                FilamentSensor.debugMessage("Orientation fields changed. Re-running.");
            }
        }
        FilamentSensor.debugMessage("Orientation field post-processing finished.");
    }


    private static int[] shapeMoments(boolean[][] shape, long cellArea) throws IOException {
        final int width = shape.length,
                height = shape[0].length;
        double sum = 0, sum_x = 0, sum_y = 0, sum_xx = 0, sum_xy = 0, sum_yy = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (shape[x][y]) {
                    sum++;
                    sum_x += x;
                    sum_y += y;
                    sum_xx += x * x;
                    sum_xy += x * y;
                    sum_yy += y * y;
                }
            }
        }
        if (sum == 0) {
            FilamentSensor.debugError("No shape found!");
            return new int[]{-1, -1, -1, -1, -1, -1};
        }
        int mean_x = (int) Math.round(sum_x / sum);
        int mean_y = (int) Math.round(sum_y / sum);
        int area = (int) Math.round(sum * Const.MF / cellArea);
        double cov_xx = sum_xx / sum - sum_x * sum_x / (sum * sum);
        double cov_xy = sum_xy / sum - sum_x * sum_y / (sum * sum);
        double cov_yy = sum_yy / sum - sum_y * sum_y / (sum * sum);
        double ew_hi = (cov_xx + cov_yy) / 2.0 +
                Math.sqrt((cov_xx - cov_yy) * (cov_xx - cov_yy) / 4.0 + cov_xy * cov_xy);
        double ew_lo = (cov_xx + cov_yy) / 2.0 -
                Math.sqrt((cov_xx - cov_yy) * (cov_xx - cov_yy) / 4.0 + cov_xy * cov_xy);
        int aspect_ratio = (int) Math.round(Math.sqrt(ew_hi / ew_lo) * Const.MF);
        int long_half_axis = (int) Math.round(Math.sqrt(ew_hi) * Const.MF);
        int phi = (int) Math.round((180 - Math.atan2(ew_hi - cov_xx, cov_xy) / Const.RAD) * Const.MF);
        return new int[]{mean_x, mean_y, area, aspect_ratio, long_half_axis, phi};
    }


    /**
     * @param color
     * @return
     * @throws IOException
     */
    public static List<BufferedImage> orientationFieldImages(BufferedImage orig, Entry entry, Color color) throws IOException {
        FilamentSensor.debugMessage("Start making orientation field images.");
        List<BufferedImage> images = new ArrayList<>();
        if (entry.getOrientationFieldContainer().getFilamentsByOrientationField() == null) {
            //images.add(tmp);//add original or not?
            return images;
        }
        for (Integer key : entry.getOrientationFieldContainer().getFilamentsByOrientationField().keySet()) {
            ColorModel cm = orig.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = orig.copyData(orig.getRaster().createCompatibleWritableRaster());
            BufferedImage tmp = new BufferedImage(cm, raster, isAlphaPremultiplied, null);

            FilamentSensor.debugMessage("Make image " + key);
            if (key <= 0) {
                ImageExporter.addFilaments(tmp, entry.getOrientationFieldContainer().getFilamentsByOrientationField().get(key), color);
                images.add(0, tmp);
                continue;
            }
            ImageExporter.addFilaments(tmp, entry.getOrientationFieldContainer().getFilamentsByOrientationField().get(key), color);
            images.add(tmp);
        }
        return images;
    }
    //endregion OrientationField
}
