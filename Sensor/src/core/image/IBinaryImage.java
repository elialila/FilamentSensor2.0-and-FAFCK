package core.image;

import ij.process.ByteProcessor;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;


public interface IBinaryImage extends Cloneable {


    int getWidth();

    void setWidth(int width);

    int getHeight();

    void setHeight(int height);


    void dilate(IBinaryImage mask);
    void erode(IBinaryImage mask);


    /**
     * Stores the image as png in the file given as parameter
     *
     * @param file
     */
    void store(File file);

    /**
     * Gets the Pixel on position x,y
     *
     * @param x
     * @param y
     * @return
     */
    boolean getPixel(int x, int y);

    /**
     * Sets the Pixel on position x,y to true
     *
     * @param x
     * @param y
     */
    void setPixel(int x, int y);

    /**
     * Resets the Pixel on position x,y to false
     *
     * @param x
     * @param y
     */
    void clearPixel(int x, int y);


    /**
     * Returns the number of Pixels set to true
     *
     * @return
     */
    int getPixelSetCount();


    /**
     * Does logical AND between this and img(which is an intersect in set therms)
     * requirement image size has to be the same
     *
     * @param img
     * @throws IllegalArgumentException if image sizes are different
     */
    void and(IBinaryImage img) throws IllegalArgumentException;

    /**
     * Does logical OR between this and img(which is an union in set therms)
     * requirement image size has to be the same
     *
     * @param img
     * @throws IllegalArgumentException
     */
    void or(IBinaryImage img) throws IllegalArgumentException;



    /**
     * Make a hard copy of the object
     *
     * @return
     */
    IBinaryImage clone();


    boolean[][] toBoolean();

    /**
     * Returns the byte value for true
     *
     * @return
     */
    int getTrueValue();

    /**
     * Sets the byte value for true
     *
     * @param trueValue new byte value for true
     */
    void setTrueValue(int trueValue);




    ByteProcessor getByteProcessor();

    BufferedImage getBufferedImage();

    /**
     * opens the BinaryImage for processing (prepares internal variables)
     */
    void open();

    /**
     * write changes to storage attribute
     */
    void flush();

    /**
     * cleans processing variables
     */
    void close();


    double compare(IBinaryImage image);


    boolean isInMemoryState();

    /**
     * Enters memory state, which means the data will be stored as outline to occupy less bits
     * Before EnterMemoryState trueValue has to be set to imageJ default(0), only matters at
     * creation time of the binary image
     */
    void enterMemoryState();

    /**
     * Exits the memory state, which means the outline data stored will be filled
     */
    void exitMemoryState();


    /**
     * SoftReferences (SoftReference'd ByteProcessor for processing)
     * processing like:
     * <p>
     * img.open();//initialise the softreferences (if they aren't set)
     * img.erode();//check for reference and then process
     * img.flush()//write changes from processor to BitMask
     * img.close()//clean softreference
     */

    List<Point2D> getPoints();

}
