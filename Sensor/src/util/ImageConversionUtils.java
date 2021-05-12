package util;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import core.image.ImageWrapper;

import java.awt.image.IndexColorModel;

public class ImageConversionUtils {

    //region LUT Gray from ij.plugin.LutLoader
    private static int grays(byte[] reds, byte[] greens, byte[] blues) {
        for (int i = 0; i < 256; i++) {
            reds[i] = (byte) i;
            greens[i] = (byte) i;
            blues[i] = (byte) i;
        }
        return 256;
    }

    private static void interpolate(byte[] reds, byte[] greens, byte[] blues, int nColors) {
        byte[] r = new byte[nColors];
        byte[] g = new byte[nColors];
        byte[] b = new byte[nColors];
        System.arraycopy(reds, 0, r, 0, nColors);
        System.arraycopy(greens, 0, g, 0, nColors);
        System.arraycopy(blues, 0, b, 0, nColors);
        double scale = nColors / 256.0;
        int i1, i2;
        double fraction;
        for (int i = 0; i < 256; i++) {
            i1 = (int) (i * scale);
            i2 = i1 + 1;
            if (i2 == nColors) i2 = nColors - 1;
            fraction = i * scale - i1;
            //IJ.write(i+" "+i1+" "+i2+" "+fraction);
            reds[i] = (byte) ((1.0 - fraction) * (r[i1] & 255) + fraction * (r[i2] & 255));
            greens[i] = (byte) ((1.0 - fraction) * (g[i1] & 255) + fraction * (g[i2] & 255));
            blues[i] = (byte) ((1.0 - fraction) * (b[i1] & 255) + fraction * (b[i2] & 255));
        }
    }


    public static void grayLUT(ImagePlus imp) {
        FileInfo fi = new FileInfo();
        fi.reds = new byte[256];
        fi.greens = new byte[256];
        fi.blues = new byte[256];
        fi.lutSize = 256;
        int nColors = 0;
        nColors = grays(fi.reds, fi.greens, fi.blues);

        if (nColors > 0) {
            if (nColors < 256)
                interpolate(fi.reds, fi.greens, fi.blues, nColors);

            if (imp.getType() == ImagePlus.COLOR_RGB)
                IJ.error("LUTs cannot be assiged to RGB Images.");
            else if (imp.isComposite() && ((CompositeImage) imp).getMode() == IJ.GRAYSCALE) {
                CompositeImage cimp = (CompositeImage) imp;
                cimp.setMode(IJ.COLOR);
                int saveC = cimp.getChannel();
                IndexColorModel cm = new IndexColorModel(8, 256, fi.reds, fi.greens, fi.blues);
                for (int c = 1; c <= cimp.getNChannels(); c++) {
                    cimp.setC(c);
                    cimp.setChannelColorModel(cm);
                }
                imp.setC(saveC);
                imp.updateAndDraw();
            } else {
                ImageProcessor ip = imp.getChannelProcessor();
                IndexColorModel cm = new IndexColorModel(8, 256, fi.reds, fi.greens, fi.blues);
                if (imp.isComposite())
                    ((CompositeImage) imp).setChannelColorModel(cm);
                else
                    ip.setColorModel(cm);
                if (imp.getStackSize() > 1)
                    imp.getStack().setColorModel(cm);
                imp.updateAndDraw();
            }
        }
    }
//endregion

    /**
     * Converts the source ImageWrapper to RGB-Type (if not already RGB)
     *
     * @param source ImageWrapper which gets converted
     * @param copy   if set to true the source will not get modified, if set to false the source will get modified
     * @return returns a RGB ImageWrapper
     */
    public static ImageWrapper convertToRGB(ImageWrapper source, boolean copy) {
        //isColorLut is not a good condition (for example a one channel red image counts as colorLut but is handled like a grayscale)
        //if colors are painted on them they will be black
        //what different non RGB images types have in common (1 color channel), at ImageWrapper creation they will get
        //converted to 8bit greyscale, those pseudo-color images are staying "red", but they are handled correctly like a
        //real gray scale in the program, but for export they appear as colorLut
        //possible solution -> check for nChannels greyscale and pseudo-greyscale do have only 1 channel
        ImageWrapper tmp = source;
        if (copy) tmp = source.clone();

        //if a single channel "color" image which was binarized is converted the background color will take the main
        //color (for example if its a green channel image, everything will get green)
        //for that purpose apply grayLUT that should solve it
        grayLUT(tmp.getImage());

        if (!tmp.getImage().getProcessor().isColorLut() || tmp.getImage().getProcessor().getNChannels() < 3) {
            ImageConverter converter = new ImageConverter(tmp.getImage());
            converter.convertToRGB();
            tmp.updateProcessors();
        }
        return tmp;
    }


    public static ImageWrapper convertToGray(ImageWrapper source, boolean copy) {
        ImageWrapper tmp = source;
        if (copy) tmp = source.clone();
        grayLUT(tmp.getImage());

        ImageConverter converter = new ImageConverter(tmp.getImage());
        converter.convertToGray8();
        tmp.updateProcessors();
        return tmp;
    }
}
