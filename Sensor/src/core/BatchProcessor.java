package core;

import core.calculation.MeanBrightnessCalc;
import core.filaments.AbstractFilament;
import core.filaments.FilamentChain;
import tracking.filament.DataTracking;
import tracking.filament.DynamicFilament;
import util.*;
import util.Annotations.Nullable;
import ij.ImagePlus;
import ij.io.Opener;
import core.calculation.OFCalculator;
import core.cell.CellPlugin;
import core.cell.ShapeContainer;
import core.image.Entry;
import core.image.IBinaryImage;
import core.image.ImageWrapper;
import core.settings.*;
import core.tracers.CurveTracer;
import core.tracers.LineSensor;
import core.tracers.Tracer;
import util.io.FilamentCsvExport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 Wraps up functionality for batch processing
 */
public class BatchProcessor {
    private static boolean forceParallel = true;//this is used for getting a single var for both methods to force parallel computation

    private ExecutorService service;


    public BatchProcessor() {

        service = Executors.newCachedThreadPool();


    }

    private int calculateJunkSize() {
        //approximate acceptable size of imageWrapper, depending on available memory
        double available = MixedUtils.getApproxAvailableMemory();
        System.out.println("available:" + available);
        //get number of processors into the calculation (multiple of proc. count if ram is available)
        //the numbers are just based on rough testing, could be adjusted
        int tmpJunkSize = 10;
        if (available >= 7) tmpJunkSize = 60;
        else if (available >= 5) tmpJunkSize = 50;
        else if (available >= 4) tmpJunkSize = 30;
        else if (available < 3) tmpJunkSize = 4;
        //tmpJunkSize = 16;//this worked for testAll on 32GB Device max Usage was about ~6GB Memory
        return tmpJunkSize;
    }


    private ImageWrapper initBasic(int junkSize, List<File> files, ProjectData projectData, Dimension2D maxDimension, @Nullable Logger logger) throws Exception {
        if (logger != null) logger.info("initBasic(" + junkSize + "," + files.size() + "," + maxDimension + ")");
        List<File> junk;
        if (junkSize > files.size()) {
            junk = new ArrayList<>(files);
            files.clear();
        } else {
            junk = new ArrayList<>(files.subList(0, junkSize));
            files.removeAll(junk);
        }
        ImageWrapper wrapper = new ImageWrapper(junk, projectData.getSettings(), maxDimension);
        //do scaling before area computation
        int scale = projectData.getSettings().getValue(Pre.scale);//....
        if (scale != 1) {
            wrapper.scale(scale);
        }
        if (logger != null) logger.info("finished initBasic");
        return wrapper;
    }

    private Dimension2D getMaxDimension(List<File> files) {
        Dimension2D maxDimension = new Dimension(0, 0);
        //set max dimension
        List<Dimension2D> dimensions = MixedUtils.getStream(files, false).map(IOUtils::getDimensionFromImage).filter(Objects::nonNull).collect(Collectors.toList());
        dimensions.forEach(dim -> {
            if (dim.getWidth() > maxDimension.getWidth())
                maxDimension.setSize(dim.getWidth(), maxDimension.getHeight());
            if (dim.getHeight() > maxDimension.getHeight())
                maxDimension.setSize(maxDimension.getWidth(), dim.getHeight());
        });
        return maxDimension;
    }


