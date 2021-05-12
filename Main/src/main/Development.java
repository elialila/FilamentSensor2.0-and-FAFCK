package main;

import core.*;
import core.cell.ShapeContainer;
import core.filaments.AbstractFilament;
import core.image.Entry;
import core.tracers.CurveTracer;
import core.tracers.Tracer;
import filters.*;
import focaladhesion.DataExtractor;
import focaladhesion.FocalAdhesionProcessor;
import ij.IJ;
import ij.ImagePlus;

import ij.Prefs;
import ij.gui.HistogramWindow;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.process.AutoThresholder;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import core.cell.CellPlugin;
import core.cell.CellShape;
import core.cell.plugins.CellPluginBenjamin;
import core.cell.plugins.CellPluginSimple;
import core.cell.plugins.CellPluginTest;
import core.cell.plugins.CellPluginThresholding;
import evaluation.EvaluationData;
import evaluation.Evaluator;

import tracking.area.AreaTracker;
import tracking.area.CellEvent;
import tracking.filament.DataTracking;
import core.image.BinaryImage;
import core.image.IBinaryImage;
import core.image.ImageWrapper;
import core.settings.*;


import util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static main.Main.*;


/**
 * This class contains just a lot of quick and dirty testing and was also used for prototype creation of methods
 * <p>
 * THIS CLASS HAS NO REAL RELEVANCE
 */
public class Development {


