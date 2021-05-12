package core;

import core.image.Entry;
import util.Annotations.NotNull;
import util.IOUtils;
import util.ImageExporter;
import util.MixedUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class for wrapping the OrientationField Illustration functionality
 * makes use of the BatchProcessor Class
 */
public class OFIllustratorBatch {


    /**
     * @param imageDirectory directory where the output will be stored
     * @param full
     * @throws Exception
     */
    public void projectIllustrations(@NotNull ProjectData projectData, @NotNull File imageDirectory, boolean full) throws Exception {
        Objects.requireNonNull(imageDirectory, "imageDirectory is null");
        if (!imageDirectory.exists()) throw new IllegalArgumentException("imageDirectory does not exist");

        List<File> imageFiles = projectData.getImageFiles();
        List<File> xmlFiles = projectData.getXmlFiles();

        //get the image files with non existing xml files - either process them now or throw exception?
        List<File> files = imageFiles.stream().filter(f -> !IOUtils.getOutFileFromImageFile(f, xmlFiles.get(0).getParentFile(), ".xml", null).exists()).collect(Collectors.toList());
        if (files.size() > 0) {
            //throw exception for now
            throw new IllegalArgumentException("projectIllustrations - not all images have processed xml data");
        }

        //check xml files for every image there should be an xml file with contents (serialized Entry)
        //@note the serialized entry does not contain the processor, which means, it would be wise to introduce
        //@note a mechanism in ImageWrapper to load such contents
        //@note probably it would be good to introduce a mechanism to load only a part of the entries at once
        //@note which means, its better to load single images in this case
        //@note and iterate over the file list instead of loading the whole stack

        //the processing should be build like this: parallel{sequential(load, process, store, unload)}
        List<Exception> exceptionsThrown = new CopyOnWriteArrayList<>();//thread-safe list
        BatchProcessor processor = new BatchProcessor();
        processor.batchProcessParallel(projectData, (wrapper, logger) -> {
            //pre process would be loading the corresponding data-xml
            try {
                wrapper.loadEntries(xmlFiles);
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                if (logger != null) logger.log(Level.SEVERE, sw.toString());
                exceptionsThrown.add(e);
            }
        }, (entry, logger) -> {
            //main processing is calling the makeIllustrations
            try {
                makeIllustrations(imageDirectory, new File(entry.getPath()).getName(), entry, full);
                entry.releaseResources();
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                if (logger != null) logger.log(Level.SEVERE, sw.toString());
                exceptionsThrown.add(e);
            }
        }, null);

        if (exceptionsThrown.size() > 0) {
            throw new Exception("projectIllustration exception/s happened, please check the log file for more information");
        }
    }

    /**
     * @param path
     * @param fileName
     * @param entry    Entry with original image
     * @param full
     * @return
     * @throws IOException
     */
    private void makeIllustrations(File path, String fileName, Entry entry, boolean full) throws Exception {
        BufferedImage main = ImageExporter.copy(ImageExporter.getBufferedImage(entry.getProcessor()));

        BufferedImage other = null;
        if (!full) other = ImageExporter.copy(ImageExporter.getBufferedImage(entry.getProcessor()));

        boolean hasOther = false;
        boolean hasMain = false;

        if (full) {
            List<Color> colorList = MixedUtils.getColors();
            hasMain = true;

            entry.getOrientationFieldContainer().getOrientationFieldIds().forEach(id ->
                    ImageExporter.addFilaments(main,
                            entry.getDataFilament().getFilaments().stream().
                                    filter(f -> f.getOrientationField() == id).collect(Collectors.toList()),
                            colorList.get((id < 0 || id > colorList.size()) ? 0 : id))
            );//stream filter by this id's foreach etc.


        } else {

            for (Integer key : entry.getOrientationFieldContainer().getOrientationFieldIds()) {
                if (key <= 0) {
                    hasOther = true;
                    ImageExporter.addFilaments(other, entry.getDataFilament().getFilaments().stream().
                            filter(f -> f.getOrientationField() == key).collect(Collectors.toList()), Color.cyan);
                } else if (key == 1) {
                    hasMain = true;
                    ImageExporter.addFilaments(main, entry.getDataFilament().getFilaments().stream().
                            filter(f -> f.getOrientationField() == key).collect(Collectors.toList()), Color.orange);
                } else {
                    hasOther = true;
                    ImageExporter.addFilaments(other, entry.getDataFilament().getFilaments().stream().
                            filter(f -> f.getOrientationField() == key).collect(Collectors.toList()), Color.orange);
                }
            }
        }
        if (hasMain) ImageExporter.exportImage(main, "main", path, fileName);
        if (hasOther) ImageExporter.exportImage(other, "other", path, fileName);
    }


}
