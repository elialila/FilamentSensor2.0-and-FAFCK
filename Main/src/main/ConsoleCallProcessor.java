package main;

import core.BatchProcessor;
import core.FilterQueue;
import core.ProjectData;
import core.cell.CellPlugin;
import core.cell.plugins.CellPluginBenjamin;
import core.settings.Settings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.IOUtils;
import util.PathScanner;
import util.ProcessingUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

//keys:
//-cmd
//possible values: batch
//-config:
//path of the config file
public class ConsoleCallProcessor {

    public static final String keyCmd = "-cmd";//values: batch
    public static final String keySettings = "-settings";
    public static final String keyPreprocessingFilterQueue = "-pre";
    public static final String keyArea = "-area";//possible values: core.cell.plugins.CellPluginBenjamin, core.cell.plugins.CellPluginSimple
    public static final String keyRoot = "-root";//root directory for file scan

    public static final String cmdBatch = "batch";


    private final String[] args;


    public static final String imgDirectoryName = "img";//output images directory name
    public static final String xmlDirectoryName = "xml";//output xml directory name
    public static final String csvDirectoryName = "csv";//output csv directory name


    public ConsoleCallProcessor(String[] args) {
        this.args = args;
    }

    private Map<String, String> getParameterMap() {

        //split the parameters into pairs
        //the key always starts with -keyName
        //then there is a space followed by the parameter
        Map<String, String> params = new HashMap<>();
        StringProperty property = new SimpleStringProperty();
        Arrays.stream(args).forEach(s -> {
            if (s.startsWith("-")) {//it's the key
                property.set(s.toLowerCase());//use lower-case to make sure it is written in small letters
            } else {//it's the parameter
                if (property.getValue() != null && !property.getValue().isEmpty()) {
                    params.put(property.getValue(), s);
                    property.set(null);
                }
            }
        });

        return params;
    }


    private CellPlugin getSelectedPlugin(Map<String, String> params) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (params.get(keyArea) != null && !params.get(keyArea).isEmpty()) {
            Class<?> aClass = Class.forName(params.get(keyArea));
            Constructor<?> constructor = aClass.getConstructor();
            Object o = constructor.newInstance();

            if (o instanceof CellPlugin) {
                return (CellPlugin) o;
            }
        }
        //by default use the Benjamin-based-CellPlugin
        return new CellPluginBenjamin();
    }

    private void runBatch(Map<String, String> params) {
        System.out.println("Running Batch-Processing");
        params.forEach((key, value) -> System.out.println(key + " " + value));

        if(params.get(keyRoot)==null||params.get(keyRoot).isEmpty()){
            System.out.println("No root directory defined for Batch-Processing -> use "+keyRoot+" \"some/directory/path\"");

            return;
        }


        try {
            File chkRoot = new File(params.get(keyRoot));
            if (!chkRoot.exists()) throw new Exception("Exception in BatchProcessing, root directory does not exist");

            long time = System.currentTimeMillis();

            List<File> directories = PathScanner.scanPath(chkRoot, PathScanner.supportedImageExtensions);

            //print the amount of directories to process
            System.out.println("Directories to Process:" + directories.size());

            for (int i = 0; i < directories.size(); i++) {//no need for parallel work (inside batchprocessing multithreading is used)
                File file = directories.get(i);

                File chkImagePath = new File(file.getAbsolutePath() + File.separator + imgDirectoryName);
                File chkDataPath = new File(file.getAbsolutePath() + File.separator + xmlDirectoryName);
                File chkCsvPath = new File(file.getAbsolutePath() + File.separator + csvDirectoryName);

                if ((chkImagePath.exists() || chkImagePath.mkdir()) &&
                        (chkDataPath.exists() || chkDataPath.mkdir()) &&
                        (chkCsvPath.exists() || chkCsvPath.mkdir())
                ) {
                    FilterQueue preQueue = null;
                    File pre = null;
                    Settings settings = null;

                    //parse filter queue if it is set and exists
                    if (params.get(keyPreprocessingFilterQueue) != null && !params.get(keyPreprocessingFilterQueue).isEmpty()) {
                        //load filter queue and set it
                        System.out.println("Pre-Processing-Queue File specified.");
                        pre = new File(params.get(keyPreprocessingFilterQueue));
                        if (pre.exists()) {
                            preQueue = IOUtils.loadXML(pre);
                            System.out.println("Pre-Processing-Queue loaded.");
                        }
                    }
                    //parse settings if it is set and exists
                    if (params.get(keySettings) != null && !params.get(keySettings).isEmpty()) {
                        System.out.println("Settings File specified.");
                        File fileSettings = new File(params.get(keySettings));
                        if (fileSettings.exists()) {
                            settings=Settings.load(fileSettings);
                            System.out.println("Settings loaded.");
                        }
                    }
                    if (settings == null) settings = new Settings();

                    ProjectData projectData = new ProjectData();
                    projectData.setRootDir(chkRoot);
                    if (pre != null) projectData.setFilterQueueFile(pre);
                    projectData.setImageOutputPath(chkImagePath);
                    projectData.setImagePath(file);
                    projectData.setXmlPath(chkDataPath);
                    projectData.setCsvPath(chkCsvPath);
                    projectData.setSettings(settings);

                    CellPlugin plugin = getSelectedPlugin(params);




                    projectData.setPlugin(plugin);
                    if (projectData.getPlugin() == null)
                        projectData.setPlugin(new CellPluginBenjamin());
                    projectData.initLists();
                    BatchProcessor processor = new BatchProcessor();

                    processor.batchProcess(projectData,
                            (preQueue != null) ? preQueue : ProcessingUtils.getDefaultPreprocessingFilterQueue(projectData.getSettings()),
                            ProcessingUtils.getDefaultLineSensorQueue(projectData.getSettings(), false)
                    );

                    projectData.initLists();
                    Date date = Calendar.getInstance().getTime();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String strDate = dateFormat.format(date);
                    projectData.store(new File(projectData.getXmlPath().getAbsolutePath() + File.separator + strDate + "_Project.xml"));

                }
                System.out.println("Directory finished(" + (i + 1) + "/" + directories.size() + ")");
            }


            System.out.println("Finished Processing.");
            System.out.println("Time taken:" + (System.currentTimeMillis() - time) / 1000 + "s");


        } catch (Exception e) {
          e.printStackTrace();
        }


    }

    public void run() {
        //todo: console calls
        // - batch processing
        Map<String, String> params = getParameterMap();
        if (cmdBatch.equals(params.get(keyCmd))) {
            runBatch(params);
        } else {
            throw new RuntimeException("Unknown Command for FilamentSensor Console-Call");
        }


    }


}
