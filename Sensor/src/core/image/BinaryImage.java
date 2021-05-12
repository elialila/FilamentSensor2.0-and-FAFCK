package core.image;


import core.Calc;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.filter.Binary;
import ij.process.ByteProcessor;
import util.ImageExporter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

/**
 * Wrapper class for binary image data-logic
 *
 */
public class BinaryImage implements IBinaryImage {

    private BitSet bits;
    private int width;
    private int height;

    private boolean inverted;

    /**
     * Represents what value a true will be, if converted to int[][] or ByteProcessor
     * <p>
     * this value should solve the many invert uses during processing
     */
    private int trueValue;


    //SoftReference on ByteProcessor which will be collected by GarbageCollector if there is need for memory
    //used for faster computation (enables imageJ methods) and memory saving because image data is stored in BitSet
    private SoftReference<ImagePlus> byteProcessor;
    private static final String title = "bin_image";
    //variable to disable changes to data, when there is an existing ByteProcessor (keep consistency of data)
    //private boolean opened=false;

    //#region constructors/initialisation
    private void init(int width, int height) {
        setWidth(width);
        setHeight(height);
        setBits(new BitSet(width * height));
        byteProcessor = null;
        inverted = false;
    }

    public BinaryImage(int width, int height) {
        init(width, height);
    }

    public BinaryImage(ByteProcessor processor) {
        this(processor.getWidth(), processor.getHeight());
        byteProcessor = new SoftReference<>(new ImagePlus(title, processor));
        flush();

    }

    public BinaryImage(Collection<Point2D> points, int width, int height) {
        this(width, height);
        points.forEach(p -> {
            if (p.getX() > 0 && p.getY() > 0)
                setPixel((int) p.getX(), (int) p.getY());
        });
    }

