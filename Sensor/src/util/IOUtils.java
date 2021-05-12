package util;


import core.settings.*;
import core.tracers.CurveTracer;
import core.tracers.LineSensor;

import util.Annotations.NotNull;
import util.Annotations.Nullable;
import core.Calc;
import core.filaments.AbstractFilament;
import focaladhesion.FocalAdhesionContainer;
import core.cell.CellShape;
import core.image.CorrelationData;
import core.image.Entry;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Should contain all Helper-Methods for FileIO
 */
public class IOUtils {


    public static void writeFile(File file, String content) throws IOException {
        // "[A-Za-z0-9]{3,4}$" should allow for 3 or 4 character file
        // name extensions.
        BufferedWriter bwr = new BufferedWriter(new FileWriter(file, false));
        bwr.write(content);
        bwr.flush();
        bwr.close();
    }


    /**
     * @param image
     * @return
     */
    public static String imageToString(int[][] image) {
        String out = Arrays.deepToString(image);
        out = out.replaceAll("\\s+", "").replace("],[", System.lineSeparator()).replace("[", "").replace("]", "");
        return out;
    }


    public static void exportVerifierTable(File path, Entry entry, Settings dp) throws IOException {
        String ls = System.lineSeparator();
        StringBuilder builder = new StringBuilder("");
        CorrelationData data = entry.getCorrelationData();
        if (data == null) return;
        if (!(data instanceof FocalAdhesionContainer)) return;

        builder.append("#FocalAdhesion Verifier-Table CSV").append(ls);
        builder.append("#number_filament,number_focal_adhesion,...").append(ls);
        entry.getDataFilament().getFilteredFilaments(dp).stream().filter(AbstractFilament::isVerified).forEach(filament -> {
            builder.append(filament.getNumber());
            if (filament.getVerifier() != null)
                builder.append(",").append(filament.getVerifier().getId().stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")));
            builder.append(ls);
        });
        File file = getOutFileFromImageFile(new File(entry.getPath()), path, ".csv", "verifier_table");
        writeFile(file, builder.toString());
    }


    /**
     * Get the Output-File from Image-File(changes file name and directory)
     *
     * @param imageFile       input image file
     * @param outputDirectory directory where the output should be
     * @param outExt          file extension of target (has to include the .) for example ".xml"
     * @return
     */
    public static File getOutFileFromImageFile(@NotNull File imageFile, @NotNull File outputDirectory, @NotNull String outExt, @Nullable String identifier) {
        Objects.requireNonNull(imageFile, "ImageFile is Null");
        Objects.requireNonNull(outputDirectory, "outputDirectory is Null");
        Objects.requireNonNull(outExt, "ext is Null");

        String nName = imageFile.getName();
        for (String ext : PathScanner.supportedImageExtensions) {
            nName = nName.replace(ext, "");
        }
        nName = nName.replaceAll("\\.", "_");
        return new File(outputDirectory.getAbsolutePath() + File.separator + nName + ((identifier != null) ? "_" + identifier : "") + outExt);
    }


    /**
     * @param toSerialize
     * @param location
     * @param imageFile
     * @param <T>
     * @throws FileNotFoundException
     * maybe compatibility issues?
     */
    public static <T extends Serializable> void writeXML(T toSerialize, File location, File imageFile) throws FileNotFoundException {
        writeXML(toSerialize, location, imageFile, null);
    }

    /**
     * @param toSerialize
     * @param location
     * @param imageFile
     * @param <T>
     * @throws FileNotFoundException
     * maybe compatibility issues?
     */
    public static <T extends Serializable> void writeXML(T toSerialize, File location, File imageFile, String identifier) throws FileNotFoundException {
        //System.out.println("IOUtils::writeXML --- T="+toSerialize);
        final File img_file = getOutFileFromImageFile(imageFile, location, ".xml", identifier);
        //System.out.println("IOUtils::writeXML --- after getFile="+img_file);
        XMLEncoder encoder = new XMLEncoder(new FileOutputStream(img_file));
        //System.out.println("IOUtils::writeXML --- after new Encoder");
        encoder.writeObject(toSerialize);
        //System.out.println("IOUtils::writeXML --- after writeObject");
        encoder.flush();
        //System.out.println("IOUtils::writeXML --- after flush");
        encoder.close();

    }


    public static <T extends Serializable> T loadXML(@NotNull File xmlFile) throws FileNotFoundException {
        Objects.requireNonNull(xmlFile, "xmlFile is null");
        if (!xmlFile.exists()) throw new IllegalArgumentException("xmlFile does not exist");
        XMLDecoder decoder = new XMLDecoder(new FileInputStream(xmlFile));
        Object result = decoder.readObject();
        decoder.close();
        try {
            T obj = (T) result;
            return obj;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Type of XML-File does not match the required Class-Type");
        }
    }


    /**
     * Exports the list of filament chains as comma separated values.
     */
    public static void exportOF(int scale, Entry entry, File file) throws IOException {
        int[] x = new int[]{scale, entry.getProcessor().getWidth(), entry.getProcessor().getHeight()};
        String out = imageToString(Calc.upsize(entry.getOrientationFieldContainer().getOrientationField(), x[0], x[1], x[2]));
        writeFile(file, out);
    }


    public static Dimension2D getDimensionFromImage(File resourceFile) {
        try (ImageInputStream in = ImageIO.createImageInputStream(resourceFile)) {
            final Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                javax.imageio.ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }


    public static void exportFingerprint(Entry entry, CellShape shape, File path, String fileName, Settings dp) throws IOException {
        int[][] fingerprint = null;
        if (dp.getValueAsBoolean(Trace.curve)) {
            fingerprint = CurveTracer.makeFingerprint(shape.getLongHalfAxis() / 5e5, entry.getOrientationFieldContainer().getOrientationField());
        } else {
            fingerprint = LineSensor.makeFingerprint(shape.getLongHalfAxis() / 5e5, entry.getOrientationFieldContainer().getOrientationField());
        }
        File file = new File(path.getAbsolutePath() + File.separator + fileName.replaceAll("\\.", "_") + "_fingerprint.csv");
        IOUtils.writeFile(file, IOUtils.imageToString(fingerprint));
    }


}
