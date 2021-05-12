package util;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
 *               2013-2014 Benjamin Eltzner
 *
 * FilamentSensor is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * or see <http://www.gnu.org/licenses/>.
 */


import util.Annotations.NotNull;

import core.Misc.Int2D;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import core.image.Entry;
import core.image.IBinaryImage;
import core.image.ImageWrapper;
import core.tracers.Tracer;

import java.awt.*;
import java.awt.image.*;
import java.lang.reflect.Method;
import java.util.Objects;


public abstract class ImageFactory {

    public static BufferedImage makeTwoLevelRedGreenImage(int[][] int_image,
                                                          boolean[][] mask_in,
                                                          boolean[][] mask_out) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0 ||
                mask_in == null || mask_in.length != int_image.length || mask_in[0].length != int_image[0].length ||
                mask_out == null || mask_out.length != int_image.length || mask_out[0].length != int_image[0].length) {
            return null;
        }

        final int width = int_image.length,
                height = int_image[0].length;
        int[] out = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int tmp = 20 + (int) Math.round(235. * int_image[x][y] / 255.);
                out[width * y + x] = 0xFF000000 | (mask_out[x][y] ? tmp << (mask_in[x][y] ? 8 : 16) :
                        (tmp << 16 | tmp << 8 | tmp));
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    public static BufferedImage makeTwoLevelRedGreenImage(@NotNull ImageProcessor processor, @NotNull IBinaryImage mask_in, @NotNull IBinaryImage mask_out) {
        Objects.requireNonNull(processor);
        Objects.requireNonNull(mask_in);
        Objects.requireNonNull(mask_out);

        if (mask_in.isInMemoryState()) {
            mask_in = mask_in.clone();
            mask_in.exitMemoryState();
        }

        if (mask_out.isInMemoryState()) {
            mask_out = mask_out.clone();
            mask_out.exitMemoryState();
        }

        final int width = processor.getWidth(),
                height = processor.getHeight();
        int[] out = new int[width * height];
        int[][] int_image = processor.getIntArray();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int tmp = 20 + (int) Math.round(235. * int_image[x][y] / 255.);
                out[width * y + x] = 0xFF000000 | (mask_out.getPixel(x, y) ? tmp << (mask_in.getPixel(x, y) ? 8 : 16) :
                        (tmp << 16 | tmp << 8 | tmp));
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    /**
     * @param wrapper
     * @return
     * @todo doesn't work properly?
     */
    public static ImageWrapper makeTwoLevelRedGreenImage(ImageWrapper wrapper) {
        wrapper = ImageConversionUtils.convertToRGB(wrapper, true);

        wrapper.getEntryList().forEach(entry -> {
            ImageProcessor processor = entry.getProcessor();
            IBinaryImage mask_in = entry.getInteriorContainer().getInterior();
            IBinaryImage mask_out = entry.getShape().getSelectedArea().getBinaryImage();
            if (mask_in.isInMemoryState()) {
                mask_in = mask_in.clone();
                mask_in.exitMemoryState();
            }

            if (mask_out.isInMemoryState()) {
                mask_out = mask_out.clone();
                mask_out.exitMemoryState();
            }


            final int width = processor.getWidth(),
                    height = processor.getHeight();
            int[][] out = new int[width][height];
            int[][] int_image = processor.getIntArray();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int tmp = 20 + (int) Math.round(235. * int_image[x][y] / 255.);
                    out[x][y] = 0xFF000000 | (mask_out.getPixel(x, y) ? tmp << (mask_in.getPixel(x, y) ? 8 : 16) :
                            (tmp << 16 | tmp << 8 | tmp));
                }
            }
            processor.setIntArray(out);
        });
        return wrapper;
    }




    public static BufferedImage makeGreyImage(int[][] int_image, boolean filter) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            return null;
        }

        int width = int_image.length;
        int height = int_image[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();

        if (filter) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int pixel = int_image[i][j] + 128;
                    raster.setSample(i, j, 0, (pixel < 0 ? 0 :
                            (pixel > 255 ? 255 :
                                    pixel)));
                }
            }
            return image;
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                raster.setSample(i, j, 0, (int_image[i][j] < 0 ? 0 :
                        (int_image[i][j] > 255 ? 255 :
                                int_image[i][j])));
            }
        }
        return image;
    }

    public static BufferedImage makeDeepGreyImage(int[][] int_image) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            return null;
        }

        final int width = int_image.length,
                height = int_image[0].length,
                white = (1 << 16) - 1;
        int max = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (int_image[i][j] > max) {
                    max = int_image[i][j];
                }
            }
        }
        if (max < white) {
            max = white;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = image.getRaster();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                raster.setSample(i, j, 0, (long) int_image[i][j] * white / max);
            }
        }
        return image;
    }


    public static BufferedImage makeColorImage(int[][] int_image, int color_count, boolean scale) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            return null;
        }

        final int width = int_image.length,
                height = int_image[0].length,
                padding = 20;
        int[] out = new int[width * height],
                colors = makeColors(color_count);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                out[width * j + i] = (int_image[i][j] < 0 ? 0xFF000000 :
                        (int_image[i][j] >= color_count ? 0xFFFFFFFF :
                                colors[int_image[i][j]]));
            }
        }

        int end = padding, start = color_count + padding;
        if (scale) {
            for (int i = padding; i < 2 * padding; i++) {
                for (int j = start; j > end; j--) {
                    out[width * j + i] = colors[j - end - 1];
                }
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        if (scale) {
            Graphics2D canvas = image.createGraphics();
            String[] labels = new String[]{"0°", "90°", "180°"};
            start += (color_count + canvas.getFontMetrics().getHeight()) / 2;
            for (String l : labels) {
                canvas.drawString(l, 5 * padding / 2, start -= color_count / 2);
            }
            /* Right-aligned text
            start += (color_count + canvas.getFontMetrics().getHeight()) / 2;
            for(String l : labels){
                int label_width = canvas.getFontMetrics().stringWidth(l);
                int x = width - label_width - 5 * padding / 2;
                canvas.drawString(l, x, start -= color_count / 2);
            }*/
            canvas.dispose();
        }

        return image;
    }

    public static BufferedImage makeThreeColorImage(int[][] int_image) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            return null;
        }

        final int width = int_image.length,
                height = int_image[0].length;
        int[] out = new int[width * height],
                colors = new int[]{0xFF000000, 0xFF0000AA, 0xFFFF6600, 0xFFFF0000};

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                out[width * j + i] = colors[int_image[i][j]];
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    public static BufferedImage makeRedGreenImage(int[][] int_image, boolean[][] mask) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0 ||
                mask == null || mask.length != int_image.length || mask[0].length != int_image[0].length) {
            return null;
        }

        final int width = int_image.length,
                height = int_image[0].length;
        int[] out = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int tmp = 20 + (int) Math.round(235. * int_image[x][y] / 255.);
                out[width * y + x] = 0xFF000000 | tmp << (mask[x][y] ? 8 : 16);
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    public static BufferedImage makeWidthMap(Pair<Int2D, Integer> pair) {

        int[][] width_map = pair.getKey().the;
        final int color_count = pair.getValue() - 1,
                width = width_map.length,
                height = width_map[0].length,
                offset = 5 * (color_count + 1) / 3 + 1;
        int[] out = new int[width * height],
                colors = makeColors(color_count);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                out[width * j + i] = (width_map[i][j] == 0 ? 0xFF000000 :
                        (width_map[i][j] == 1 ||
                                width_map[i][j] > color_count + 1 ? 0xFFFFFFFF :
                                colors[((offset - width_map[i][j]) % color_count)]));
            }
        }

        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(out, width * height);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        return new BufferedImage(cm, raster, false, null);
    }

    public static BufferedImage greyWidthMap(Pair<Int2D, Integer> pair) {
        int[][] width_map = pair.getKey().the;
        final int color_count = pair.getValue(),
                width = width_map.length,
                height = width_map[0].length;
        int[] grey = makeGreyLevels(color_count);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                raster.setSample(i, j, 0, (width_map[i][j] == 0 ? 0 :
                        (width_map[i][j] > color_count ? 255 :
                                grey[width_map[i][j] - 1])));
            }
        }
        return image;
    }

    public static BufferedImage makeColorImage(int[][] int_image, boolean scale) {
        return makeColorImage(int_image, 180, scale);
    }

    public static BufferedImage makeCustomImage(int[] int_image, final int width) {
        if (int_image == null || int_image.length <= 0) {
            return null;
        }
        final int height = int_image.length / width;
        ColorModel cm = ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(int_image, int_image.length);
        WritableRaster raster = Raster.createWritableRaster(cm.createCompatibleSampleModel(width, height), buffer, null);
        return new BufferedImage(cm, raster, false, null);
    }

    public static int[] makeColors(int count) {
        int current;
        int[] rainbow = new int[count];
        double step = 1530.0 / count;

        for (int i = 0; i < count; i++) {
            current = (int) (i * step);

            int r = (current < 511 ? 255 :
                    (current < 766 ? 765 - current :
                            (current < 1021 ? 0 :
                                    (current < 1276 ? current - 1020 :
                                            255))));
            int g = (current < 511 ? current / 2 :
                    (current < 766 ? 255 :
                            (current < 1021 ? 1020 - current :
                                    0)));
            int b = (current < 766 ? 0 :
                    (current < 1021 ? current - 765 :
                            (current < 1276 ? 255 :
                                    1530 - current)));

            rainbow[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        return rainbow;
    }

    public static int[] makeGreyLevels(int count) {
        int[] grey = new int[count];
        double step = 200.0 / count;

        for (int i = 0; i < count; i++) {
            grey[i] = 55 + (int) ((i + 1) * step);
        }

        return grey;
    }

    public static BufferedImage getAreaImage(Entry entry) {
        IBinaryImage binaryImage = entry.getShape().getAreas().stream().map(a -> {
            IBinaryImage clone = a.getBinaryImage().clone();
            clone.exitMemoryState();
            return clone;
        }).reduce(
                (a, b) -> {
                    a.or(b);
                    return a;
                }).orElse(null);
        if (binaryImage != null)
            return binaryImage.getBufferedImage();
        return null;
    }

    public static ImageWrapper getAreaImage(ImageWrapper wrapper) {
        ImageWrapper mask = wrapper.clone();
        mask.updateProcessors();
        mask.getEntryList().forEach(e -> {//make every image black
            e.getProcessor().setColor(Color.BLACK);
            e.getProcessor().fillRect(0, 0, mask.getWidth(), mask.getHeight());
            e.getShape().getAreas().forEach(a -> a.getBinaryImage().exitMemoryState());
        });
        ImageExporter.addArea(mask, Color.white, null, null);

        return mask;
    }


/*
    public static BufferedImage blubb(int type,int[][] img){
        BufferedImage image;

        switch (type) {
            case DataImage.ORIG:
                return ImageFactory.makeGreyImage(img, false);
            case DataImage.PRE:
                return ImageFactory.makeGreyImage(img, false);
            case DataImage.DIFF:
                return ImageFactory.makeGreyImage(m_int_image_filter_difference, true);
            case DataImage.BIN:
                return ImageFactory.makeBinaryImage(m_binary_image, true);
            case DataImage.HOUGH:
                return ImageFactory.makeBinaryImage(m_binary_image_hough, false);
            case DataImage.EMPTY:
                return ImageFactory.makeGreyImage(new int[m_width][m_height], false);
            default:
                return null;
        }




        switch (type) {
            case ORI_MAP:
                image = ImageFactory.makeColorImage(tracer().getOrientationField(), false);
                break;
            case FINGERPRINT:
                double axis = cellData().getLongHalfAxis() / 5e5;
                image = ImageFactory.makeDeepGreyImage(tracer().makeFingerprint(axis));
                break;
            case RED_GREEN:
                image = imageProcessing().getRedGreenImage();
                break;
            default:
                return null;
        }

    }*/


    /*public static BufferedImage getElogImage(int type, FilterELoG filterELoG) {
        switch (type) {
            case DataImage.ELOG_COLOR:
                if (filterELoG == null) {
                    return null;
                }
                return filterELoG.imageColor();
            case DataImage.ELOG_GREY:
                if (filterELoG == null) {
                    return null;
                }
                return filterELoG.imageGrey();
            default:
                break;
        }
        return null;
    }*/


    public static BufferedImage getOrientationMap(int[][] orientationField) {
        return ImageFactory.makeColorImage(orientationField, false);
    }

    public static ImageWrapper getOrientationMap(ImageWrapper wrapper) {
        ImageStack stack = new ImageStack(wrapper.getWidth(), wrapper.getHeight());

        wrapper.getEntryList().stream().forEachOrdered(entry -> {
            ImageProcessor processor = new ColorProcessor(getOrientationMap(entry.getOrientationFieldContainer().getOrientationField()));
            stack.addSlice(entry.getPath(), processor);
        });
        return new ImageWrapper(new ImagePlus(wrapper.getImage().getTitle(), stack), wrapper.getParameters());
    }


    public static BufferedImage getFingerprint(Entry entry, Class<? extends Tracer> tracer) {
        double axis = entry.getShape().getAreas().get(0).getLongHalfAxis() / 5e5;//get from first area (should be the largest)
        try {
            Method method = tracer.getMethod("makeFingerprint", double.class, int[][].class);
            int[][] fingerprint = (int[][]) method.invoke(null, axis, entry.getOrientationFieldContainer().getOrientationField());
            return ImageFactory.makeDeepGreyImage(fingerprint);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ImageWrapper getFingerprint(ImageWrapper wrapper, Class<? extends Tracer> tracer) {
        ImageStack stack = new ImageStack(wrapper.getWidth(), wrapper.getHeight());

        wrapper.getEntryList().stream().forEachOrdered(entry -> {
            ImageProcessor processor = new ByteProcessor(getFingerprint(entry, tracer));
            stack.addSlice(entry.getPath(), processor);
        });
        return new ImageWrapper(new ImagePlus(wrapper.getImage().getTitle(), stack), wrapper.getParameters());
    }


}
