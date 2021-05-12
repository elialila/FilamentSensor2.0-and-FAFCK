package main;

import core.FilamentSensor;
import core.settings.Config;
import util.ProcessingUtils;
import util.Annotations.Nullable;
import model.MainModel;
import model.ProjectModel;
import org.apache.commons.io.FileUtils;


import core.ProjectData;
import core.settings.Settings;
import core.image.BinaryImage;
import view.MainWindow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


/**
 * @note Test data is done with flag[3]=false; if the difference is too much it will result in wrong result
 * @note keep that in mind
 * @note automatic test needs another true data for flag[3]==true
 */
public class Main {


    public static final boolean DEBUG = true;//set to true for focal adhesion testing (path's will be inserted automatically)


    //will automatically switch flag[3] to true if F3 is added to name
    //currently only Test5 has true data for F3
    public static String testDirectoryName = "TestAll";//Test5F3
    public static String testDirectoryRootPath = "G:\\Test\\Filamentsensor Testdata\\";

    public static final String DIR_PLUGIN_FILTER = "plugin_filter";
    public static final String DIR_PLUGIN_AREA = "plugin_area";
    public static final String DIR_FILTER_LIST = "filter_lists";


    public static final String sourceImageDirectoryName = "orig";
    public static final String outputCsvDirectoryName = "csv";
    public static final String outputImgDirectoryName = "img";
    public static final String outputXmlDirectoryName = "xml";
    public static final String outputIllustrationDirectoryName = "illustration";

    public static final String trueCsvName = "true_csv";
    public static final String trueImgName = "true_img";


