package util;

import core.Const;
import core.filaments.AbstractFilament;
import filters.FilterCanvasResizer;
import filters.FilterInvert;
import focaladhesion.FocalAdhesionContainer;
import focaladhesion.FocalAdhesionProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.*;
import core.cell.DataFilaments;
import core.FilterQueue;
import core.cell.CellShape;
import core.cell.ShapeContainer;
import tracking.area.AreaTracker;
import tracking.area.CellEvent;
import tracking.area.DynamicArea;
import tracking.filament.DataTracking;
import tracking.filament.DynamicFilament;
import core.image.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;


/**
 * Should contain all image export methods and contains methods for painting things on image (like filaments, area, ...)
 */
public class ImageExporter {


    public static BufferedImage copy(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics2D g = b.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    /**
     * Returns a Buffered RGB image of the input file
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static BufferedImage getRGB(File file) throws IOException {
        //BufferedImage image = ImageIO.read(file);//since this should support tif, imageIO can't be used here
        ImagePlus tmp = IJ.openImage(file.getAbsolutePath());
        BufferedImage image = tmp.getBufferedImage();
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics2d = output.createGraphics();
        graphics2d.drawImage(image, null, 0, 0);
        graphics2d.dispose();
        tmp.close();
        return output;
    }

    /**
     * Returns a Buffered RGB image of min size dimension (image is copied mid)
     *
     * @param file
     * @param dimension
     * @return
     */
    public static BufferedImage getRGB(File file, Dimension2D dimension) {
        ImagePlus tmp = IJ.openImage(file.getAbsolutePath());
        BufferedImage image = tmp.getBufferedImage();

        int nWidth = (int) (dimension.getWidth() > image.getWidth() ? dimension.getWidth() : image.getWidth());
        int nHeight = (int) (dimension.getHeight() > image.getHeight() ? dimension.getHeight() : image.getHeight());

        int xOff = (int) ((dimension.getWidth() - image.getWidth()) / 2);
        int yOff = (int) ((dimension.getHeight() - image.getHeight()) / 2);

        BufferedImage output = new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics2d = output.createGraphics();
        //graphics2d.setColor(Color.black);//or transparent? / no fill rect
        //graphics2d.fillRect(0,0,nWidth,nHeight);
        graphics2d.drawImage(image, null, Math.max(xOff, 0), Math.max(yOff, 0));
        graphics2d.dispose();
        tmp.close();
        return output;


    }


    public static void exportImage(ImagePlus imagePlus, File location) {
        FileSaver fileSaver = new FileSaver(imagePlus);
        if (imagePlus.isStack()) fileSaver.saveAsTiffStack(location.getAbsolutePath());
        else
            fileSaver.saveAsPng(location.getAbsolutePath());
    }

    public static void exportImage(ImageProcessor imageProcessor, File location) throws IOException {
        BufferedImage image = imageProcessor.getBufferedImage();
        exportImage(image, location);
    }

    public static void exportImage(BufferedImage image, File location) throws IOException {
        ImageIO.write(image, "png", location);
    }

    public static void exportImage(BufferedImage image, String identifier, File location, String name) throws IOException {
        String tmp = name;
        for (int i = 0; i < PathScanner.supportedImageExtensions.size(); i++) {
            tmp = tmp.replace(PathScanner.supportedImageExtensions.get(i), "");
        }
        final File img_file = new File(location.getAbsolutePath() + File.separator +
                tmp.replaceAll("\\.", "_") + (identifier != null ? "_" + identifier : "") + ".png");

        exportImage(image, img_file);
    }


    public static void exportImage(IBinaryImage image, File location) throws IOException {
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("ImageExporter::Image not properly initialized");
        }
        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        final int width = image.getWidth(), height = image.getHeight();
        final int blackRGB = Color.black.getRGB(), whiteRGB = Color.white.getRGB();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (!image.getPixel(i, j)) img.setRGB(i, j, blackRGB);
                else img.setRGB(i, j, whiteRGB);
            }
        }
        ImageIO.write(img, "png", location);
    }

    public static void exportImage(boolean[][] image, String file) {
        if (image.length <= 0 || image[0].length <= 0) {
            System.out.println("Length <= 0:" + image.length + " x " + image[0].length + ";" + file);
            return;
        }
        BufferedImage img = new BufferedImage(image.length, image[0].length, BufferedImage.TYPE_BYTE_BINARY);
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (!image[i][j]) img.setRGB(i, j, Color.black.getRGB());
                else img.setRGB(i, j, Color.white.getRGB());
            }

        }
        try {
            ImageIO.write(img, "png", new File(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportImage(int[][] image, String file, int imgType) {
        if (imgType != BufferedImage.TYPE_BYTE_GRAY) imgType = BufferedImage.TYPE_BYTE_BINARY;
        if (image.length <= 0 || image[0].length <= 0) {
            System.out.println("Length <= 0:" + image.length + " x " + image[0].length + ";" + file);
            return;
        }
        BufferedImage img = new BufferedImage(image.length, image[0].length, imgType);
        WritableRaster raster = img.getRaster();
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                raster.setSample(i, j, 0, (image[i][j] < 0 ? 0 : (image[i][j] > 255 ? 255 : image[i][j])));
            }
        }
        try {
            ImageIO.write(img, "png", new File(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage addArea(BufferedImage image, ShapeContainer container) {
        //paint all shapes on image(for each? or just use aggregated image?)
        if (container.getAggregatedArea() != null) image = addArea(image, container.getAggregatedArea(), Color.blue);
        if (container.getAggregatedExtArea() != null)
            image = addArea(image, container.getAggregatedExtArea(), Color.yellow);

        if (container.getAreas() != null) {
            Graphics2D graphics2D = image.createGraphics();
            graphics2D.setColor(Color.green);
            container.getAreas().forEach(cellShape -> {
                Rectangle2D rect = cellShape.getBounds();
                graphics2D.drawRect((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
            });
            graphics2D.dispose();
        }


        return image;
    }


    public static BufferedImage addArea(BufferedImage image, CellShape cellShape, Color colorArea) {
        if (cellShape == null) return image;

        IBinaryImage binImage = cellShape.getBinaryImage();


        int bfType = (colorArea.equals(Color.white) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), bfType);
        Graphics2D graphics2d = output.createGraphics();
        graphics2d.drawImage(image, null, 0, 0);


        if (binImage != null) {
            System.out.println("ImageExporter::addArea() --- isInMemoryState=" + ((BinaryImage) binImage).isInMemoryState());
            graphics2d.setColor(colorArea);
            addOutlines(graphics2d, binImage);
        }

        graphics2d.dispose();
        return output;
    }


    private static void addOutlines(Graphics2D graphics2d, IBinaryImage binaryImage) {

        if (!(binaryImage).isInMemoryState()) {
            binaryImage = binaryImage.clone();
            (binaryImage).enterMemoryState();
        }
        //if (binaryImage instanceof BinaryImage) ((BinaryImage) binaryImage).clearEdges();//clear those edges
        for (int x = 0; x < binaryImage.getWidth(); x++) {
            for (int y = 0; y < binaryImage.getHeight(); y++) {
                if (binaryImage.getPixel(x, y))
                    graphics2d.drawLine(x, y, x, y);//0 is "true" (the outline is drawn with 0)
            }
        }
    }


    public static BufferedImage addArea(BufferedImage image, IBinaryImage binaryImage, Color colorArea) {
        if (binaryImage == null) return image;
        int bfType = (colorArea.equals(Color.white) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), bfType);
        Graphics2D graphics2d = output.createGraphics();
        graphics2d.drawImage(image, null, 0, 0);
        graphics2d.setColor(colorArea);
        addOutlines(graphics2d, binaryImage);
        graphics2d.dispose();
        return output;
    }


    /**
     * @param base    base image the area is added on (image gets changed)
     * @param area    the area added to base image
     * @param color   color of the drawn pixels
     * @param details if details==true the outline of the area is drawn if details==false the bounding box is drawn
     */
    public static void addArea(BufferedImage base, CellShape area, Color color, boolean details) {
        Graphics2D graphics2d = base.createGraphics();
        graphics2d.setColor(color);
        if (details) {
            addOutlines(graphics2d, area.getBinaryImage());
        } else {
            graphics2d.drawRect(area.getBounds().x, area.getBounds().y, area.getBounds().width, area.getBounds().height);
        }
        graphics2d.dispose();
    }


    public static BufferedImage getBufferedImage(ImageProcessor imageProcessor) {

        BufferedImage output = new BufferedImage(imageProcessor.getWidth(), imageProcessor.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics2d = output.createGraphics();
        if (!imageProcessor.isBinary()) {
            graphics2d.drawImage(imageProcessor.getBufferedImage(), null, 0, 0);
        } else {
            //assumption: default imageJ settings (255==white==background)
            int[][] arr = imageProcessor.getIntArray();
            final int width = imageProcessor.getWidth(), height = imageProcessor.getHeight();
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (arr[i][j] == 255) {//that's white(background) in imageJ(default settings)
                        //we want white for foreground and black for background
                        output.setRGB(i, j, Color.black.getRGB());
                    } else {
                        output.setRGB(i, j, Color.white.getRGB());
                    }
                }
            }
        }
        graphics2d.dispose();
        return output;
    }

    /**
     * @param base          has to be a RGB Image
     * @param filaments
     * @param filamentColor
     * @return
     */
    public static void addFilaments(BufferedImage base, DataFilaments filaments, Color filamentColor) {
        addFilaments(base, filaments.getFilaments(), filamentColor);
    }


    public static void addFilaments(BufferedImage base, List<AbstractFilament> filaments, Color filamentColor) {
        addFilaments(base, filaments, filamentColor, true);
    }

    public static void addFilaments(BufferedImage base, List<AbstractFilament> filaments, Color filamentColor, boolean variableLineWidth) {
        Graphics2D graphics2d = base.createGraphics();
        graphics2d.setColor(filamentColor);
        for (AbstractFilament singleFilament : filaments) {
            if (singleFilament.isKeep()) {

                if (variableLineWidth)
                    graphics2d.setStroke(new BasicStroke((float) (singleFilament.getWidth() / Const.MF),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                else
                    graphics2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                if (singleFilament.isVerified()) {
                    graphics2d.setColor(Color.magenta);
                } else {
                    graphics2d.setColor(filamentColor);
                }
                List<Point> points = singleFilament.getPoints();
                for (int i = 0; i < points.size() - 1; i++) {
                    graphics2d.drawLine(points.get(i).x, points.get(i).y,
                            points.get(i + 1).x, points.get(i + 1).y);
                }
            }
        }
        graphics2d.dispose();
    }


    public static void addFocalAdhesions(BufferedImage baseImage, ImageProcessor fAProcessor, Color color, final int highValue) {
        //overlay the threshed focal adhesions in color green
        int[][] focalAdhesions = fAProcessor.getIntArray();
        final int width = baseImage.getWidth();
        final int height = baseImage.getHeight();
        final int green = color.getRGB();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (focalAdhesions[i][j] == highValue) baseImage.setRGB(i, j, green);
            }
        }
    }

    public static void addFocalAdhesions(BufferedImage baseImage, Color color, Entry entry) {
        CorrelationData cd = entry.getCorrelationData();
        if (cd instanceof FocalAdhesionContainer) {
            FocalAdhesionContainer fac = (FocalAdhesionContainer) cd;
            if (fac.getData() != null) {
                Graphics2D graphics = baseImage.createGraphics();
                graphics.setColor(color);
                fac.getData().forEach(focalAdhesion -> {

                    graphics.drawLine(
                            (int) focalAdhesion.getMainAxisStart().getX(),
                            (int) focalAdhesion.getMainAxisStart().getY(),
                            (int) focalAdhesion.getMainAxisEnd().getX(),
                            (int) focalAdhesion.getMainAxisEnd().getY()
                    );

                    graphics.drawLine(
                            (int) focalAdhesion.getSideAxisStart().getX(),
                            (int) focalAdhesion.getSideAxisStart().getY(),
                            (int) focalAdhesion.getSideAxisEnd().getX(),
                            (int) focalAdhesion.getSideAxisEnd().getY()
                    );

                    AffineTransform t = new AffineTransform();
                    t.translate(focalAdhesion.getCenter().getX(), focalAdhesion.getCenter().getY());
                    t.rotate(focalAdhesion.getOrientation());

                    graphics.transform(t);
                    graphics.drawOval((int) (0 - focalAdhesion.getLengthMainAxis() / 2),
                            (int) (0 - focalAdhesion.getLengthSideAxis() / 2),
                            (int) (focalAdhesion.getLengthMainAxis()),
                            (int) (focalAdhesion.getLengthSideAxis())
                    );
                    try {
                        graphics.transform(t.createInverse());
                    } catch (NoninvertibleTransformException e) {
                        //...
                    }
                });
                graphics.dispose();
            }
        }
    }


    /**
     * Adds the Area stored in ImageWrapper{wrapper} to the image
     * input is changed
     *
     * @param wrapper
     */
    public static void addArea(ImageWrapper wrapper) {
        wrapper = ImageConversionUtils.convertToRGB(wrapper, false);//convert to rgb (if its not already rgb it gets converted)
        addArea(wrapper, Color.blue, Color.yellow, Color.green);
    }


    /**
     * Adds the Area stored in ImageWrapper{wrapper} to the image
     * input is changed
     *
     * @param wrapper
     */
    public static void addArea(ImageWrapper wrapper, Color colorArea, Color extArea, Color boundingBox) {
        wrapper = ImageConversionUtils.convertToRGB(wrapper, false);//convert to rgb (if its not already rgb it gets converted)
        MixedUtils.getStream(wrapper.getEntryList(), false).forEach(entry -> {
            int[][] arr = entry.getProcessor().getIntArray();

            IBinaryImage img = entry.getShape().getAggregatedArea();
            IBinaryImage imgExt = entry.getShape().getAggregatedExtArea();
            for (int x = 0; x < entry.getProcessor().getWidth(); x++) {
                for (int y = 0; y < entry.getProcessor().getHeight(); y++) {
                    if (img != null && img.getPixel(x, y) && colorArea != null) {
                        arr[x][y] = colorArea.getRGB();
                    }
                    if (imgExt != null && imgExt.getPixel(x, y) && extArea != null) {
                        arr[x][y] = extArea.getRGB();
                    }
                }
            }
            entry.getProcessor().setIntArray(arr);
            if (boundingBox != null) {
                entry.getProcessor().setColor(boundingBox);
                entry.getShape().getAreas().forEach(area ->
                        entry.getProcessor().drawRect(
                                (int) area.getBounds().getX(),
                                (int) area.getBounds().getY(),
                                (int) area.getBounds().getWidth(),
                                (int) area.getBounds().getHeight()
                        )
                );
            }
        });
    }


    /**
     * Adds Filament's which are stored in ImageWrapper to the image contained by ImageWrapper
     * Input is changed
     *
     * @param wrapper           onto which the filaments will be drawn
     * @param filamentColor     color for unverified filaments
     * @param verifiedColor     color for verified filaments
     * @param variableLineWidth true: line width depending on filament-width; false: line width statically 1 pixel
     */
    public static void addFilaments(ImageWrapper wrapper, Color filamentColor, Color verifiedColor, boolean variableLineWidth) {
        ImageWrapper wrapperContent = ImageConversionUtils.convertToRGB(wrapper, false);
        MixedUtils.getStream(wrapperContent.getEntryList(), false).forEach(entry -> {
            List<AbstractFilament> filaments = entry.getDataFilament().getFilteredFilaments(wrapperContent.getParameters());
            for (AbstractFilament singleFilament : filaments) {
                if (singleFilament.isKeep()) {
                    if (singleFilament.isVerified()) {
                        entry.getProcessor().setColor(verifiedColor);
                    } else {
                        entry.getProcessor().setColor(filamentColor);
                    }
                    if (variableLineWidth)
                        entry.getProcessor().setLineWidth((int) (singleFilament.getWidth() / Const.MF));
                    else entry.getProcessor().setLineWidth(1);
                    List<Point> points = singleFilament.getPoints();
                    for (int i = 0; i < points.size() - 1; i++) {
                        entry.getProcessor().drawLine(points.get(i).x, points.get(i).y,
                                points.get(i + 1).x, points.get(i + 1).y);
                    }
                }
            }
        });
    }


    public static void printColorCodedFAImageLegend(ImageProcessor ip, Color cFiberVerifiedByOne,
                                                    Color cFiberVerifiedByGreaterOne,
                                                    Color cNonVerifiedFibers,
                                                    Color cUnusedFA,
                                                    Color cSingleVerificationFA,
                                                    Color cMultiVerificationFA, int height) {

        //#region draw legend
        int xLegend = 20;
        ip.setFont(ip.getFont().deriveFont(24f));

        ip.setColor(cFiberVerifiedByOne);
        ip.fillRect(xLegend, height, 15, 15);//coordinates start top-left
        ip.drawString("Fiber verified by one FA", xLegend + 20, height + 20);//coordinates start bottom-left

        ip.setColor(cFiberVerifiedByGreaterOne);
        ip.fillRect(xLegend, height + 30, 15, 15);
        ip.drawString("Fiber verified by more than one FA", xLegend + 20, height + 20 + 30); //25);

        ip.setColor(cNonVerifiedFibers);
        ip.fillRect(xLegend, height + 60, 15, 15);
        ip.drawString("Fiber not verified", xLegend + 20, height + 20 + 60);


        ip.setColor(cUnusedFA);
        ip.fillRect(xLegend, height + 90, 15, 15);
        ip.drawString("Unused FA", xLegend + 20, height + 20 + 90);

        ip.setColor(cSingleVerificationFA);
        ip.fillRect(xLegend, height + 120, 15, 15);
        ip.drawString("FA verifies one fiber", xLegend + 20, height + 20 + 120);

        ip.setColor(cMultiVerificationFA);
        ip.fillRect(xLegend, height + 150, 15, 15);
        ip.drawString("FA verifies multiple fibers", xLegend + 20, height + 20 + 150);


        //#endregion
    }


    /**
     * Does not change input Wrapper (clones it)
     *
     * @param wrapper           should contain FA- and Filament-Data
     * @param variableLineWidth
     * @param displayNumber
     * @return
     */
    public static ImageWrapper getColorCodedImage(ImageWrapper wrapper, boolean variableLineWidth, boolean displayNumber) {
        ImageWrapper wrapperContent = ImageConversionUtils.convertToRGB(wrapper, true);

        final int heightOffset = 180;
        final int height = wrapperContent.getHeight();
        //clone input, redo validation and verify all (one end, more than one end etc.)

        FilterQueue filterQueue = new FilterQueue();
        FilterCanvasResizer resizer = new FilterCanvasResizer(wrapperContent.getWidth(), wrapperContent.getHeight() + heightOffset);
        filterQueue.add(resizer);
        filterQueue.run(wrapperContent, (f) -> {
        });
        wrapperContent.updateProcessors();


        List<Color> colorList = MixedUtils.getColorsForColorCoding();
        // color code fibers verified by 1 FA, by >1 FA's, non used FA's, non verified Fibers etc.
        Color cFiberVerifiedByOne = colorList.get(0),//Color.magenta,
                cFiberVerifiedByGreaterOne = colorList.get(1),
                cNonVerifiedFibers = colorList.get(2),
                cUnusedFA = colorList.get(5),
                cSingleVerificationFA = colorList.get(3),
                cMultiVerificationFA = colorList.get(4);


        MixedUtils.getStream(wrapperContent.getEntryList(), false).forEach(entry -> {
            ImageProcessor ip = entry.getProcessor();
            //#region draw legend
            printColorCodedFAImageLegend(ip, cFiberVerifiedByOne, cFiberVerifiedByGreaterOne, cNonVerifiedFibers, cUnusedFA, cSingleVerificationFA, cMultiVerificationFA, height);
            //#endregion
            List<AbstractFilament> filaments = entry.getDataFilament().getFilteredFilaments(wrapper.getParameters());
            for (AbstractFilament singleFilament : filaments) {
                if (singleFilament.isKeep()) {
                    if (singleFilament.getVerifier() != null) {
                        if (singleFilament.getVerifier().getId().size() == 1)
                            ip.setColor(cFiberVerifiedByOne);
                        else if (singleFilament.getVerifier().getId().size() > 1)
                            ip.setColor(cFiberVerifiedByGreaterOne);
                    } else {
                        ip.setColor(cNonVerifiedFibers);
                    }
                    if (variableLineWidth)
                        ip.setLineWidth((int) (singleFilament.getWidth() / Const.MF));
                    else ip.setLineWidth(1);
                    List<Point> points = singleFilament.getPoints();
                    for (int i = 0; i < points.size() - 1; i++) {
                        ip.drawLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
                    }
                }
            }


            CorrelationData cd = entry.getCorrelationData();
            if (cd instanceof FocalAdhesionContainer) {
                FocalAdhesionContainer fac = (FocalAdhesionContainer) cd;
                if (fac.getData() != null) {
                    ip.setLineWidth(2);//thicken lines
                    fac.getFilteredData(entry.getDataFilament(), wrapper.getParameters()).forEach(focalAdhesion -> {
                        long verifiyingFAs = entry.getDataFilament().getFilaments().stream().
                                filter(fil -> (fil.getVerifier() != null)).
                                flatMap(fil -> fil.getVerifier().getId().stream()).filter(i -> focalAdhesion.getNumber() == i).
                                count();

                        if (verifiyingFAs > 1) {
                            ip.setColor(cMultiVerificationFA);
                        } else if (verifiyingFAs == 1) {
                            ip.setColor(cSingleVerificationFA);
                        } else {
                            ip.setColor(cUnusedFA);
                        }

                        List<Point2D> hull = focalAdhesion.getConvexHull();
                        for (int i = 0; i < hull.size(); i++) {
                            Point2D curr = hull.get(i);
                            if (i < hull.size() - 1) {
                                Point2D next = hull.get(i + 1);
                                ip.drawLine((int) curr.getX(), (int) curr.getY(), (int) next.getX(), (int) next.getY());
                            } else {
                                Point2D first = hull.get(0);
                                ip.drawLine((int) curr.getX(), (int) curr.getY(), (int) first.getX(), (int) first.getY());
                            }
                        }
                        if (displayNumber) {
                            int x = (int) focalAdhesion.getCenter().getX(), y = (int) focalAdhesion.getCenter().getY();
                            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
                            double orientation = Math.toDegrees(focalAdhesion.getOrientation());
                            if ((orientation > 45 && orientation < 45 + 90) || (orientation > 45 + 180 && orientation < 45 + 90 + 180)) {
                                //very vertical orientation -> the drawn number should be shifted to left or right
                                if (x < 30) x += 20;
                                else x -= 20;
                            } else {
                                //very horizontal orientation -> the drawn number should be shifted up or down
                                if (y < 30) y += 20;
                                else y -= 10;
                            }
                            ip.setColor(Color.WHITE);//set a color for string it should be easily readable
                            ip.setFont(font);
                            ip.drawString(Integer.toString(focalAdhesion.getNumber()), x, y);
                        }
                    });
                }
            }


        });


        return wrapperContent;
    }


    //building the method like this with graphics as parameter it could be done with svggraphics2d
    public static void drawFilamentTrackingOverview(Graphics2D graphics2D, List<DynamicFilament> filamentList, int maxTime, int width, int height, int sizeOffset) {
        //this is the max height
        int numberOfFilaments = filamentList.size();
        int lineLength = (int) Math.floor(((double) width) / maxTime);
        int lineWidth = (int) Math.floor(((double) height) / numberOfFilaments);
        System.out.println("maxTime=" + maxTime + ",lineLength=" + lineLength + ",lineWidth=" + lineWidth);

        if (lineLength < 20) lineLength = 20;
        final int ll = lineLength;
        if (lineWidth < 2) lineWidth = 2;
        if (lineWidth > 15) lineWidth = 15;
        final int lw = lineWidth;

        List<Color> colors = MixedUtils.getColors();
        final int xOffset = 40;
        final int yOffset = 10;
        graphics2D.setStroke(new BasicStroke(lineWidth));


        for (int i = 0; i < numberOfFilaments; i++) {
            DynamicFilament filament = filamentList.get(i);
            Map<Integer, AbstractFilament> map = filament.getFilaments();
            final int n = i;//we take y because we do not count the dropped filaments in the output image
            graphics2D.setColor(colors.get(i % colors.size()));
            map.keySet().forEach(x -> graphics2D.drawLine((x * ll) + xOffset, (n * lw) + yOffset, ((x + 1) * ll) + xOffset, (n * lw) + yOffset));
        }

        graphics2D.setStroke(new BasicStroke(2));
        graphics2D.setColor(Color.black);
        for (int i = 0; i <= maxTime; i++) {
            int x = (i * lineLength) + xOffset;
            int xMod = 0;
            if (i == maxTime) xMod = -10;
            graphics2D.drawLine(x, yOffset, x, height + 10 + yOffset);
            graphics2D.drawString(Integer.toString(i), x + xMod, (height + sizeOffset / 2) + yOffset);
        }
        graphics2D.drawString("Timeline [time]", (width + sizeOffset) / 2, (int) (height + (2 * ((double) sizeOffset) + ((double) sizeOffset / 6)) / 3) + yOffset);

        // get a reference of the affine transform of the original coordinate system
        AffineTransform defaultAt = graphics2D.getTransform();
        // make new affine transform for rotation
        AffineTransform at = new AffineTransform();
        at.rotate(-Math.PI / 2);
        graphics2D.setTransform(at);
        // draw on the new coordinate system
        graphics2D.drawString("Filament-Life-Lines", ((height + sizeOffset) / 2) * -1, 30);
        // restore the original coordinate system
        graphics2D.setTransform(defaultAt);
    }

    /**
     * Create an image with life lines of Filaments tracked by SingleFilament-Tracking
     *
     * @param tracking
     * @param chkMinLength
     * @param minLength
     * @return image
     */
    public static BufferedImage getFilamentTrackingOverview(DataTracking tracking, boolean chkMinLength, int minLength, boolean chkMaxLength, int maxLength) {
        final int lineHeight = 2;
        final int columnWidth = 30;
        final int sizeOffset = 60;

        List<DynamicFilament> filamentList = tracking.filterTrackedFilaments(chkMinLength, minLength, chkMaxLength, maxLength);
        int numberOfFilaments = filamentList.size();

        int imageHeight = lineHeight * numberOfFilaments;
        if (imageHeight < 300) imageHeight = 300;
        int imageWidth = columnWidth * tracking.getMaxTime();
        if (imageWidth < 100) imageWidth = 100;

        BufferedImage image = new BufferedImage(imageWidth + sizeOffset, imageHeight + sizeOffset, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(1f, 1f, 1f, 0.02f));
        graphics.fillRect(0, 0, imageWidth + sizeOffset, imageHeight + sizeOffset);
        drawFilamentTrackingOverview(graphics, filamentList, tracking.getMaxTime(), imageWidth, imageHeight, sizeOffset);
        graphics.dispose();

        return image;
    }


    //building the method like this with graphics as parameter it could be done with svggraphics2d
    public static void drawAreaTrackingOverview(Graphics2D graphics2D, List<DynamicArea> areaList, int maxTime, int width, int height, int sizeOffset) {
        //this is the max height
        int numberOfAreas = areaList.size();
        int lineLength = (int) Math.floor(((double) width) / maxTime);
        int lineWidth = (int) Math.floor(((double) height) / numberOfAreas);
        //System.out.println("maxTime=" + maxTime + ",lineLength=" + lineLength + ",lineWidth=" + lineWidth);


        if (lineLength < 20) lineLength = 20;
        final int ll = lineLength;
        if (lineWidth < 2) lineWidth = 2;
        if (lineWidth > 15) lineWidth = 15;
        final int lw = lineWidth;

        List<Color> colors = MixedUtils.getColors();
        final int xOffset = 40;
        final int yOffset = 10;
        graphics2D.setStroke(new BasicStroke(lineWidth));

        for (int i = 0; i < numberOfAreas; i++) {
            DynamicArea filament = areaList.get(i);
            Map<Integer, CellEvent> map = filament.getAreas();
            final int n = i;//we take y because we do not count the dropped filaments in the output image
            graphics2D.setColor(colors.get(i % colors.size()));
            map.keySet().forEach(x -> graphics2D.drawLine((x * ll) + xOffset, (n * lw) + yOffset, ((x + 1) * ll) + xOffset, (n * lw) + yOffset));
        }

        graphics2D.setStroke(new BasicStroke(2));
        graphics2D.setColor(Color.black);
        for (int i = 0; i <= maxTime; i++) {
            int x = (i * lineLength) + xOffset;
            int xMod = 0;
            if (i == maxTime) xMod = -10;
            graphics2D.drawLine(x, yOffset, x, height + 10 + yOffset);
            graphics2D.drawString(Integer.toString(i), x + xMod, (height + sizeOffset / 2) + yOffset);
        }
        graphics2D.drawString("Timeline [time]", (width + sizeOffset) / 2, (int) (height + (2 * ((double) sizeOffset) + ((double) sizeOffset / 6)) / 3) + yOffset);

        // get a reference of the affine transform of the original coordinate system
        AffineTransform defaultAt = graphics2D.getTransform();
        // make new affine transform for rotation
        AffineTransform at = new AffineTransform();
        at.rotate(-Math.PI / 2);
        graphics2D.setTransform(at);
        // draw on the new coordinate system
        graphics2D.drawString("Cell-Life-Lines", ((height + sizeOffset) / 2) * -1, 30);
        // restore the original coordinate system
        graphics2D.setTransform(defaultAt);
    }

    /**
     * Create an image with life lines of Filaments tracked by SingleFilament-Tracking
     *
     * @param tracking
     * @param chkMinLength
     * @param minLength
     * @return image
     */
    public static BufferedImage getAreaTrackingOverview(AreaTracker tracking, boolean chkMinLength, int minLength, boolean chkMaxLength, int maxLength) {
        final int lineHeight = 2;
        final int columnWidth = 30;
        final int sizeOffset = 60;

        List<DynamicArea> areaList = tracking.filterUniqueAreas(chkMinLength, minLength, chkMaxLength, maxLength);
        int numberOfAreas = areaList.size();

        int maxTime = tracking.getUniqueAreas().stream().flatMap(d -> d.getAreas().keySet().stream()).mapToInt(i -> i).max().orElse(-1);


        int imageHeight = lineHeight * numberOfAreas;
        if (imageHeight < 300) imageHeight = 300;
        int imageWidth = columnWidth * maxTime;//here we need the amount of timeslots
        if (imageWidth < 100) imageWidth = 100;

        BufferedImage image = new BufferedImage(imageWidth + sizeOffset, imageHeight + sizeOffset, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(1f, 1f, 1f, 0.02f));
        graphics.fillRect(0, 0, imageWidth + sizeOffset, imageHeight + sizeOffset);
        drawAreaTrackingOverview(graphics, areaList, maxTime, imageWidth, imageHeight, sizeOffset);
        graphics.dispose();

        return image;
    }


    /**
     * Returns an ImageWrapper with one DynamicFilament drawn to it.
     *
     * @param orig              original ImageWrapper with source images
     * @param dynamicFilament   DynamicFilament which should be drawn
     * @param filamentColor     color of the filaments
     * @param verifiedColor     color in case the filament was verified
     * @param variableLineWidth if the line width is fixed or depends on filament width
     * @return
     */
    public static ImageWrapper addFilaments(ImageWrapper orig, DynamicFilament dynamicFilament, Color filamentColor, Color verifiedColor, boolean variableLineWidth) {
        ImageWrapper clone = ImageConversionUtils.convertToRGB(orig, true);

        MixedUtils.getStream(new ArrayList<>(dynamicFilament.getFilaments().entrySet()), false).forEach(entry -> {
            int time = entry.getKey();
            ImageProcessor processor = clone.getEntryList().get(time).getProcessor();
            AbstractFilament filament = entry.getValue();

            if (filament.isKeep()) {
                if (filament.isVerified()) {
                    processor.setColor(verifiedColor);
                } else {
                    processor.setColor(filamentColor);
                }
                if (variableLineWidth)
                    processor.setLineWidth((int) (filament.getWidth() / Const.MF));
                else processor.setLineWidth(1);
                List<Point> points = filament.getPoints();
                for (int i = 0; i < points.size() - 1; i++) {
                    processor.drawLine(points.get(i).x, points.get(i).y,
                            points.get(i + 1).x, points.get(i + 1).y);
                }
            }
        });
        return clone;
    }


    /**
     * Adds FocalAdhesion to the image of the wrapper
     * Currently only Convex Hull is drawn
     *
     * @param wrapper        input Wrapper which has to contain correlation data. the input is changed!
     * @param colorFAEllipse color for FA-Ellipse Shape
     * @param colorFAHull    color for FA-Convex-Hull Shape
     * @param displayNumber  if true the FA Numbers are drawn onto the image
     */
    public static void addFocalAdhesions(ImageWrapper wrapper, Color colorFAEllipse, Color colorFAHull, boolean displayNumber, @Annotations.Nullable Color colorNumber) {
        final ImageWrapper wrapperContent = ImageConversionUtils.convertToRGB(wrapper, false);
        MixedUtils.getStream(wrapperContent.getEntryList(), false).forEach(entry -> {
            CorrelationData cd = entry.getCorrelationData();
            if (cd instanceof FocalAdhesionContainer) {
                FocalAdhesionContainer fac = (FocalAdhesionContainer) cd;
                if (fac.getData() != null) {
                    entry.getProcessor().setLineWidth(2);//thicken lines
                    fac.getFilteredData(entry.getDataFilament(), wrapperContent.getParameters()).forEach(focalAdhesion -> {
                        //entry.getProcessor().setColor(colorFAEllipse);
                        //for now just paint the hull
                        /*EllipseFitter f=new EllipseFitter();
                        f.theta = focalAdhesion.getOrientation();//should be stored in RAD so no conversion needed
                        //the starting point from theta is PI/2 different than the starting point from the angle calculation of FocalAdhesion
                        //changing theta by a fixed amount does not solve the issue, some of the adhesion's are then off more or less
                        f.major = focalAdhesion.getLengthMainAxis();
                        f.minor = focalAdhesion.getLengthSideAxis();
                        f.xCenter = focalAdhesion.getCenter().getX();
                        f.yCenter = focalAdhesion.getCenter().getY();
                        f.drawEllipse(entry.getProcessor());*/
                        entry.getProcessor().setColor(colorFAEllipse);
                        List<Point2D> hull = focalAdhesion.getConvexHull();
                        for (int i = 0; i < hull.size(); i++) {
                            Point2D curr = hull.get(i);
                            if (i < hull.size() - 1) {
                                Point2D next = hull.get(i + 1);
                                entry.getProcessor().drawLine((int) curr.getX(), (int) curr.getY(), (int) next.getX(), (int) next.getY());
                            } else {
                                Point2D first = hull.get(0);
                                entry.getProcessor().drawLine((int) curr.getX(), (int) curr.getY(), (int) first.getX(), (int) first.getY());
                            }
                        }
                        if (displayNumber) {
                            int x = (int) focalAdhesion.getCenter().getX(), y = (int) focalAdhesion.getCenter().getY();
                            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
                            double orientation = Math.toDegrees(focalAdhesion.getOrientation());
                            if ((orientation > 45 && orientation < 45 + 90) || (orientation > 45 + 180 && orientation < 45 + 90 + 180)) {
                                //very vertical orientation -> the drawn number should be shifted to left or right
                                if (x < 30) x += 20;
                                else x -= 20;
                            } else {
                                //very horizontal orientation -> the drawn number should be shifted up or down
                                if (y < 30) y += 20;
                                else y -= 10;
                            }
                            entry.getProcessor().setColor(colorNumber);//set a color for string it should be easily readable
                            entry.getProcessor().setFont(font);
                            entry.getProcessor().drawString(Integer.toString(focalAdhesion.getNumber()), x, y);
                        }
                    });
                }
            }
        });
    }


    /**
     * This method is for changing the input image for being used as overlay
     * It works on a binary image but the BufferedImage has to be declared as Type ARGB
     *
     * @param source
     * @param foreground
     * @return
     */
    public static Image getImageForOverlay(BufferedImage source, Color foreground) {

        final int r1 = foreground.getRed();
        final int g1 = foreground.getGreen();
        final int b1 = foreground.getBlue();

        final int rgbA = foreground.getRGB();

        ImageFilter filter = new RGBImageFilter() {
            public final int filterRGB(int x, int y, int rgb) {
                int r = (rgb & 0xFF0000) >> 16;
                int g = (rgb & 0xFF00) >> 8;
                int b = rgb & 0xFF;
                if (r == 0 && g == 0 && b == 0) {
                    // Set fully transparent but keep color
                    return rgb & 0xFFFFFF;
                } else if (r == 255 && g == 255 && b == 255) {
                    return rgbA;
                }
                return rgb;
            }
        };

        ImageProducer ip = new FilteredImageSource(source.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }


    public static void ommitNonFocalAdhesion(ImageWrapper mask, FocalAdhesionProcessor.FAVerificationMethod mode) {

        mask.updateProcessors();
        mask.getEntryList().forEach(e -> {//make every image black
            e.getProcessor().setColor(Color.BLACK);
            e.getProcessor().fillRect(0, 0, mask.getWidth(), mask.getHeight());
        });
        MixedUtils.getStream(mask.getEntryList(), false).forEach(entry -> {
            CorrelationData cd = entry.getCorrelationData();
            if (cd instanceof FocalAdhesionContainer) {
                FocalAdhesionContainer fac = (FocalAdhesionContainer) cd;
                if (fac.getData() != null) {
                    entry.getProcessor().setLineWidth(2);//thicken lines
                    fac.getData().forEach(focalAdhesion -> {
                        entry.getProcessor().setColor(Color.white);
                        if (mode.equals(FocalAdhesionProcessor.FAVerificationMethod.ellipse)) {

                            Polygon polygon = PointUtils.getPointsOnEllipse(focalAdhesion.getCenter(), focalAdhesion.getLengthMainAxis(), focalAdhesion.getLengthSideAxis(), focalAdhesion.getOrientation());
                            entry.getProcessor().fillPolygon(polygon);

                            //since there is a gap between ellipse halfs fill it with a line
                            double x = focalAdhesion.getCenter().getX();
                            double y = focalAdhesion.getCenter().getY() - focalAdhesion.getLengthSideAxis() / 2;
                            double y2 = focalAdhesion.getCenter().getY() + focalAdhesion.getLengthSideAxis() / 2;
                            Point2D p1 = PointUtils.getPointRotated(new Point2D.Double(x, y), focalAdhesion.getCenter(), focalAdhesion.getOrientation());
                            Point2D p2 = PointUtils.getPointRotated(new Point2D.Double(x, y2), focalAdhesion.getCenter(), focalAdhesion.getOrientation());
                            entry.getProcessor().setLineWidth(2);
                            entry.getProcessor().drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

                        } else if (mode.equals(FocalAdhesionProcessor.FAVerificationMethod.convexHull)) {

                            List<Point2D> hull = focalAdhesion.getConvexHull();
                            Polygon polygon = new Polygon();
                            polygon.npoints = hull.size();
                            polygon.xpoints = hull.stream().mapToInt(p -> (int) p.getX()).toArray();
                            polygon.ypoints = hull.stream().mapToInt(p -> (int) p.getY()).toArray();
                            entry.getProcessor().fillPolygon(polygon);
                        } else if (mode.equals(FocalAdhesionProcessor.FAVerificationMethod.pixel)) {
                            IBinaryImage bin = focalAdhesion.getPixelArea();
                            for (int i = 0; i < entry.getProcessor().getWidth(); i++) {
                                for (int j = 0; j < entry.getProcessor().getHeight(); j++) {
                                    if (bin.getPixel(i, j)) entry.getProcessor().drawPixel(i, j);
                                }
                            }
                        }

                    });
                }
            }
        });

          /*if (mode.equals(FocalAdhesionProcessor.FAVerificationMethod.convexHull)) {
            addFocalAdhesions(mask, Color.white, Color.white, false);
        } else {

        }*/

        FilterQueue queue = new FilterQueue();
        queue.add(new FilterInvert());
        queue.run(mask, (f) -> {
        });
    }

    /**
     * Creates a ImageWrapper with black and white colors, black background and filaments are drawn in white onto it
     *
     * @param filaments
     * @return
     */
    public static ImageWrapper getFilamentWrapperAsMask(ImageWrapper filaments) {
        ImageWrapper mask = filaments.clone();
        mask.updateProcessors();
        mask.getEntryList().forEach(e -> {//make every image black
            e.getProcessor().setColor(Color.BLACK);
            e.getProcessor().fillRect(0, 0, mask.getWidth(), mask.getHeight());
        });

        addFilaments(mask, Color.white, Color.white, false);
        return mask;
    }


}