    public BinaryImage(boolean[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0)
            throw new IllegalArgumentException("given array is null or empty");
        init(array.length, array[0].length);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (array[x][y]) setPixel(x, y);
            }
        }
    }

    /**
     * Initialises BinaryImage with array values; if array[x][y] > threshold
     *
     * @param array
     * @param threshold
     */
    public BinaryImage(int[][] array, int threshold) {
        if (array == null || array.length == 0 || array[0].length == 0)
            throw new IllegalArgumentException("given array is null or empty");
        init(array.length, array[0].length);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (array[x][y] > threshold) setPixel(x, y);
            }
        }
    }

    public BinaryImage(File file) {
        this(IJ.openImage(file.getPath()).getProcessor().convertToByteProcessor());
    }
    //#endregion


    public BitSet getBits() {
        return bits;
    }

    public void setBits(BitSet bits) {
        this.bits = bits;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isInverted() {
        return inverted;
    }

    //private void setOpened(boolean opened){this.opened=opened;}
    //public boolean isOpened(){return opened;}


    /**
     * Slow version with bounds check
     *
     * @param x
     * @param y
     * @param image
     * @return
     */
    public static final int translate(int x, int y, IBinaryImage image) {
        if (image.getWidth() > x && image.getHeight() > y && x >= 0 && y >= 0) {
            return y * image.getWidth() + x;
        }
        return -1;
    }

    /**
     * Create a hard copy of the binary image data     *
     *
     * @return
     */
    private BitSet getWorkingCopy() {
        return (BitSet) getBits().clone();
    }


    @Override
    public void store(File file) {
        try {
            ImageExporter.exportImage(this, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean getPixel(int x, int y) {
        if (x < 0 || y < 0) throw new IllegalArgumentException("(x=" + x + ",y=" + y + ") out of Bounds");
        final int idx = translate(x, y, this);
        if (idx < 0)
            throw new IllegalArgumentException("(x=" + x + ",y=" + y + ",idx=" + idx + ") out of Bounds(w=" + getWidth() + ",h=" + getHeight() + ")");
        return getBits().get(idx);
    }

    public void setPixel(int x, int y) {
        getBits().set(translate(x, y, this));
    }

    public void clearPixel(int x, int y) {
        getBits().clear(translate(x, y, this));
    }

    @Override
    public int getPixelSetCount() {
        return getBits().cardinality();
    }

    @Override
    public void and(IBinaryImage img) throws IllegalArgumentException, IllegalStateException {
        //if(isOpened())throw new IllegalStateException("Image is opened, no modification allowed");
        if (img.getWidth() != getWidth() || img.getHeight() != getHeight())
            throw new IllegalArgumentException("Images are not of the same size:(" + getWidth() + "," + getHeight() + ")!=(" + img.getWidth() + "," + img.getHeight() + ")");
        if (img instanceof BinaryImage) {
            getBits().and(((BinaryImage) img).getBits());
        } else {
            throw new IllegalArgumentException("Images are not of the same type");
        }
    }


    //@todo implement padding on and/or


    @Override
    public void or(IBinaryImage img) throws IllegalArgumentException {
        //if(isOpened())throw new IllegalStateException("Image is opened, no modification allowed");
        if (img.getWidth() != getWidth() || img.getHeight() != getHeight())
            throw new IllegalArgumentException("Images are not of the same size:(" + getWidth() + "," + getHeight() + ")!=(" + img.getWidth() + "," + img.getHeight() + ")");
        if (img instanceof BinaryImage) {
            getBits().or(((BinaryImage) img).getBits());
        } else {
            throw new IllegalArgumentException("Images are not of the same type");
        }
    }

    @Override
    public BinaryImage clone() {
        BinaryImage clone = new BinaryImage(getWidth(), getHeight());
        flush();
        clone.setBits(getWorkingCopy());
        clone.setInMemoryState(isInMemoryState());
        clone.inverted = isInverted();
        clone.setTrueValue(getTrueValue());
        return clone;
    }

    @Override
    public boolean[][] toBoolean() {
        return toBoolean(false);
    }

    @Override
    public int getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(int trueValue) {
        this.trueValue = trueValue;
    }

    public boolean[][] toBoolean(boolean flip) {
        boolean[][] img = new boolean[getWidth()][getHeight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                img[x][y] = (flip) ? !getPixel(x, y) : getPixel(x, y);
            }
        }
        return img;
    }


    /*
    store area as outline
    things to keep in mind:
    on creation of binary image it has to be outlined -> keep track of the true value
    on open the ByteProcessor has to be filled -> no one keeps in mind that only the outline is stored
    on flush the filled ByteProcessor has to be outlined again
    on IBinaryImage Methods like dilate and erode the image has to be filled
    */

    private boolean inMemoryState;

    private void setInMemoryState(boolean inMemoryState) {
        this.inMemoryState = inMemoryState;
    }

    public boolean isInMemoryState() {
        return inMemoryState;
    }

    /**
     * Enters memory state, which means the data will be stored as outline to occupy less bits
     * Before EnterMemoryState trueValue has to be set to imageJ default(0), only matters at
     * creation time of the binary image
     */
    public void enterMemoryState() {
        if (inMemoryState) return;
        ByteProcessor byteProcessor = getByteProcessor();

        if (Prefs.blackBackground)
            byteProcessor.invert();//ByteProcessor doesn't care for blackBackground outline will be wrong, if not checked
        byteProcessor.outline();
        if (Prefs.blackBackground) byteProcessor.invert();
        flush();//flush data
        close();
        inMemoryState = true;
    }

    /**
     * Exits the memory state, which means the outline data stored will be filled
     */
    public void exitMemoryState() {
        if (!inMemoryState) return;
        Binary bin = new Binary();
        //test with flipped pixels
        ByteProcessor byteProcessor = getByteProcessor();
        int[][] arr = byteProcessor.getIntArray();
        bin.setup("fill", null);
        bin.run(getByteProcessor());
        flush();
        inMemoryState = false;
    }





    @Override
    public ByteProcessor getByteProcessor() {
        if (byteProcessor != null && byteProcessor.get() != null)
            return (ByteProcessor) byteProcessor.get().getProcessor();
        open();
        return (ByteProcessor) byteProcessor.get().getProcessor();
    }


    public BufferedImage getBufferedImage() {
        if (byteProcessor != null && byteProcessor.get() != null) return byteProcessor.get().getBufferedImage();
        BufferedImage output = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        final int width = getWidth(), height = getHeight();
        final int white = Color.white.getRGB();
        //System.out.println("BinaryImage::getBufferedImage() ---whiteRGB="+white);
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                if (getPixel(x, y)) {
                    output.setRGB(x, y, white);
                }
            }
        return output;
    }

    @Override
    /**
     * note that byteProcessor actions do not care about Prefs.blackBackground
     * only ij class Binary do
     */
    public void open() {
        if (byteProcessor != null && byteProcessor.get() != null) return;
        ByteProcessor byteProcessor = new ByteProcessor(width, height);

        int fg = Prefs.blackBackground ? 255 : 0;
        final int foreground = isInverted() ? 255 - fg : fg;
        final int background = 255 - foreground;

        byteProcessor.setBackgroundValue(background);


        //final int foreground = 0;//standard imageJ foreground color (black foreground, white background)
        //System.out.println("BinaryImage::open() --- foreground("+foreground+"),background("+(255-foreground)+")");
        int[][] arrVals = byteProcessor.getIntArray();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                arrVals[x][y] = (this.getPixel(x, y)) ? foreground : (255 - foreground);
                //imagej standard => white == background && black == foreground
            }
        }
        byteProcessor.setIntArray(arrVals);
        /*
        if (Prefs.blackBackground) {
            byteProcessor.invert();
        }
        if (isInverted()) byteProcessor.invert();
        */
        this.byteProcessor = new SoftReference<>(new ImagePlus(title, byteProcessor));
    }

    @Override
    public void flush() {
        if (byteProcessor == null || byteProcessor.get() == null) return;
        ByteProcessor bP = (ByteProcessor) byteProcessor.get().getProcessor();
        if (bP != null) {
            int[][] arr = bP.getIntArray();
            int foreground = 0;
            if (Prefs.blackBackground) {
                foreground = 255;
            }
            if (isInverted()) foreground = 255 - foreground;
            //System.out.println("BinaryImage::flush() --- high="+high+",trueValue="+getTrueValue());
            getBits().clear();
            for (int i = 0; i < getWidth(); i++) {
                for (int j = 0; j < getHeight(); j++) {
                    if (arr[i][j] == foreground) {
                        setPixel(i, j);
                    }
                }
            }
            //if larger than half of the area -> store the opposite
            if (getPixelSetCount() > (getWidth() * getHeight() / 2)) {
                invert();
            }
            //System.out.println("BinaryImage::flush() --- pixelCount/Area="+getPixelSetCount()+"/"+(getWidth()*getHeight()));
        }
    }

    @Override
    public void close() {
        if (byteProcessor == null) return;
        byteProcessor.clear();
    }

    @Override
    public double compare(IBinaryImage image) {
        if (image == null || image.getWidth() != this.getWidth() || image.getHeight() != this.getHeight()) {
            throw new IllegalArgumentException("BinaryImage::compare --- image parameter incorrect");
        }
        final int width = getWidth(), height = getHeight();
        long cnt = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (getPixel(x, y) == image.getPixel(x, y)) cnt++;
            }
        }

        return (double) cnt / (width * height);
    }


    /**
     * Dilates Image by structuring element mask
     * Calc.dilate is internally used
     *
     * @param mask
     */
    public void dilate(IBinaryImage mask) {
        flush();//update image data if there are changes made to cached ByteProcessor
        close();//close processing stuff (inconsistency danger)
        boolean[][] result = Calc.dilate(toBoolean(), mask.toBoolean());
        if (result == null) return;
        getBits().clear();
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (result[x][y]) this.setPixel(x, y);
            }
        }
    }

    /**
     * Erodes Image by structuring element mask
     * Calc.erode is internally used
     *
     * @param mask
     */
    public void erode(IBinaryImage mask) {
        flush();
        close();
        boolean[][] result = Calc.erode(toBoolean(), mask.toBoolean());
        if (result == null) return;
        getBits().clear();
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (result[x][y]) this.setPixel(x, y);
            }
        }
    }

    public void clearEdges() {
        //clear edges (strange behaviour on outline, it outlines the edges too)
        for (int x = 0; x < getWidth(); x++) {
            clearPixel(x, 0);
            clearPixel(x, getHeight() - 1);
        }
        for (int y = 0; y < getHeight(); y++) {
            clearPixel(0, y);
            clearPixel(getWidth() - 1, y);
        }
    }


    /**
     * Returns a list of Point2D representing each pixel set on the binary image
     * the list is sorted by lowest point.x first
     */
    public List<Point2D> getPoints() {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < getWidth(); i++)
            for (int j = 0; j < getHeight(); j++) {
                if (getPixel(i, j)) points.add(new Point(i, j));
            }
        //sort points by x Value -> lowest x first
        points.sort(Comparator.comparingDouble(Point2D::getX));
        return points;
    }


    public BinaryImage invert() {
        inverted ^= true;
        bits.flip(0, getWidth() * getHeight());
        trueValue = 255 - trueValue;
        return this;
    }

    /**
     * Enlargens the BinaryImage and copies it content to image center
     *
     * @param newWidth
     * @param newHeight
     * @return
     */
    public BinaryImage copyToCenter(int newWidth, int newHeight) {
        if (this.getWidth() > newWidth || this.getHeight() > newHeight) {
            throw new IllegalArgumentException("BinaryImage::copyToCenter --- newWidth||newHeight < oldWidth||oldHeight");
        }

        BinaryImage binaryImage = new BinaryImage(newWidth, newHeight);
        binaryImage.setTrueValue(getTrueValue());
        binaryImage.setInMemoryState(isInMemoryState());
        final int stepX = (newWidth - getWidth()) / 2;
        final int stepY = (newHeight - getHeight()) / 2;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (getPixel(x, y)) binaryImage.setPixel(x + stepX, y + stepY);
            }
        }
        return binaryImage;
    }

    //could be optimized with bitset get from to range
    public BinaryImage getSubImage(Rectangle rect) {
        return getSubImage(rect.x, rect.y, rect.width, rect.height);
    }

    public BinaryImage getSubImage(int x, int y, int w, int h) {
        BinaryImage sub = new BinaryImage(w, h);
        for (int i = x; i < x + w && i < getWidth(); i++) {
            for (int j = y; j < y + h && j < getHeight(); j++) {
                if (getPixel(i, j)) sub.setPixel(i - x, j - y);
            }
        }
        return sub;
    }


}