    public static String getSourcePath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + sourceImageDirectoryName;
    }

    public static String getCsvOutputPath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + outputCsvDirectoryName;
    }

    public static String getImgOutputPath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + outputImgDirectoryName;
    }

    public static String getCsvTruePath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + trueCsvName;
    }

    public static String getImgTruePath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + trueImgName;
    }

    public static String getXmlOutputPath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + outputXmlDirectoryName;
    }

    public static String getIllustrationOutputPath() {
        return testDirectoryRootPath + testDirectoryName + File.separator + outputIllustrationDirectoryName;
    }


    private File directoryPluginFilters;
    private File directoryPluginArea;
    private File directoryFilterLists;


    public static void testGUI(String[] args) {
        //Prefs.blackBackground=true;
        //Thats how the JavaFX GUI is called
        MainModel model = new MainModel();
        MainWindow mainWindow = new MainWindow();


        if (Main.DEBUG) {
            ProjectModel projectModel = new ProjectModel();
            projectModel.setFileFocalAdhesions(Main.testDirectoryRootPath + File.separator + "testFocalAdhesion" + File.separator + Main.sourceImageDirectoryName + File.separator + "FocalAdhesion.ome.tif");
            projectModel.setFileStressFibers(Main.testDirectoryRootPath + File.separator + "testFocalAdhesion" + File.separator + Main.sourceImageDirectoryName + File.separator + "StressFibers.ome.tif");
            model.setProjectModel(projectModel);
            mainWindow.openDebug(args, model);
        } else {
            System.out.println("Main::testGUI() --- mainWindow.hash=" + mainWindow.hashCode());
            mainWindow.open(args);

        }


    }

    public static ProjectData getProject(Settings dp, @Nullable String testDirectoryNameOverride) {
        ProjectData pdn = new ProjectData();
        pdn.setRootDir(new File(testDirectoryRootPath + ((testDirectoryNameOverride == null) ? testDirectoryName : testDirectoryNameOverride)));
        pdn.setImagePath(new File(pdn.getRootDir().getAbsolutePath() + File.separator + sourceImageDirectoryName));

        File csv = new File(pdn.getRootDir().getAbsolutePath() + File.separator + outputCsvDirectoryName);
        File xml = new File(pdn.getRootDir().getAbsolutePath() + File.separator + outputXmlDirectoryName);
        File out = new File(pdn.getRootDir().getAbsolutePath() + File.separator + outputImgDirectoryName);
        if (!csv.exists()) csv.mkdirs();
        if (!xml.exists()) xml.mkdirs();
        if (!out.exists()) out.mkdirs();
        pdn.setCsvPath(csv);
        pdn.setXmlPath(xml);
        pdn.setImageOutputPath(out);
        pdn.initLists();
        pdn.setSettings(dp);
        return pdn;
    }



    private static void initSavedFilterQueues() {
        Settings dp = new Settings();
        //create default filter lists

        File filtersDirectory = Config.getInstance().getFiltersDirectory();
        File outputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + "default.xml");
        try {
            XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
            encoder.writeObject(ProcessingUtils.getDefaultPreprocessingFilterQueue(dp));
            encoder.flush();
            encoder.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        outputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + "simple.xml");
        try {
            XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
            encoder.writeObject(ProcessingUtils.getSimpleFilterQueue(dp));
            encoder.flush();
            encoder.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initConfig(int calls) {

        File configFile = new File(System.getProperty("user.home") + File.separator + ".filamentsensor" + File.separator + "application-settings.xml");
        configFile.getParentFile().mkdirs();
        if (configFile.exists()) {
            try {
                Config.load(configFile);
            } catch (Exception e) {
                //if exception appears (outdated xml version etc.), remove the file and try again
                configFile.delete();
                if (calls < 1) initConfig(1);//prevent endless loop
                e.printStackTrace();
            }
        } else {
            Config.getInstance().init();
            Config.getInstance().setConfigurationFile(configFile.getAbsolutePath());
            try {
                Config.getInstance().store();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        initSavedFilterQueues();

    }

    public static void main(String[] args) {
        if (DEBUG) {
            FilamentSensor.DEBUG = false;//too much output
            FilamentSensor.VERBOSE = false;//too much output
            FilamentSensor.ERROR = false;
            FilamentSensor.PERFORMANCE = true;
            FilamentSensor.setDebugStream(System.out::println);
            FilamentSensor.setMessageStream(System.out::println);
            FilamentSensor.setErrorStream(System.err::println);
        }
        try {
            File currDir = new File(".");//get project location for test data
            System.out.println(currDir.getAbsolutePath());
            String path = currDir.getAbsolutePath();
            path = path.substring(0, path.length() - 1);
            testDirectoryRootPath = path + File.separator + "test" + File.separator;
            System.out.println(testDirectoryRootPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initConfig(0);
        Settings dp = new Settings();
        testGUI(args);

    }



    public static void debugPerformance(String text, long timeStart) {
        if (DEBUG)
            System.out.println(text + ":" + (((double) System.currentTimeMillis() - timeStart) / 1000) + "s");
    }


    /**
     * https://stackoverflow.com/questions/8567905/how-to-compare-images-for-similarity-using-java/
     *
     * @param fileA
     * @param fileB
     * @return
     */
    public static float compareImage(File fileA, File fileB) {

        float percentage = 0;
        try {
            // take buffer data from both image files //
            BufferedImage biA = ImageIO.read(fileA);
            DataBuffer dbA = biA.getData().getDataBuffer();
            int sizeA = dbA.getSize();
            BufferedImage biB = ImageIO.read(fileB);
            DataBuffer dbB = biB.getData().getDataBuffer();
            int sizeB = dbB.getSize();
            int count = 0;
            // compare data-buffer objects //
            if (sizeA == sizeB) {
                for (int i = 0; i < sizeA; i++) {
                    if (dbA.getElem(i) == dbB.getElem(i)) {
                        count++;
                    }
                }
                percentage = (count * 100) / sizeA;
            } else {
                System.out.println("Both the images are not of same size");
            }

        } catch (Exception e) {
            System.out.println("Failed to compare image files ...");
        }
        return percentage;
    }


    public static void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }


    private void setupDirectoryStructure() {
        //create sub-directories in directory of executing file for filter-plugins, area-plugins, filter-lists
        File currDir = new File(".");//get project location for test data

        directoryPluginFilters = new File(currDir.getParent() + File.separator + DIR_PLUGIN_FILTER);
        directoryPluginArea = new File(currDir.getParent() + File.separator + DIR_PLUGIN_AREA);
        directoryFilterLists = new File(currDir.getParent() + File.separator + DIR_FILTER_LIST);
        if (!directoryPluginFilters.exists()) directoryPluginFilters.mkdir();
        if (!directoryPluginArea.exists()) directoryPluginArea.mkdir();
        if (!directoryFilterLists.exists()) directoryFilterLists.mkdir();
    }


    private void initializeFilters() {
        //load all filter-lists stored


    }

    private void initializePlugins() {
        //load all plugins
        if (directoryPluginArea != null) {
            try {
                addPath(directoryPluginArea);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (directoryPluginFilters != null) {
            try {
                addPath(directoryPluginFilters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    private void addPath(File f) throws Exception {
        URI u = f.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{u});
    }


}
