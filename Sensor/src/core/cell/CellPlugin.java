package core.cell;

import ij.Prefs;
import util.Annotations.NotNull;
import util.Annotations.Nullable;
import core.Const;

import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import core.image.BinaryImage;
import core.image.IBinaryImage;

import java.awt.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public abstract class CellPlugin implements Serializable {

    private boolean memoryState = true;

    public CellPlugin() {
    }

    public boolean isMemoryState() {
        return memoryState;
    }

    public void setMemoryState(boolean memoryState) {
        this.memoryState = memoryState;
    }

    /**
     * Does not change input, just calculates the area Data
     *
     * @param binary_image
     */
    public static void initialize(IBinaryImage binary_image, CellShape shape, boolean skipExcursionImage) {

        final int width = binary_image.getWidth();
        final int height = binary_image.getHeight();

        double sum_x = 0, sum_y = 0, sum_xx = 0, sum_xy = 0, sum_yy = 0;
        double area = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (binary_image.getPixel(x, y)) {
                    area++;
                    sum_x += x;
                    sum_y += y;
                    sum_xx += x * x;
                    sum_xy += x * y;
                    sum_yy += y * y;
                }
            }
        }

        shape.setArea((int) area);
        if (area == 0) {
            shape.setOrientation(0);
            shape.setNewOrientation(0);
            shape.setAspectRatio(0);
            shape.setOrderParameter(0);
            shape.setNewOrderParameter(0);
            shape.setCenter(new Point(-1, -1));
            //change
            shape.setBinaryImage(binary_image);
            //@todo create logger
            return;//@todo removed exception temporary
            //throw new RuntimeException("No cell found");
        }

        int mean_x = (int) Math.round(sum_x / area);
        int mean_y = (int) Math.round(sum_y / area);
        double cov_xx = sum_xx / area - sum_x * sum_x / (area * area);
        double cov_xy = sum_xy / area - sum_x * sum_y / (area * area);
        double cov_yy = sum_yy / area - sum_y * sum_y / (area * area);

        double ew_hi = (cov_xx + cov_yy) / 2.0 +
                Math.sqrt((cov_xx - cov_yy) * (cov_xx - cov_yy) / 4.0 + cov_xy * cov_xy);
        double ew_lo = (cov_xx + cov_yy) / 2.0 -
                Math.sqrt((cov_xx - cov_yy) * (cov_xx - cov_yy) / 4.0 + cov_xy * cov_xy);
        double phi = 180 - Math.atan2(ew_hi - cov_xx, cov_xy) / Const.RAD;

        // Calculate excursion ratio.
        double cos_phi = -Math.cos(phi * Const.RAD), sin_phi = Math.sin(phi * Const.RAD),
                aspect_ratio = Math.sqrt(ew_hi / ew_lo),
                a2 = area * aspect_ratio / Math.PI,
                b2 = area / (aspect_ratio * Math.PI);
        double excursion = 0;
        int[][] excursionImage = new int[width][height];
        for (int x = 0; x < width; x++) {
            double x_cent = x - sum_x / area, cx = cos_phi * x_cent, sx = -sin_phi * x_cent;
            for (int y = 0; y < height; y++) {
                double y_cent = y - sum_y / area,
                        x_rot = (cx + sin_phi * y_cent),
                        y_rot = (sx + cos_phi * y_cent);
                boolean in_ellipse = (x_rot * x_rot / a2 + y_rot * y_rot / b2 <= 1);

                if (binary_image.getPixel(x, y)) {
                    if (in_ellipse) {
                        excursionImage[x][y] = 2;
                    } else {
                        excursion++;
                        excursionImage[x][y] = 3;
                    }
                } else if (in_ellipse) {
                    excursionImage[x][y] = 1;
                }
            }
        }
        excursion /= area;

        if (!skipExcursionImage) {
            shape.setExcursionImage(excursionImage);
        } else {
            shape.setExcursionImage(null);
        }

        shape.setOrientation((long) (Const.M * phi));
        shape.setNewOrientation(-1000 * Const.M);
        shape.setAspectRatio((long) (Const.M * Math.sqrt(ew_hi / ew_lo)));
        shape.setCenter(new Point(mean_x, mean_y));
        shape.setOrderParameter(-1000 * Const.M);
        shape.setNewOrderParameter(-1000 * Const.M);
        shape.setExcursion((long) (Const.M * excursion));
        shape.setLongHalfAxis((long) Math.sqrt(shape.getArea() * Const.MF * shape.getAspectRatio() / Math.PI));
    }

    /**
     * Calculates the order parameter and store it in shape
     *
     * @param orientation_field
     * @param shape
     */
    public static final void calculateOrderParameter(int[][] orientation_field, CellShape shape) {

        double sin_sum = 0.0;
        double cos_sum = 0.0;
        double old_numerator = 0.0;
        double new_numerator = 0.0;
        double pixel_count = 0.0;
        final int height = orientation_field[0].length;

        final int[] histogram = new int[181];
        for (final int[] anOrientation_field : orientation_field) {
            for (int y = 0; y < height; y++) {
                if (anOrientation_field[y] > -1) {
                    histogram[180 - anOrientation_field[y]]++;
                    pixel_count += 1;
                }
            }
        }
        for (int phi = 0; phi < 181; phi++) {
            sin_sum += histogram[phi] * Math.sin(2.0 * phi * Const.RAD);
            cos_sum += histogram[phi] * Math.cos(2.0 * phi * Const.RAD);
        }
        double best_ori = (0.5 * Math.atan(sin_sum / cos_sum) / Const.RAD + 180) % 180;
        shape.setNewOrientation(Math.round(best_ori * Const.M));
        for (int phi = 0; phi < 181; phi++) {
            old_numerator += histogram[phi] * Math.cos(2.0 * (phi - shape.getOrientation() / Const.MF) * Const.RAD);
            new_numerator += histogram[phi] * Math.cos(2.0 * (phi - best_ori) * Const.RAD);
        }
        shape.setOrderParameter(Math.round(Const.MF * old_numerator / pixel_count));
        shape.setNewOrderParameter(Math.round(Const.MF * new_numerator / pixel_count));
        if (shape.getNewOrderParameter() < 0) {
            shape.setNewOrderParameter(shape.getNewOrderParameter() * -1);
            best_ori = (best_ori + 90) % 180;
        }
        shape.setNewOrientation(Math.round(best_ori * Const.M));
    }


    /**
     * Gets the Rectangle Bounds of a binary shape
     *
     * @param binaryImage input from which the bounds will be calculated
     * @return Rectangle bounds of the shape
     */
    private Rectangle getBounds(@NotNull IBinaryImage binaryImage) {
        Objects.requireNonNull(binaryImage);
        final int width = binaryImage.getWidth();
        final int height = binaryImage.getHeight();
        int xMin = Integer.MAX_VALUE, xMax = 0, yMin = Integer.MAX_VALUE, yMax = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (binaryImage.getPixel(i, j)) {
                    if (i < xMin) xMin = i;
                    if (i > xMax) xMax = i;
                    if (j < yMin) yMin = j;
                    if (j > yMax) yMax = j;
                }
            }
        }
        return new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin);
    }


    /**
     * Gets for each separate shape in the source image one IBinaryImage with the shape pixels set to true
     * if no shape is found, return an empty list
     *
     * @param image   input image, the input is duplicated in caller, no need to duplicate in implementation
     * @param mask    input mask, not necessary, restricts the output by the shape of mask
     * @param minArea defines the minimum area allowed (if below the shape should not be stored in the result)
     * @return List<IBinaryImage> the result should never be null
     */
    protected abstract @NotNull
    List<IBinaryImage> getShapes(@NotNull ImageProcessor image, @Nullable IBinaryImage mask, final int minArea);

    /**
     * Gets for each separate shape in the source image one IBinaryImage with the shape pixels set to true
     * if no shape is found, return an empty list
     *
     * @param image   input image, the input is duplicated in caller, no need to duplicate in implementation
     * @param mask    input mask, not necessary, restricts the output by the shape of mask
     * @param minArea defines the minimum area allowed (if below the shape should not be stored in the result)
     * @return List<IBinaryImage> the result should never be null
     */
    protected abstract @NotNull
    List<IBinaryImage> getExtendedShapes(@NotNull ImageProcessor image, @Nullable IBinaryImage mask, final int minArea);


    /*public static final ShapeContainer getCellData(@NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, Class<? extends CellPlugin> plugin) throws IllegalAccessException, InstantiationException {
        CellPlugin pluginInstance = plugin.newInstance();
        return pluginInstance.getCellData(image, mask, minRange, minArea, pluginInstance.isMemoryState());
    }*/


    /**
     * Initialises only the Extended Shape and the Container
     *
     * @param image    image from which the area should be calculated
     * @param mask     mask that restricts the area (can be null)
     * @param minRange min gray-scale range
     * @param minArea  minimum area allowed
     * @param plugin   cell plugin which is handles area calculation
     * @param <T>      type of CellPlugin implementation
     * @return ShapeContainer which contains the extended area of the cell
     */
    public static final <T extends CellPlugin> ShapeContainer getCellDataExt(@NotNull ImageProcessor image, @Nullable ShapeContainer mask, final int minRange, final int minArea, @NotNull T plugin) {
        return plugin.getCellData(image, mask, minRange, minArea, plugin.isMemoryState());
    }


    /**
     * Gets the Cell-Data from an ImageProcessor via an object of <T extends CellPlugin>
     * Calls the whole procedure (getCellData&updateCellData) -> creates the extShape and the shape
     *
     * @param image    image from which the area should be calculated
     * @param mask     mask that restricts the area (can be null)
     * @param minRange min gray-scale range
     * @param minArea  minimum area allowed
     * @param plugin   cell plugin which is handles area calculation
     * @param <T>      type of CellPlugin implementation
     * @return ShapeContainer which contains the ext-area and the area of the cell
     */
    public static final <T extends CellPlugin> ShapeContainer getCellData(@NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, @NotNull T plugin) {
        Objects.requireNonNull(image, "CellPlugin::getCellData --- image is null");
        Objects.requireNonNull(plugin, "CellPlugin.getCellData --- plugin is null");
        ShapeContainer container = plugin.getCellData(image, mask, minRange, minArea, plugin.isMemoryState());
        plugin.updateCellData(container, image, mask, minRange, minArea, plugin.isMemoryState());
        return container;
    }

    /**
     * Updates the cell shape for {container}
     *
     * @param image    image from which the area should be calculated
     * @param mask     mask that restricts the area (can be null)
     * @param minRange min gray-scale range
     * @param minArea  minimum area allowed
     * @param plugin   cell plugin which is handles area calculation
     * @param <T>      type of CellPlugin implementation
     */
    public static final <T extends CellPlugin> void updateCellData(@NotNull ShapeContainer container, @NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, @NotNull T plugin) {
        Objects.requireNonNull(image, "CellPlugin::updateCellData --- image is null");
        Objects.requireNonNull(plugin, "CellPlugin.updateCellData --- plugin is null");
        Objects.requireNonNull(container, "CellPlugin.updateCellData --- container is null");
        plugin.updateCellData(container, image, mask, minRange, minArea, plugin.isMemoryState());
    }

    /*
    public static final void updateCellData(@NotNull ShapeContainer container, @NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, Class<? extends CellPlugin> plugin) throws IllegalAccessException, InstantiationException {
        CellPlugin pluginInstance = plugin.newInstance();
        pluginInstance.updateCellData(container, image, mask, minRange, minArea,true);
    }*/


    /**
     * Method for calculating the areas of an image, this method is used to split the extended area which should be used in filters and the exact area
     * which should be used as cell area representation
     *
     * @param container
     * @param image
     * @param mask
     * @param minRange
     * @param minArea
     */
    public final void updateCellData(@NotNull ShapeContainer container, @NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, boolean memoryState) {
        final double origRange = (image.getMax() - image.getMin());
        if (origRange < minRange) {
            return;
        }
        IBinaryImage aggregatedMask = null;
        if (mask != null) {
            aggregatedMask = mask.getAggregatedArea();
        }
        if (aggregatedMask != null && aggregatedMask.isInMemoryState()) {
            aggregatedMask = aggregatedMask.clone();
            aggregatedMask.exitMemoryState();
        }
        //debug section
        if (container == null) {
            throw new RuntimeException("CellPlugin::updateCellData() --- Container == null");
        }
        if (container.getAreas() == null)
            throw new RuntimeException("CellPlugin::updateCellData() --- container.getAreas() == null");


        container.getAreas().clear();
        //duplicate image to prevent changes in input
        image = image.duplicate();


        //applying this filter creates problems with getting the correct shapes (it increases the amount of black pixels drastically)
        //FilterAreaMask filterAreaMask = new FilterAreaMask();
        //filterAreaMask.run(image, container.getAggregatedExtArea());


        Objects.requireNonNull(getShapes(image, aggregatedMask, minArea)).forEach(binaryImage -> {
            CellShape result = new CellShape(binaryImage);
            initialize(binaryImage, result, true);
            result.setBounds(getBounds(binaryImage));
            if (binaryImage instanceof BinaryImage && memoryState) {
                binaryImage.setTrueValue(0);//imageJ foreground value, since enterMemoryState uses imageJ
                binaryImage.enterMemoryState();
            }
            container.getAreas().add(result);
        });
        container.getAreas().sort(Comparator.comparingInt(CellShape::getArea));
        //System.out.println("CellPlugin::updateCellData() --- areas.size()="+container.getAreas().size());

        Collections.reverse(container.getAreas());//desc order sorting (largest area first)
    }


    public final ShapeContainer getCellData(@NotNull ImageProcessor image, @Nullable ShapeContainer mask, int minRange, final int minArea, boolean memoryState) {
        final double origRange = (image.getMax() - image.getMin());
        if (origRange < minRange) {
            return new ShapeContainer();
        }
        IBinaryImage aggregatedExtArea = null;
        if (mask != null) {
            aggregatedExtArea = mask.getAggregatedExtArea();
        }
        if (aggregatedExtArea != null && aggregatedExtArea.isInMemoryState()) {
            aggregatedExtArea = aggregatedExtArea.clone();
            aggregatedExtArea.exitMemoryState();
        }
        image = image.duplicate();//duplicate image to prevent changes in input

        ShapeContainer shapes = new ShapeContainer();
        Objects.requireNonNull(getExtendedShapes(image, aggregatedExtArea, minArea)).forEach(binaryImage -> {
            CellShape result = new CellShape(binaryImage);
            initialize(binaryImage, result, true);
            result.setBounds(getBounds(binaryImage));
            if (binaryImage instanceof BinaryImage && memoryState) {
                binaryImage.setTrueValue(0);//imageJ foreground value, since enterMemoryState uses imageJ
                binaryImage.enterMemoryState();
            }
            shapes.getExtAreas().add(result);
        });


        shapes.getExtAreas().sort(Comparator.comparingInt(CellShape::getArea));
        Collections.reverse(shapes.getExtAreas());//desc order sorting (largest area first)

        return shapes;
    }


    /**
     * Changes the input Image!
     *
     * @param image
     * @param binMethod
     * @return
     */
    public static IBinaryImage getCellImage(ImageProcessor image, AutoThresholder.Method binMethod) {
        AutoThresholder autoThresholder = new AutoThresholder();
        return getCellImage(image, autoThresholder.getThreshold(binMethod, image.getHistogram()));
    }

    /**
     * Changes the input Image!
     *
     * @param image
     * @param threshold
     * @return
     */
    public static IBinaryImage getCellImage(ImageProcessor image, int threshold) {
        image.threshold(threshold);
        if (!Prefs.blackBackground) {
            image.invert();
        }
        return new BinaryImage((ByteProcessor) image);
    }


}