    private static void testSerializeFilterQueue(Settings dp) {
        try {
            FileOutputStream fos = new FileOutputStream(getXmlOutputPath() + File.separator + "testFilterSerialization.xml");
            XMLEncoder encoder = new XMLEncoder(fos);
            FilterQueue filters = new FilterQueue();

            FilterCrossCorrelation fcc = new FilterCrossCorrelation(dp.getValue(Pre.corr_zero), dp.getValue(Pre.corr_mask_size));
            fcc.preCalc();
            filters.add(fcc);

            FilterEnhanceContrast fec = new FilterEnhanceContrast(10, 240);
            filters.add(fec);

            filters.add(new FilterScale(0.2));


            encoder.writeObject(filters);
            encoder.flush();
            encoder.close();


            XMLDecoder decoder = new XMLDecoder(new FileInputStream(getXmlOutputPath() + File.separator + "testFilterSerialization.xml"));

            FilterQueue filters2 = (FilterQueue) decoder.readObject();
            decoder.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }


    private static void testAreaCheck(Settings dp) {

        int min_range = dp.getValue(Pre.min_range);
        File source = Arrays.stream(new File(getSourcePath()).listFiles()).filter(f -> f.getName().contains("10056")).findAny().orElse(null);
        if (source == null) {
            System.out.println("Input-File not found!");
            return;
        }
        System.out.println("MinRange:" + min_range);
        ImageWrapper imageWrapper = new ImageWrapper(source, dp);
        try {
            imageWrapper.getWorker().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        CellShape shape = imageWrapper.getEntryList().stream().filter(e -> e.getPath().equals(source.getAbsolutePath())).map(e -> e.getShape().getAreas().get(0)).findAny().orElse(null);
        if (shape != null) {
            System.out.println(shape.getArea() + "/" + (shape.getBinaryImage().getWidth() * shape.getBinaryImage().getHeight()));
        }
        try {
            ImageExporter.exportImage(imageWrapper.getImage().getProcessor(), new File(getImgOutputPath() + File.separator + "Clean.png"));
            ImageExporter.exportImage(shape.getBinaryImage(), new File(getImgOutputPath() + File.separator + "Shape.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        AutoThresholder autoThresholder = new AutoThresholder();

        List<AutoThresholder.Method> methods = Arrays.asList(AutoThresholder.Method.Yen, AutoThresholder.Method.Li, AutoThresholder.Method.Otsu, AutoThresholder.Method.Triangle);

        final int[] histogram = imageWrapper.getImage().getProcessor().getHistogram();

        ImageProcessor p1 = imageWrapper.getImage().getProcessor().duplicate();
        List<Integer> thresholds = methods.stream().map(method -> autoThresholder.getThreshold(method, histogram)).sorted().collect(Collectors.toList());
        System.out.println("Main::" + thresholds);

    }


    /**
     * @todo maybe usable in the future; compile own produced filters for use in filamentsensor
     *
     * Prototype method for writing filters and compiling them on the fly
     * for easier addition of custom filters
     * <p>
     * the next "problem" would be creating gui elements on those custom filters
     * probably most used parameters will be int, double, boolean
     * should be possible to create a gui creator with reflection which is matching properties with gui elements
     */
    public static void testCompileFilters() {


        File source = new File(getSourcePath()).listFiles()[0];
        ImagePlus imagePlus = IJ.openImage(source.getAbsolutePath());
        ImageProcessor image = imagePlus.getProcessor();
        String fullClassName = "testpkg.filters.TestFilter";


        File classPath = new File(".");

        File pathToClass = new File(testDirectoryRootPath + File.separator + "TestFilter.java");

        //check for development environment and use a different classpath

        String parent = classPath.getAbsolutePath().substring(0, classPath.getAbsolutePath().length() - 2);

        File chkJar = new File(parent + File.separator + "out" + File.separator + "artifacts" + File.separator + "Main_jar" + File.separator + "Main.jar");

        String command =
                "javac -cp " + ((!chkJar.exists()) ? (parent + File.separator + "*.jar") : chkJar.getAbsolutePath()) + " " +
                        "-d " + parent + " " +
                        pathToClass.getAbsolutePath();

        try {
            System.out.println(command);
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            reader.lines().forEach(l -> System.out.println(l));

            BufferedReader reader2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            reader2.lines().forEach(l -> System.out.println(l));


            // Create a File object on the root of the directory containing the class file


            try {
                // Convert File to a URL
                URL url = new File(parent).toURI().toURL();          // file:/c:/myclasses/
                URL[] urls = new URL[]{url};

                // Create a new class loader with the directory
                ClassLoader cl = new URLClassLoader(urls);

                // Load in the class; MyClass.class should be located in
                // the directory file:/c:/myclasses/com/mycompany
                Class cls = cl.loadClass(fullClassName);
                IFilter filter = (IFilter) cls.newInstance();
                System.out.println(filter);

                if (filter instanceof IFilterPrecalc) {

                }
                if (filter instanceof IFilterObservable) {

                }
                if (filter instanceof IImageFilterAreaDependant) {

                }
                if (filter instanceof IImageFilter) {
                    ((IImageFilter) filter).run(image);
                }
                if (filter instanceof IStackFilter) {

                }


                ImageExporter.exportImage(image, new File(parent + File.separator + "outputImage.png"));


            } catch (MalformedURLException e) {
            } catch (ClassNotFoundException e) {
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testNewBatchProcessing2(Settings dp) {
        BatchProcessor processor = new BatchProcessor();
        ProjectData projectData = getProject(dp, "TestAll");
        projectData.setPlugin(new CellPluginBenjamin());
        System.out.println(projectData);
        long time = System.currentTimeMillis();
        try {
            processor.batchProcess(
                    projectData, ProcessingUtils.getDefaultPreprocessingFilterQueue(dp),
                    ProcessingUtils.getDefaultLineSensorQueue(dp, false)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Time taken:" + ((double) (System.currentTimeMillis() - time) / 1000) + "s");
    }


    public static void testIllustrateNew(Settings dp) {
        ProjectData projectData = new ProjectData();
        projectData.setRootDir(new File(testDirectoryRootPath));
        projectData.setCsvPath(new File(getCsvOutputPath()));
        projectData.setImagePath(new File(getSourcePath()));
        projectData.setImageOutputPath(new File(getImgOutputPath()));
        projectData.setPlugin(new CellPluginBenjamin());
        projectData.setXmlPath(new File(getXmlOutputPath()));
        projectData.initLists();
        System.out.println(projectData);
        try {
            OFIllustratorBatch processor = new OFIllustratorBatch();
            processor.projectIllustrations(projectData, new File(getImgOutputPath()), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Test Result, ProjectData is now correctly being serialized
     */
    public static void testProjectData() {
        ProjectData pdn = new ProjectData();
        pdn.setRootDir(new File(getSourcePath()));
        pdn.setImageFiles(PathScanner.getFilesInDirectory(pdn.getRootDir(), PathScanner.supportedImageExtensions, PathScanner.filteredNameSuffixes, true));
        pdn.setXmlFiles(PathScanner.getFilesInDirectory(pdn.getRootDir(), Collections.singletonList(".xml"), PathScanner.filteredNameSuffixes, true));
        pdn.setProjectFile(pdn.getXmlFiles().get(0));

        File store = new File(getXmlOutputPath() + File.separator + "testProject.xml");
        try {
            pdn.store(store);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ProjectData pdn2 = null;
        try {
            pdn2 = ProjectData.load(store);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        File result = PathScanner.getCommonParent(new File(getXmlOutputPath()), new File(getSourcePath()));
        System.out.println("Test");
    }

    public static void testCellPluginThresholding(Settings dp) {
        ProjectData project = Main.getProject(dp, null);
        final int minRange = project.getSettings().getValue(Pre.min_range);
        try {
            ImageWrapper wrapper = new ImageWrapper(project.getImageFiles(), project.getSettings());

            int minArea = (int) ((double) wrapper.getWidth() * wrapper.getHeight() * 0.005);//minArea == 0.5% of Image Size
            //minArea percent should be taken into Settings

            CellPluginThresholding cpt = new CellPluginThresholding();
            AutoThresholder autoThresholder = new AutoThresholder();
            List<Integer> thresholds = MixedUtils.getStream(wrapper.getEntryList(), false).
                    map(entry ->
                            autoThresholder.getThreshold(AutoThresholder.Method.Li, entry.getProcessor().getHistogram()
                            )
                    ).collect(Collectors.toList());
            double average = thresholds.stream().mapToInt(i -> i).average().orElse(0);
            if (average != 0) {
                average = Math.ceil(average);
                cpt.thresholdShapeProperty().set((int) average);
                cpt.thresholdExtendedShapeProperty().set((int) (average + 2));
                System.out.println("Average:" + average + ";");
                MixedUtils.getStream(wrapper.getEntryList(), false).forEach(entry -> {
                    entry.setShape(CellPlugin.getCellData(entry.getProcessor(), null, minRange, minArea, cpt));

                    Color test = new Color(255, 0, 0, 127);//Alpha 127 ist schon ziemlich gut, sichtbar und transparent


                    //bench "Calc.largestObjects" against "PointUtils.getClusters" to see which one has better performance
                    //"largestObjects" is clearly the winner 0.xxx sec to xx.xxx sec
                    if (entry.getShape().getAggregatedArea() != null) {
                        //entry.getShape().getAggregatedArea().store(IOUtils.getOutFileFromImageFile(new File(entry.getPath()), project.getImageOutputPath(), ".png", "Debug"));
                        long timeOverall = System.currentTimeMillis();
                        BufferedImage rgbA = ImageExporter.getBufferedImage(entry.getProcessor());
                        Graphics2D graphics = rgbA.createGraphics();
                        entry.getShape().getAggregatedArea().exitMemoryState();
                        BufferedImage shape = ImageExporter.getBufferedImage(entry.getShape().getAggregatedArea().getByteProcessor());
                        long time = System.currentTimeMillis();
                        Image overlay = ImageExporter.getImageForOverlay(shape, test);
                        Main.debugPerformance("getImageOverlay", time);//die zeiten vom getImageForOverlay sind eigentlich ganz gut, was braucht hier so lange?
                        graphics.drawImage(overlay, 0, 0, null);
                        graphics.dispose();
                        Main.debugPerformance("Overall", timeOverall);
                        try {
                            long timeWrite = System.currentTimeMillis();
                            ImageIO.write(rgbA, "png", IOUtils.getOutFileFromImageFile(new File(entry.getPath()), project.getImageOutputPath(), ".png", "Debug"));
                            Main.debugPerformance("Write", timeWrite);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testContrastAdjuster(Settings dp) {
        ProjectData project = Main.getProject(dp, null);
        final int minRange = project.getSettings().getValue(Pre.min_range);
        try {
            ImageWrapper wrapper = new ImageWrapper(project.getImageFiles(), project.getSettings());

            ContrastAdjuster contrastAdjuster = new ContrastAdjuster(wrapper);
            contrastAdjuster.doUpdate(0, false, ContrastAdjuster.AdjusterAction.auto);

            contrastAdjuster.doUpdate(0, false, ContrastAdjuster.AdjusterAction.set);

            contrastAdjuster.apply(0, false);
            File output = IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), project.getImageOutputPath(), ".png", "debug");
            ImageExporter.exportImage(wrapper.getEntryList().get(0).getProcessor().getBufferedImage(), output);

            //compare to ij created auto image
            float result = Main.compareImage(output, IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), project.getImageOutputPath(), ".png", "debug_ij"));
            System.out.println("cmp result=" + result);


            //ij image is set to min=20 manually
            contrastAdjuster.minSliderProperty().set(20);
            contrastAdjuster.doUpdate(1, false, ContrastAdjuster.AdjusterAction.min);
            contrastAdjuster.apply(1, false);
            output = IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(1).getPath()), project.getImageOutputPath(), ".png", "debug_min");
            ImageExporter.exportImage(wrapper.getEntryList().get(1).getProcessor().getBufferedImage(), output);
            result = Main.compareImage(output, IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(1).getPath()), project.getImageOutputPath(), ".png", "debug_ij_min"));
            System.out.println("cmp result2=" + result);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void testRecognizeDiffSizes(Settings dp) {
        ProjectData project = Main.getProject(dp, "testIReg1");
        BatchProcessor batchProcessor = new BatchProcessor();

        System.out.println("isRestrictAreaChanges=" + project.getSettings().getValueAsBoolean(Batch.restrictAreaChanges));
        project.setPlugin(new CellPluginBenjamin());

        try {
            batchProcessor.batchProcess(project,
                    ProcessingUtils.getDefaultPreprocessingFilterQueue(project.getSettings()),
                    ProcessingUtils.getDefaultLineSensorQueue(project.getSettings(), false)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The test-method works, introduce image dependency in the UI for updates (chain update should only be used in preview and on button click "onUpdateImages"
     * if its used on whole stack for changes it probably kills the program
     *
     * @param dp
     */
    public static void testImageDependency(Settings dp) {
        ProjectData project = Main.getProject(dp, "testFocalAdhesion");


        boolean oldPref = Prefs.blackBackground;
        // (the whole invert stuff could be dropped due to this?)
        Prefs.blackBackground = true;//this should be set in the program on a centralized place

        //Process Fibers
        File stressFibersStack = project.getImageFiles().stream().filter(s -> s.getName().contains("StressFibers.ome")).findAny().orElse(null);
        ImageWrapper wrapperFibers = new ImageWrapper(stressFibersStack, dp);
        System.out.println("Get Shapes");
        try {
            wrapperFibers.initializeShape(new CellPluginBenjamin());
            wrapperFibers.getWorker().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ImageWrapper pre = wrapperFibers.clone();
        ImageWrapper line = wrapperFibers.clone();
        ImageWrapper fil = wrapperFibers;

        ProcessingUtils.establishImageDependency(wrapperFibers, pre, line, fil,
                ProcessingUtils.getDefaultPreprocessingFilterQueue(dp),
                ProcessingUtils.getDefaultLineSensorQueue(dp, false), dp, new SimpleDoubleProperty(1));

        try {
            //it loops forever in the scanFilaments (processing the whole queue again and again)
            //2020-01-28 issue resolved by introducing circle detection - if circular dependency detected it just not completes the circle
            //next issue chainUpdate==false does not change anything it runs trough the whole chain
            //2020-01-28 issue resolved by adding an if-statement
            CompletableFuture<Void> future = wrapperFibers.updateDependencies(wrapperFibers, false).thenAccept(v -> {
                System.out.println("Any Filaments found:" + wrapperFibers.getEntryList().stream().anyMatch(e -> e.getDataFilament().count() > 0));

                ImageExporter.addFilaments(wrapperFibers, Color.RED, Color.magenta, true);
                ImageExporter.exportImage(wrapperFibers.getImage(),
                        IOUtils.getOutFileFromImageFile(stressFibersStack, project.getImageOutputPath(), ".ome.tif", ""));


            });
            future.exceptionally(exception -> {
                throw new RuntimeException(exception);

            });
            future.get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }


    public static void testToleranceImpactOnFilamentWidth(Settings dp) {

        //take a test image, calculate filaments width different tolerance setting and compare values (widthmap size etc.)
        ProjectData project = getProject(dp, "Test5");//TestTolerance

        dp.setProperty(Trace.minlen, 5);
        dp.setProperty(Trace.step, 2);

        try {
            ImageWrapper orig = new ImageWrapper(project.getImageFiles(), dp);
            ImageWrapper pre = orig.clone();
            ImageWrapper line = orig.clone();
            ImageWrapper fil = orig.clone();

            orig.initializeShape(new CellPluginBenjamin()).get();

            ProcessingUtils.initializePreprocessing(orig, 1d, pre);
            ProcessingUtils.preProcess(pre, ProcessingUtils.getDefaultPreprocessingFilterQueue(dp), (f) -> {
            });
            ProcessingUtils.lineSensor(pre, line, ProcessingUtils.getDefaultLineSensorQueue(dp, false), (f) -> {
            });
            //basic calculations done


            //ProcessingUtils.filamentSensor(line,dp);


            //Entry entry = line.getEntryList().get(0);

            line.getEntryList().forEach(entry -> {

                //ImageExporter.exportImage(entry.getProcessor(),new File(project.getImageOutputPath().getAbsolutePath()+File.separator+"binary.png"));

                ShapeContainer shape = entry.getShape();
                IBinaryImage tmpBin = null;
                if (shape != null && shape.getSelectedArea() != null) tmpBin = shape.getSelectedArea().getBinaryImage();


                Tracer tracer5 = new CurveTracer();
                List<AbstractFilament> filsTolerance5 = tracer5.scanFilaments(entry.getProcessor(),
                        0.05d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer10 = new CurveTracer();
                List<AbstractFilament> filsTolerance10 = tracer10.scanFilaments(entry.getProcessor(),
                        0.1d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer15 = new CurveTracer();
                List<AbstractFilament> filsTolerance15 = tracer15.scanFilaments(entry.getProcessor(),
                        0.15d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer20 = new CurveTracer();
                List<AbstractFilament> filsTolerance20 = tracer20.scanFilaments(entry.getProcessor(),
                        0.20d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer25 = new CurveTracer();
                List<AbstractFilament> filsTolerance25 = tracer25.scanFilaments(entry.getProcessor(),
                        0.25d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));


                Tracer tracer30 = new CurveTracer();
                List<AbstractFilament> filsTolerance30 = tracer30.scanFilaments(entry.getProcessor(),
                        0.3d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer35 = new CurveTracer();
                List<AbstractFilament> filsTolerance35 = tracer35.scanFilaments(entry.getProcessor(),
                        0.35d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer40 = new CurveTracer();
                List<AbstractFilament> filsTolerance40 = tracer40.scanFilaments(entry.getProcessor(),
                        0.40d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer45 = new CurveTracer();
                List<AbstractFilament> filsTolerance45 = tracer45.scanFilaments(entry.getProcessor(),
                        0.45d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));

                Tracer tracer50 = new CurveTracer();
                List<AbstractFilament> filsTolerance50 = tracer50.scanFilaments(entry.getProcessor(),
                        0.5d,
                        dp.getValue(Trace.minlen),
                        dp.getValue(Trace.minangle),
                        dp.getValue(Trace.step));


                boolean[][] binImage = new BinaryImage(entry.getProcessor().getIntArray(), 254).toBoolean();

                ((CurveTracer) tracer5).updateFilamentWidth(binImage);
                ((CurveTracer) tracer10).updateFilamentWidth(binImage);
                ((CurveTracer) tracer15).updateFilamentWidth(binImage);
                ((CurveTracer) tracer20).updateFilamentWidth(binImage);
                ((CurveTracer) tracer25).updateFilamentWidth(binImage);
                ((CurveTracer) tracer30).updateFilamentWidth(binImage);
                ((CurveTracer) tracer35).updateFilamentWidth(binImage);
                ((CurveTracer) tracer40).updateFilamentWidth(binImage);
                ((CurveTracer) tracer45).updateFilamentWidth(binImage);
                ((CurveTracer) tracer50).updateFilamentWidth(binImage);


                try {


                    //#region image output
                    BufferedImage origBi = orig.getImage(1);
                    ImageExporter.addFilaments(origBi, filsTolerance5, Color.blue);
                    ImageIO.write(origBi, "png", new File(project.getImageOutputPath().getAbsolutePath() + File.separator + new File(entry.getPath()).getName()));

                    origBi = orig.getImage(1);
                    ImageExporter.addFilaments(origBi, filsTolerance50, Color.blue);
                    ImageIO.write(origBi, "png", new File(project.getImageOutputPath().getAbsolutePath() + File.separator + new File(entry.getPath()).getName() + "_50.png"));


                    origBi = line.getImage(1);

                    ImageExporter.addFilaments(origBi, filsTolerance50, Color.cyan);

                    ImageExporter.addFilaments(origBi, filsTolerance5, Color.orange);


                    ImageIO.write(origBi, "png", new File(project.getImageOutputPath().getAbsolutePath() + File.separator + new File(entry.getPath()).getName() + "_width_cmp.png"));


                    //origBi=orig.getImage(1);
                    //ImageExporter.addFilaments(origBi,filsTolerance10.stream().filter(f->!f.isPossibleError()).collect(Collectors.toList()), Color.blue);
                    //ImageIO.write(origBi,"png",new File(project.getImageOutputPath().getAbsolutePath()+File.separator+new File(entry.getPath()).getName()+"_clean.png"));


                    //origBi=orig.getImage(1);
                    //ImageExporter.addFilaments(origBi,filamentsTest,Color.cyan);
                    //ImageExporter.addFilaments(origBi,fils2,Color.magenta);
                    //ImageIO.write(origBi,"png",new File(project.getImageOutputPath().getAbsolutePath()+File.separator+"output_filament2.png"));

                    //origBi=orig.getImage(1);
                    //ImageExporter.addFilaments(origBi,filamentsTest,Color.cyan);
                    //ImageExporter.addFilaments(origBi,fils3,Color.magenta);
                    //ImageIO.write(origBi,"png",new File(project.getImageOutputPath().getAbsolutePath()+File.separator+"output_filament3.png"));
                    //#endregion


                } catch (IOException e) {
                    e.printStackTrace();
                }


            });


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public static void storeAsStack(Settings dp) {
        ProjectData project = getProject(dp, "TestAll");
        try {
            ImageWrapper stack = new ImageWrapper(project.getImageFiles(), project.getSettings());
            ImageExporter.exportImage(stack.getImage(), IOUtils.getOutFileFromImageFile(project.getImageFiles().get(0), project.getImageOutputPath(), ".ome.tif", "all"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void testFilamentTracking(Settings dp) {

        ProjectData project = getProject(dp, "TestFilm");
        try {
            Prefs.blackBackground = true;
            ImageWrapper wrapper = new ImageWrapper(project.getImageFiles(), dp);

            CellPlugin cp = new CellPluginBenjamin();
            wrapper.initializeShape(cp).get();
            ImageWrapper preWrapper = wrapper.clone();
            ImageWrapper lineWrapper = wrapper.clone();
            System.out.println("AREA DONE");


            FilterQueue pre = ProcessingUtils.getDefaultPreprocessingFilterQueue(dp);
            FilterQueue line = ProcessingUtils.getDefaultLineSensorQueue(dp, false);
            ProcessingUtils.preProcess(preWrapper, pre, (f) -> {
            });
            System.out.println("PREPROCESSING DONE");
            ProcessingUtils.lineSensor(preWrapper, lineWrapper, line, (f) -> {
            });
            System.out.println("LINESENSOR DONE");
            long time = System.currentTimeMillis();
            ProcessingUtils.filamentSensor(lineWrapper, dp);
            FilamentSensor.debugPerformance("FilamentScan:", time);
            System.out.println("FILAMENTS DONE");
            DataTracking dataTracking = new DataTracking();
            dataTracking.loadData(wrapper);
            System.out.println("LOAD DATA DONE");
            time = System.currentTimeMillis();
            dataTracking.findSolitaryFilaments(15, 4, 1, 10);
            FilamentSensor.debugPerformance("findSolitaryFilaments:", time);
            System.out.println("FIND SOLITARY FILAMENTS DONE");
            time = System.currentTimeMillis();
            boolean result = dataTracking.trackFilaments(15, 4, 1, 10, true);
            FilamentSensor.debugPerformance("trackFilaments:", time);
            System.out.println("TRACKING DONE");
            System.out.println("result=" + result);
            System.out.println("Dynamic Filaments:" + dataTracking.getTrackedFilaments().size());


            //tracked filaments 7797
            //m_filament_data.size==144
            //image is 550 x 23000 large, scale image better
            //x should be variable depending on max time
            //y should be reduced 1) by dropping filaments that only exist in a few times (as parameter)
            //reduce line height
            System.out.println("Max Time:" + dataTracking.getMaxTime());

            //timings test 0: filamentscan 39,249s; findSolitaryFilaments(54,545s); trackFilaments: 45,255s
            //timings test 1: filamentscan 38,059s; findSolitaryFilaments(10,708s); trackFilaments: 46,684s
            //changes for test 1: run a parallel stream foreach in the getDistanceMatrices method
            //timings test 2: filamentscan 40,28s; findSolitaryFilaments(10,024s); trackFilaments: 7,424s
            final boolean chkMinLength = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
            final int minLength = dp.getValue(SFTracking.existsInMin);
            final boolean chkMaxLength = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
            final int maxLength = dp.getValue(SFTracking.existsInMax);


            BufferedImage image = ImageExporter.getFilamentTrackingOverview(dataTracking, chkMinLength, minLength, chkMaxLength, maxLength);
            ImagePlus imp = new ImagePlus("Overview", image);
            ImageWindow iwOverView = new ImageWindow(imp);
            iwOverView.setVisible(true);

            //ImageExporter.exportImage(imp,new File(project.getImageOutputPath().getAbsolutePath()+File.separator+"output.png"));

            ImageWrapper output = wrapper.clone();
            ImageExporter.addFilaments(output, Color.red, Color.green, true);
            StackWindow sw = new StackWindow(output.getImage());
            sw.setVisible(true);
            //good possibility to follow a tracked filament
            //diagram with time series on X and a filament-life-line Y axis is just the filament-number
            //additional information on the x axis could be the number of filaments found
            ImageWrapper output2 = ImageExporter.addFilaments(wrapper, dataTracking.getTrackedFilaments().get(0), Color.red, Color.green, true);
            StackWindow sw2 = new StackWindow(output2.getImage());
            sw2.setVisible(true);


            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testAreaTrackingV2(Settings dp) {
        ProjectData project = getProject(dp, "testAll");

        //ugly doesn't work well since area calculation is unable to produce useful results for that images
        ProjectData uglyProject = getProject(dp, "testAreaCorrelator");
        File testUgly = uglyProject.getImageFiles().stream().filter(f -> f.getName().equals("Pos86_small.ome.tif")).findAny().orElse(null);

        boolean ugly = false;

        try {
            ImageWrapper wrapper;
            if (!ugly || testUgly == null) wrapper = new ImageWrapper(project.getImageFiles(), dp);
            else wrapper = new ImageWrapper(testUgly, dp);
            //init area
            CellPlugin plugin = new CellPluginBenjamin();
            wrapper.initializeShape(plugin).get();

            //Tracking
            AreaTracker tracker = new AreaTracker();
            tracker.process(wrapper, dp, (progress) -> {
            });

            ImageWrapper rgbWrapper = ImageConversionUtils.convertToRGB(wrapper, true);
            ImageWrapper rgbWrapper2 = ImageConversionUtils.convertToRGB(wrapper, true);

            List<Color> colors = MixedUtils.getColors().subList(12, 50);

            //show results
            for (int i = 0; i < rgbWrapper.getSize(); i++) {
                final int n = i;
                for (int j = 0; j < tracker.getUniqueAreas().size(); j++) {
                    CellEvent event = tracker.getUniqueAreas().get(j).getAreas().get(n);
                    Color color = colors.get(j);
                    rgbWrapper2.getEntryList().get(n).getProcessor().setColor(color);
                    if (event != null) {
                        if (event instanceof CellEvent.CellStartEvent) {
                            Rectangle rect = ((CellEvent.CellStartEvent) event).getSource().getBounds();
                            rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.YELLOW);
                            rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);

                        } else if (event instanceof CellEvent.CellAliveEvent) {
                            Rectangle rect = ((CellEvent.CellAliveEvent) event).getTarget().getBounds();
                            rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.GREEN);
                            rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                        } else if (event instanceof CellEvent.CellSplitEvent) {
                            ((CellEvent.CellSplitEvent) event).getTarget().stream().map(e -> e.getBounds()).forEach(rect -> {
                                rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.RED);
                                rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                                rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            });
                        } else if (event instanceof CellEvent.CellFusionEvent) {
                            Rectangle rect = ((CellEvent.CellFusionEvent) event).getTarget().getBounds();
                            rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.BLUE);
                            rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                        } else if (event instanceof CellEvent.CellTouchEvent) {
                            Rectangle rect = ((CellEvent.CellTouchEvent) event).getTarget().getBounds();
                            rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.MAGENTA);
                            rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                        } else if (event instanceof CellEvent.CellDeTouchEvent) {
                            ((CellEvent.CellDeTouchEvent) event).getTarget().stream().map(e -> e.getBounds()).forEach(rect -> {
                                rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.orange);
                                rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                                rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            });
                        } else if (event instanceof CellEvent.CellEndEvent) {
                            Rectangle rect = ((CellEvent.CellEndEvent) event).getSource().getBounds();
                            rgbWrapper.getEntryList().get(n).getProcessor().setColor(Color.GRAY);
                            rgbWrapper.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);
                            rgbWrapper2.getEntryList().get(n).getProcessor().drawRect(rect.x, rect.y, rect.width, rect.height);

                        }
                    }
                }
            }
            StackWindow stackWindow = new StackWindow(rgbWrapper.getImage());
            stackWindow.setVisible(true);

            StackWindow stackWindow2 = new StackWindow(rgbWrapper2.getImage());
            stackWindow2.setVisible(true);

            Thread.currentThread().join();


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}