    protected Logger getLogger(ProjectData projectData) {
        FileHandler fh;
        Logger logger = Logger.getLogger(getClass().getName());
        try {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
            String strDate = dateFormat.format(date);
            // This block configure the logger with handler and formatter
            fh = new FileHandler(projectData.getRootDir().getAbsolutePath() + File.separator + strDate + "_log.txt");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);


            FilamentSensor.setDebugStream(logger::info);
            FilamentSensor.setMessageStream(logger::info);
            FilamentSensor.setErrorStream(s -> logger.log(Level.SEVERE, s));


            return logger;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param projectData
     * @param entryConsumer This method should create a standardized way of batch-processing over files with different methods
     *                      It uses parallel computation of the entries
     *                      for example the normal batchprocessing or project illustrations etc.
     */
    public void batchProcessParallel(ProjectData projectData, @Nullable BiConsumer<ImageWrapper, Logger> preProcessing,
                                     @Nullable BiConsumer<Entry, Logger> entryConsumer,
                                     @Nullable BiConsumer<List<Entry>, Logger> postProcessing) throws Exception {
        //calculate batch-size
        int junkSize = calculateJunkSize();
        List<File> files = projectData.getImageFiles();
        Dimension2D maxDimension = getMaxDimension(files);


        Logger logger = getLogger(projectData);

        //no need for copying the list, because its already a new list object, see core.ProjectDataNew
        List<Entry> entryListAll = new ArrayList<>();
        do {//do-while loop
            //if restrict to area is active, the aggregated area from the last image from the previous loop run has to be given as parameter
            ImageWrapper wrapper = initBasic(junkSize, files, projectData, maxDimension, logger);
            //pre processing is here
            if (preProcessing != null) preProcessing.accept(wrapper, logger);
            System.out.println("BatchProcessor::batchProcess --- after preProcessing");
            //main processing
            if (entryConsumer != null)
                MixedUtils.getStream(wrapper.getEntryList(), forceParallel).forEach((entry) -> entryConsumer.accept(entry, logger));
            System.out.println("BatchProcessor::batchProcess --- after entryConsumer");
            if (postProcessing != null)
                entryListAll.addAll(wrapper.getEntryList());
            wrapper.closeImage();
        } while (files.size() > 0);
        if (postProcessing != null) postProcessing.accept(entryListAll,logger);
        System.out.println("BatchProcessor::batchProcess --- after postProcessing");
        //storing project file is done in ui
        //Date date = Calendar.getInstance().getTime();
        //DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd_hh_mm");
        //String strDate = dateFormat.format(date);
        //projectData.store(new File(projectData.getXmlPath()+File.separator+strDate+"_Project.xml"));
        //preprocess
        //process batches
        //postprocess with all entries?
        LogManager.getLogManager().reset();//release all logger resources
    }

    /**
     * This method should create a standardized way of batch-processing over files with different methods
     * It uses sequential-like computation of the entries (entry depend on previous entry)
     * for example the normal batchprocessing or project illustrations etc.
     *
     * @param projectData
     * @param preProcessing
     * @param entryConsumer
     * @param postProcessing
     */
    public void batchProcessSequential(ProjectData projectData, @Nullable BiConsumer<ShapeContainer,
            Pair<ImageWrapper, Logger>> preProcessing, @Nullable BiConsumer<Entry, Logger> entryConsumer,
                                       @Nullable BiConsumer<List<Entry>, Logger> postProcessing) throws Exception {
        //calculate batch-size
        int junkSize = calculateJunkSize();
        List<File> files = projectData.getImageFiles();
        //no need for copying the list, because its already a new list object, see core.ProjectDataNew
        List<Entry> entryListAll = new ArrayList<>();
        ShapeContainer lastShape = null;
        Dimension2D maxDimension = getMaxDimension(files);

        Logger logger = getLogger(projectData);

        do {//do-while loop
            //if restrict to area is active, the aggregated area from the last image from the previous loop run has to be given as parameter
            ImageWrapper wrapper = initBasic(junkSize, files, projectData, maxDimension, logger);
            System.out.println("BatchProcessor::batchProcess --- after initBasic");
            //pre processing is here
            if (preProcessing != null) preProcessing.accept(lastShape, new Pair<>(wrapper, logger));
            System.out.println("BatchProcessor::batchProcess --- after preProcessing");
            //get last shape of this junk
            lastShape = wrapper.getEntryList().get(wrapper.getEntryList().size() - 1).getShape();
            //main processing
            if (entryConsumer != null)
                MixedUtils.getStream(wrapper.getEntryList(), forceParallel).forEach(entry -> entryConsumer.accept(entry, logger));
            System.out.println("BatchProcessor::batchProcess --- after entryConsumer");
            if (postProcessing != null) entryListAll.addAll(wrapper.getEntryList());

            wrapper.closeImage();
        } while (files.size() > 0);
        if (postProcessing != null) postProcessing.accept(entryListAll, logger);
        System.out.println("BatchProcessor::batchProcess --- after postProcessing");
        LogManager.getLogManager().reset();//release all logger resources
    }


    private void exportDynamicFilamentImages(DataTracking tracking, boolean chkMin, int minLength, boolean chkMax, int maxLength, List<Entry> entryList, File imageDirectory) {

        List<Color> colors = MixedUtils.getColors();
        final int cOffset = 634;//just a random number to shift the starting colors
        List<DynamicFilament> filtered = tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength).
                stream().filter(DynamicFilament::isKeep).collect(Collectors.toList());


        Dimension2D maxDimension = getMaxDimension(entryList.stream().map(e -> new File(e.getPath())).collect(Collectors.toList()));


        //if one image has no filament, the single filament tracking will fail with exception
        //iterate over all images in stack
        IntStream.range(0, entryList.size()).forEach(current -> {
            //retrieve current stack entry
            Entry entry = entryList.get(current);
            //get the orig image as buffered image
            //image has to be loaded first, since the processor is not set
            BufferedImage bi = ImageExporter.getRGB(new File(entry.getPath()), maxDimension);
            //group the filtered DynamicFilaments by their lifespan
            filtered.stream().collect(Collectors.groupingBy(DynamicFilament::getLength)).forEach((key, value) -> {
                //retrieve a color for each lifespan (should be unique)
                Color groupColor = colors.get((key.intValue() + cOffset) % colors.size());
                //get all filaments with specific life span (current key of our grouped data) for the image that is processed
                List<AbstractFilament> filaments = value.stream().filter(df -> df.getFilaments().get(current) != null).flatMap(df -> ((FilamentChain) df.getFilaments().get(current)).getFilaments().stream()).collect(Collectors.toList());// new Pair<>(df, ((FilamentChain)df.getFilaments().get(current - 1)).getFilaments())).collect(Collectors.toList());
                //add filaments onto the image
                ImageExporter.addFilaments(bi, filaments, groupColor);
            });
            try {
                //export the image with all filaments added
                ImageExporter.exportImage(bi, IOUtils.getOutFileFromImageFile(new File(entry.getPath()), imageDirectory, ".png", "cc_sf_filament"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }


    /**
     * Handles the post processing of batch processing
     *
     * @param entryList
     * @param parameters
     * @param dirCsvOutput
     * @param dirImageOutput
     */
    private void batchProcessPostProcess(List<Entry> entryList, Settings parameters, File dirCsvOutput, File dirImageOutput, @Nullable Logger logger) {
        if (parameters.getValueAsBoolean(Batch.doSingleFilamentTracking)) {
            DataTracking tracking = new DataTracking();
            tracking.loadData(entryList);
            if (tracking.findSolitaryFilaments(parameters.getValueAsDouble(SFTracking.max_dist),
                    parameters.getValueAsDouble(SFTracking.factor_angle),
                    parameters.getValueAsDouble(SFTracking.factor_length),
                    parameters.getValue(SFTracking.length))) {
                if (tracking.trackFilaments(parameters.getValueAsDouble(SFTracking.max_dist),
                        parameters.getValueAsDouble(SFTracking.factor_angle),
                        parameters.getValueAsDouble(SFTracking.factor_length),
                        parameters.getValue(SFTracking.length), parameters.getValueAsBoolean(SFTracking.combineMultiMatches))) {

                    boolean chkMin = parameters.getValueAsBoolean(SFTracking.chkExistsInMin);
                    boolean chkMax = parameters.getValueAsBoolean(SFTracking.chkExistsInMax);
                    int minLength = parameters.getValue(SFTracking.existsInMin);
                    int maxLength = parameters.getValue(SFTracking.existsInMax);

                    ImageWrapper pseudoWrapper = new ImageWrapper();
                    pseudoWrapper.getEntryList().addAll(entryList);

                    //export stuff here
                    //images as stack?
                    BufferedImage image = ImageExporter.getFilamentTrackingOverview(tracking, chkMin, minLength, chkMax, maxLength);

                    try {
                        ImageIO.write(image, "png", IOUtils.getOutFileFromImageFile(new File(entryList.get(0).getPath()), dirImageOutput, ".png", "sf_overview"));
                        FilamentCsvExport.exportDynamicFilamentCsv(tracking, pseudoWrapper, dirCsvOutput, parameters);
                        FilamentCsvExport.exportDynamicFilamentCsvGroupedByTime(tracking, pseudoWrapper, dirCsvOutput, parameters);
                        this.exportDynamicFilamentImages(tracking, chkMin, minLength, chkMax, maxLength, entryList, dirImageOutput);
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        if (logger != null) logger.log(Level.SEVERE, sw.toString());
                    }

                }
            }


        }


        // Post processing
        if (parameters.getValueAsBoolean(Batch.determineOrientationFields) && parameters.getValueAsBoolean(Batch.postProcessOrientationFields)) {
            if (logger != null) logger.info("orientation Field post processing for directory");
            OFCalculator.orientationFieldPostProcessing(entryList);
        }

        entryList.forEach(entry -> {
            try {
                List<BufferedImage> images;

                if (parameters.getValueAsBoolean(Batch.determineOrientationFields) && parameters.getValueAsBoolean(Batch.saveOrientationFieldImages)) {
                    images = OFCalculator.orientationFieldImages(ImageExporter.getRGB(new File(entry.getPath())), entry, parameters.getValueAsColor(Batch.color));
                } else images = new ArrayList<>();
                FilamentCsvExport.exportFilamentCSV(dirCsvOutput, false, entry);
                entry.releaseResources();
                if (!images.isEmpty()) {
                    try {
                        ImageExporter.exportImage(images.remove(0), "noise", dirImageOutput, new File(entry.getPath()).getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        if (logger != null) logger.log(Level.SEVERE, sw.toString());
                    }
                    int i = 1;
                    for (BufferedImage image : images) {
                        ImageExporter.exportImage(image, "orientation_field" + i++, dirImageOutput, new File(entry.getPath()).getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                if (logger != null) logger.log(Level.SEVERE,sw.toString());
            }
        });

    }


    /**
     * This method should process all files inside the project data and stores the processed data (images, csv, xml) and
     * also stores the filterqueues-(probably only the pre-processing, because line sensor queue can be derived from projectData.getSettings()
     * it also stores which area plugin was used (a member of projectData), and projectData is also xml-serialized at the end of the batch,
     * practically it also could be serialized at the beginning, since it does not contain any of the processed data.
     *
     * @param projectData
     * @param fQPreProcessing
     * @param fQLineSensor
     * @throws Exception
     */
    public void batchProcess(ProjectData projectData, FilterQueue fQPreProcessing, FilterQueue fQLineSensor) throws Exception {
        BiConsumer<Entry, Logger> entryConsumer = (entry, logger) -> {
            try {
                Tracer tracer = null;
                if (projectData.getSettings().getValue(Trace.curve) > 0) {
                    tracer = new CurveTracer();
                } else {
                    tracer = new LineSensor();
                }

                Dimension2D maxDimension = new Dimension(entry.getProcessor().getWidth(), entry.getProcessor().getHeight());

                //System.out.println("ImageWrapper::scanFilaments --- start " + entry.getPath());
                ShapeContainer shape = entry.getShape();
                IBinaryImage tmpBin = null;
                if (shape != null && shape.getSelectedArea() != null) tmpBin = shape.getSelectedArea().getBinaryImage();
                IBinaryImage interior = entry.getDataFilament().scanFilaments(entry.getProcessor(), tmpBin, projectData.getSettings(), tracer);
                entry.getOrientationFieldContainer().setOrientationField(tracer.getOrientationField());

                if (projectData.getSettings().getValue(Trace.split) * projectData.getSettings().getValue(Trace.curve) > 0) {
                    entry.getDataFilament().splitToLinear(projectData.getSettings().getValue(Trace.step));//previously fixed value of 10
                }

                if (tmpBin != null && interior != null) {
                    entry.getInteriorContainer().setInteriorData(MeanBrightnessCalc.areaAndMeanBrightness(interior, tmpBin, entry.getProcessor()));
                    entry.getInteriorContainer().setInterior(interior);
                }
                //currently only calculate orientation field for selected area(which is the largest area at this moment 2021-04-12)
                //foreach shape calculate orientation field
                //entry.getShape().getAreas().forEach(cellShape->
                //        CellPlugin.calculateOrderParameter(entry.getDataFilament().getTracer().getOrientationField(),cellShape)
                //);
                if (entry.getShape().getSelectedArea() != null) {
                    CellPlugin.calculateOrderParameter(entry.getOrientationFieldContainer().getOrientationField(), entry.getShape().getSelectedArea());
                }

                exportFilamentData(entry, maxDimension, projectData.getSettings(), projectData.getCsvPath(), projectData.getImageOutputPath(), projectData.getXmlPath(), logger);
                //remove slice from image?
                cleanUpEntry(entry);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                if (logger != null) logger.log(Level.SEVERE, sw.toString());

                e.printStackTrace();
            }
        };

        BiConsumer<List<Entry>, Logger> postProcessingConsumer = (entries, logger) ->
                batchProcessPostProcess(entries, projectData.getSettings(), projectData.getCsvPath(), projectData.getImageOutputPath(), logger);


        if (!projectData.getSettings().getValueAsBoolean(Batch.restrictAreaChanges)) {
            batchProcessParallel(projectData, (wrapper, logger) -> {
                //default parallel area computing
                try {
                    wrapper.initializeShape(projectData.getPlugin()).get();
                    //wait till the whole area processing is done
                } catch (Exception e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (logger != null) logger.log(Level.SEVERE,sw.toString());
                }

                //06.11.2019 in the init shape method there is some problem? (shape container == null)
                List<Entry> emptyShape = wrapper.getEntryList().stream().filter(e -> e.getShape() == null).collect(Collectors.toList());
                if (emptyShape.size() > 0) {
                    emptyShape.forEach(e -> System.out.println(e.getPath()));
                    throw new RuntimeException("Shapes empty");
                }

                //maybe necessary to keep images (store filaments on original image etc.)
                //calculate images
                ProcessingUtils.preProcess(wrapper, fQPreProcessing, f -> { });//ignore reporter
                //default parallel area computing

                ProcessingUtils.lineSensor(wrapper, wrapper, fQLineSensor, f -> { });//ignore reporter
            }, entryConsumer, postProcessingConsumer);

        } else {
            batchProcessSequential(projectData, (lastShape, pair) -> {
                Logger logger = pair.getValue();
                ImageWrapper wrapper = pair.getKey();
                try {
                    //sequential area computing with area of predecessor as mask for successor area
                    wrapper.initializeShapeRestricted(projectData.getPlugin(), lastShape).get();//wait till whole area processing is done
                } catch (Exception e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (logger != null) logger.log(Level.SEVERE, sw.toString());
                    //throw a run time exception? or at least log exception
                }
                //maybe necessary to keep images (store filaments on original image etc.)
                //calculate images
                System.out.println("ProcessingUtils::batchProcessing() --- start preprocessing current junk");
                ProcessingUtils.preProcess(wrapper, fQPreProcessing, f -> {
                });//ignore reporter

                System.out.println("ProcessingUtils::batchProcessing() --- start line sensor current junk");
                ProcessingUtils.lineSensor(wrapper, wrapper, fQLineSensor, f -> {
                });//ignore reporter
            }, entryConsumer, postProcessingConsumer);
        }

    }


    /**
     * @param entry
     * @param dp
     * @param dirCsvOutput
     * @param dirImageOutput do each file write in a thread (executor service cachedthreadpool, and wait for completion at the end)
     */
    private void exportFilamentData(Entry entry, Dimension2D maxDimension, Settings dp, File dirCsvOutput, File dirImageOutput, File dirXmlOutput, @Nullable Logger logger) {
        //export images

        Color color = dp.getValueAsColor(Batch.color);

        try {
            if (dp.getValueAsBoolean(Batch.determineOrientationFields)) {
                try {
                    OFCalculator.calculateOrientationFields(entry, entry.getShape().getSelectedAreaExt(), dp, false);
                    //exception dropped here, possible dependency problem
                    //infinite loop on special test picture
                } catch (IOException e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (logger != null) logger.log(Level.SEVERE, sw.toString());
                }
            }
            CompletableFuture<Void> f1 = null, f2, f3 = null, f4 = null;

            if (dp.getValueAsBoolean(Batch.calculateFingerprints) && entry.getShape().getSelectedArea() != null) {
                f1 = CompletableFuture.runAsync(() -> {
                    try {
                        IOUtils.exportFingerprint(entry, entry.getShape().getSelectedArea(), dirCsvOutput, new File(entry.getPath()).getName(), dp);
                    } catch (IOException e) {
                        e.printStackTrace();
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        if (logger != null) logger.log(Level.SEVERE, sw.toString());
                    }
                }, service);
            }


            BufferedImage image;
            if (color.equals(Color.white)) {
                //empty image
                image = new BufferedImage(entry.getProcessor().getWidth(), entry.getProcessor().getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            } else {
                image = ImageExporter.getRGB(new File(entry.getPath()), maxDimension); //image has to be padded to max dimension
            }
            ImageExporter.addFilaments(image, entry.getDataFilament(), color);
            //store to file
            f2 = CompletableFuture.runAsync(() -> {
                try {
                    ImageExporter.exportImage(image, "filaments", dirImageOutput, new File(entry.getPath()).getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (logger != null) logger.log(Level.SEVERE, sw.toString());
                }
            }, service);


            if (dp.getValue(Trace.no_boundary) == 1) {
                //mask = resulting binaryImage from scanFilaments, cell = cell area Binary Image
                //m_int_image_initial = original image
                if (entry.getShape().getSelectedArea() != null && entry.getInteriorContainer().getInterior() != null) {
                    f3 = CompletableFuture.runAsync(() -> {
                        try {
                            Opener opener = new Opener();
                            ImagePlus ip = opener.openImage(entry.getPath());
                            BufferedImage imageInterior = ImageFactory.makeTwoLevelRedGreenImage(ip.getProcessor(), entry.getInteriorContainer().getInterior(), entry.getShape().getSelectedArea().getBinaryImage());
                            ImageExporter.addFilaments(imageInterior, entry.getDataFilament(), color);
                            ImageExporter.exportImage(imageInterior, "interior", dirImageOutput, new File(entry.getPath()).getName());
                            ip.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            if (logger != null) logger.log(Level.SEVERE, sw.toString());
                        }
                    }, service);
                }
            }

            f4 = CompletableFuture.runAsync(() -> {
                try {
                    IOUtils.writeXML(entry, dirXmlOutput, new File(entry.getPath()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (logger != null) logger.log(Level.SEVERE, sw.toString());
                }
            }, service);


            if (f1 != null) {
                //handle f1 exceptions and .get() to block and wait, repeat the same for f2 and f3
                f1.exceptionally((exception) -> {
                    exception.printStackTrace();
                    //log
                    return null;
                });
                f1.get();
            }
            if (f2 != null) {
                f2.exceptionally((exception) -> {
                    exception.printStackTrace();
                    //log
                    return null;
                });
                f2.get();
            }
            if (f3 != null) {
                f3.exceptionally((exception) -> {
                    exception.printStackTrace();
                    //log
                    return null;
                });
                f3.get();
            }

            f4.exceptionally((exception) -> {
                exception.printStackTrace();
                //log
                return null;
            });
            f4.get();

        } catch (Exception e) {
            //log exception
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            if (logger != null) logger.log(Level.SEVERE,sw.toString());
        }


    }

    private void cleanUpEntry(Entry entry) {
        //set interior data to null
        entry.getInteriorContainer().setInterior(null);
        entry.getShape().getMechAreas().clear();
        entry.getOrientationFieldContainer().setOrientationField(null);
        entry.setProcessor(null);
        //set extended area and mech area null
    }

}
