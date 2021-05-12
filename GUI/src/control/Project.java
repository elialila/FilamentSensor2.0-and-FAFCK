package control;

import core.OFIllustratorBatch;
import core.settings.Batch;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import model.ImageDetailStackModel;
import model.ProjectModel;
import core.BatchProcessor;
import core.FilterQueue;
import util.ProcessingUtils;
import core.ProjectData;
import core.cell.CellPlugin;
import core.cell.plugins.CellPluginBenjamin;
import core.settings.Bin;
import core.settings.Settings;
import util.IOUtils;
import util.PathScanner;


import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static utils.UIUtils.handleDirectoryChooser;

public class Project extends AbstractControl {


    private ProjectModel model;

    @FXML
    private TextField tSettings;

    @FXML
    private ProgressBar pbProgressBatchProcessing;

    @FXML
    private CheckBox chkDoSingleFilamentTracking;


    @FXML
    private TextField tImageDirectory;

    @FXML
    private TextField tBatchProcessingFilterPre;

    @FXML
    private TextField tDataDirectory;

    @FXML
    private TextField tProjectFile;

    @FXML
    private TextField tProjectFile2;

    @FXML
    private TextField tImageOutputDirectory;

    @FXML
    private TextField tCsvOutputDirectory;

    @FXML
    private TextField tBatchProcessingRootDirectory;

    @FXML
    private TextField tBatchProcessingCsvFolderName;

    @FXML
    private TextField tBatchProcessingXmlFolderName;

    @FXML
    private TextField tBatchProcessingImgFolderName;

    @FXML
    private Button btnLoad;

    @FXML
    private Button btnIlluminate;

    @FXML
    private Button btnBatchProcessing;

    @FXML
    private Button btnStore;

    @FXML
    private Button btnLoadProject;

    private BooleanProperty readyProperty;

    @FXML
    private ComboBox<CellPlugin> cbArea;


    public Project() {
        model = new ProjectModel();
    }




    @FXML
    private void handleChooseFilterPre(ActionEvent event) {
        handleDirectoryChooser("choose pre processing filter list(.xml file)", tBatchProcessingFilterPre, event, false);
    }

    @FXML
    private void handleChooseBatchProcessingRoot(ActionEvent event) {
        handleDirectoryChooser("choose the root path to the images", tBatchProcessingRootDirectory, event, true);
    }
    @FXML
    private void handleChooseImagePath(ActionEvent event) {
        handleDirectoryChooser("choose the path to the images",tImageDirectory,event,true);
    }
    @FXML
    private void handleChooseDataPath(ActionEvent event) {
        handleDirectoryChooser("choose where data should be stored",tDataDirectory,event,true);
    }
    @FXML
    private void handleChooseProjectFile(ActionEvent event) {
        handleDirectoryChooser("choose where the project-file should be stored",tProjectFile,event,false);
    }

    @FXML
    private void handleChooseImageOutput(ActionEvent event) {
        handleDirectoryChooser("choose where the output images should be stored", tImageOutputDirectory, event, true);
    }

    @FXML
    private void handleChooseCSVOutput(ActionEvent event) {
        handleDirectoryChooser("choose where the output csv should be stored", tCsvOutputDirectory, event, true);
    }

    @FXML
    private void handleChooseSettings(ActionEvent actionEvent) {
        handleDirectoryChooser("choose which settings should be imported", tSettings, actionEvent, false);
    }


    @FXML
    private void handleStartLoading(ActionEvent event) {
        if (model.getImageDirectory() == null || model.getImageDirectory().isEmpty()) {// || tDataDirectory.getText().length() == 0 || tProjectFile.getText().length() == 0) {
            getMainController().addDebugMessage("Image Path is empty");
            return;
        }
        //start importing
        File fImages = new File(model.getImageDirectory());
        if (!fImages.exists()) {
            getMainController().addDebugMessage("Image folder doesn't exist");
            return;
        }
        if (!fImages.isDirectory() && !fImages.getName().contains(PathScanner.OME_TIF)) {
            getMainController().addDebugMessage("Image folder is not a directory or a stack-file(.ome.tif)");
            return;
        }


        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Initialize Project");
        alert.setContentText("Would you like to load the standard thumbnail view or" +
                " the stack view(!!in development!!)(needs a machine with at least 8GB RAM" +
                " (depending on the number of images), recommended >=16GB RAM)" +
                " filaments currently not displayed in stack view."
        );
        ButtonType okButton = new ButtonType("Thumbnail View", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Stack View", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, noButton, cancelButton);
        final Project self = this;
        alert.showAndWait().ifPresent(type -> {
            if (type == cancelButton) {
                getMainController().addDebugMessage("Action canceled");
                return;
            }else {
                try {
                    readyProperty.setValue(false);
                    getMainController().getModel().initializeProjectData();
                    Task task = null;
                    if (type == okButton) {
                        task = new Task<Void>() {
                            @Override
                            public Void call() {
                                try {
                                    updateProgress(50, 100);
                                    getMainController().getModel().setStackModel(new ImageDetailStackModel());
                                    MainControl.<ImageOverview>addDynamicContent(getMainController(), self, "/view/ImageOverview.fxml", (t) -> t.setImageData(getMainController().getModel().getProjectData()));
                                    updateProgress(100, 100);
                                } catch (Exception e) {
                                    getMainController().addDebugMessage(e);
                                }

                                return null;
                            }
                        };

                    } else if (type == noButton) {
                        task = new Task<Void>() {
                            @Override
                            public Void call() {
                                try {
                                    readyProperty.setValue(false);
                                    updateProgress(0, 100);
                                    getMainController().getModel().setStackModel(new ImageDetailStackModel());
                                    updateProgress(50, 100);
                                    MainControl.<ImageDetailStack>addDynamicContent(getMainController(), self, "/view/ImageDetailStack.fxml", (ids) -> {
                                        try {
                                            ids.setImages(getMainController().getModel().getProjectData().getImageFiles());
                                        } catch (Exception e) {
                                            getMainController().addDebugMessage(e);
                                            e.printStackTrace();
                                        }


                                    });
                                    updateProgress(100, 100);

                                } catch (Exception e) {
                                    getMainController().addDebugMessage(e);
                                }
                                readyProperty.setValue(true);
                                return null;
                            }
                        };
                    }
                    getMainController().runAsync(task);

                } catch (Exception e) {
                    getMainController().addDebugMessage(e);
                    e.printStackTrace();
                }
                readyProperty.setValue(true);
            }
        });
    }

    @FXML
    private void handleIlluminateAll(ActionEvent event) {
        if (getMainController().getModel() == null || getMainController().getModel().getProjectModel() == null ||
                getMainController().getModel().getProjectModel().getProjectFile() == null || getMainController().getModel().getProjectModel().getCsvOutputDirectory() == null ||
                getMainController().getModel().getProjectModel().getImageOutputDirectory() == null ||
                getMainController().getModel().getProjectModel().getDataDirectory() == null) {
            getMainController().addDebugMessage("Data not completely typed in");
            return;
        }


        File chkProject = new File(getMainController().getModel().getProjectModel().getProjectFile());
        if (chkProject.exists() && chkProject.getName().contains(".xml")) {
            handleLoadProject(null);
        }

        File chkData = new File(getMainController().getModel().getProjectModel().getDataDirectory());
        if (chkData.exists() && tImageOutputDirectory.getText() != null && !tImageOutputDirectory.getText().equals("") &&
                getMainController().getModel().getProjectData() != null) {
            long dataCount = Arrays.stream(Objects.requireNonNull(chkData.listFiles())).filter(f -> f.getName().contains(".xml")).count();
            int length = getMainController().getModel().getProjectData().getImageFiles().size();
            if (dataCount < length) {
                getMainController().addDebugMessage("Check the XML-Files some are missing");
                return;
            }
        }

        if (getMainController().getModel() == null || getMainController().getModel().getProjectData() == null) {
            getMainController().addDebugMessage("Some Exception appeared, MainModel/ProjectData ==null");
            return;
        }
        try {

            File chkImageOutput = new File(model.getImageOutputDirectory());
            if (!chkImageOutput.exists()) {
                chkImageOutput.mkdirs();
            }

            if (!chkImageOutput.exists() || !chkImageOutput.isDirectory()) {
                throw new Exception("Exception in Illuminate All, Image Output Directory missing or not a directory");
            }
            getMainController().addDebugMessage("Start Illumination ...");
            Task task = new Task<Void>() {
                @Override
                public Void call() {
                    try {
                        readyProperty.setValue(false);
                        updateProgress(0, 100);
                        OFIllustratorBatch processor = new OFIllustratorBatch();
                        processor.projectIllustrations(getMainController().getModel().getProjectData(), chkImageOutput, true);
                        updateProgress(100, 100);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                    }
                    readyProperty.setValue(true);
                    getMainController().addDebugMessage("Finished Illumination.");
                    return null;
                }
            };
            getMainController().runAsync(task);

        } catch (Exception e) {
            getMainController().addDebugMessage(e);
            e.printStackTrace();
        }
    }


    @FXML
    private void handleBatchProcessing(ActionEvent event) {
        //iterate root directory recursive and build up a list of folders with images inside
        //add the xml,csv,img folder to those folders and batchprocess them sequentially
        //log the overall progress and current progress

        try {
            File chkRoot = new File(model.getBatchProcessingRootDirectory());
            if (!chkRoot.exists()) throw new Exception("Exception in BatchProcessing, root directory is not existent");

            Task task = new Task<Void>() {
                @Override
                public Void call() {
                    long time = System.currentTimeMillis();
                    try {
                        List<File> directories = PathScanner.scanPath(chkRoot, PathScanner.supportedImageExtensions);
                        readyProperty.setValue(false);
                        updateProgress(0, directories.size());
                        for (int i = 0; i < directories.size(); i++) {//no need for parallel work (inside batchprocessing multithreading is used)
                            File file = directories.get(i);

                            File chkImagePath = new File(file.getAbsolutePath() + File.separator + model.getBatchProcessingImageDirectoryName());
                            File chkDataPath = new File(file.getAbsolutePath() + File.separator + model.getBatchProcessingXmlDirectoryName());
                            File chkCsvPath = new File(file.getAbsolutePath() + File.separator + model.getBatchProcessingCsvDirectoryName());
                            model.setBatchProcessingCurrentProgress(0);
                            if ((chkImagePath.exists() || chkImagePath.mkdir()) &&
                                    (chkDataPath.exists() || chkDataPath.mkdir()) &&
                                    (chkCsvPath.exists() || chkCsvPath.mkdir())
                            ) {
                                FilterQueue preQueue = null;
                                File pre = null;
                                if (tBatchProcessingFilterPre.getText() != null && !tBatchProcessingFilterPre.getText().isEmpty()) {
                                    //load filter queue and set it
                                    pre = new File(tBatchProcessingFilterPre.getText());
                                    if (pre.exists()) {
                                        preQueue = IOUtils.loadXML(pre);
                                    }
                                }


                                ProjectData projectData = new ProjectData();
                                projectData.setRootDir(chkRoot);
                                if (pre != null) projectData.setFilterQueueFile(pre);
                                projectData.setImageOutputPath(chkImagePath);
                                projectData.setImagePath(file);
                                projectData.setXmlPath(chkDataPath);
                                projectData.setCsvPath(chkCsvPath);
                                projectData.setSettings(getMainController().getModel().getProjectData().getSettings());

                                projectData.getSettings().getPropertyAsBoolean(Batch.doSingleFilamentTracking).set(model.isBatchProcessingDoSingleFilamentTracking());


                                if (!tSettings.getText().isEmpty()) {
                                    Settings settings = Settings.load(new File(tSettings.getText()));
                                    projectData.setSettings(settings);

                                    System.out.println("Project::handleBatchProcessing() --- settings imported");
                                    System.out.println("Project::handleBatchProcessing() --- Bin.restrict,Bin.isAreaExt" + settings.getValueAsBoolean(Bin.restrict) + "," + settings.getValueAsBoolean(Bin.is_area_or_ext));


                                }


                                CellPlugin plugin = cbArea.getSelectionModel().getSelectedItem();
                                projectData.setPlugin(plugin);
                                if (projectData.getPlugin() == null)
                                    projectData.setPlugin(new CellPluginBenjamin());
                                projectData.initLists();
                                BatchProcessor processor = new BatchProcessor();
                                model.batchProcessingCurrentProgressProperty().set(0);
                                //pbProgressBatchProcessing.progressProperty().set(0);
                                processor.batchProcess(projectData,
                                        (preQueue != null) ? preQueue : ProcessingUtils.getDefaultPreprocessingFilterQueue(projectData.getSettings()),
                                        ProcessingUtils.getDefaultLineSensorQueue(projectData.getSettings(), false)
                                );

                                projectData.initLists();
                                Date date = Calendar.getInstance().getTime();
                                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                String strDate = dateFormat.format(date);
                                projectData.store(new File(projectData.getXmlPath().getAbsolutePath() + File.separator + strDate + "_Project.xml"));
                                model.batchProcessingCurrentProgressProperty().set(100);
                            }
                            updateProgress(i + 1, directories.size());
                        }
                        updateProgress(directories.size(), directories.size());

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                    }
                    readyProperty.setValue(true);
                    getMainController().addDebugMessage("Finished Processing.");
                    getMainController().addDebugMessage("Time taken:" + (System.currentTimeMillis() - time) / 1000 + "s");
                    return null;
                }
            };
            getMainController().runAsync(task);


        }catch(Exception e){
            getMainController().addDebugMessage(e);
            return;
        }

    }


    @FXML
    private void handleLoadProject(ActionEvent event) {
        if (getMainController().getModel().getProjectModel().getProjectFile() != null) {
            try {
                getMainController().getModel().initializeLoadProject();
            } catch (Exception e) {
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }
        }


    }


    @FXML
    private void handleStoreProject(ActionEvent event) {
        if (getMainController().getModel() == null) {//|| getMainController().getModel().getProjectData() == null) {
            getMainController().addDebugMessage("Project not initialized yet. Press start loading");
            return;
        }

        if (model.getProjectFile() == null ||model.getProjectFile().isEmpty()) {
            getMainController().addDebugMessage("Project-File not set");
            return;
        }

        File chkProjectFile = new File(model.getProjectFile());
        boolean result = true;
        if (!chkProjectFile.getParentFile().exists()) {
            result = chkProjectFile.getParentFile().mkdirs();
            try {
                chkProjectFile.createNewFile();

            } catch (IOException e) {
                result = false;
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }
        }
        if (result) {
            if (!getMainController().getModel().getProjectData().isInitialized()) {
                try {
                    getMainController().getModel().initializeProjectData();
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                    getMainController().addDebugMessage("Project not saved");
                    return;
                }
            }
            getMainController().getModel().getProjectData().setProjectFile(chkProjectFile);
            try {
                //in case there is already a stack model, store the filter-queue settings
                if (getMainController().getModel().getStackModel() != null &&
                        getMainController().getModel().getStackModel().getPreProcessing() != null) {
                    File outputFile = new File(
                            chkProjectFile.getParentFile().getAbsolutePath() +
                                    File.separator + "filters_" +
                                    getMainController().getModel().getStackModel().getPreProcessing().hashCode() +
                                    ".xml"
                    );
                    try {
                        //write XML Filter-Queue
                        XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
                        encoder.writeObject(getMainController().getModel().getStackModel().getPreProcessing());
                        encoder.flush();
                        encoder.close();
                        getMainController().getModel().getProjectData().setFilterQueueFile(outputFile);
                    } catch (FileNotFoundException e) {
                        getMainController().addDebugMessage(e);
                        e.printStackTrace();
                    }

                }
                getMainController().getModel().getProjectData().store(chkProjectFile);
                getMainController().addDebugMessage("Project File saved");
            } catch (FileNotFoundException e) {
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }
        } else {
            getMainController().addDebugMessage("Project not saved, something is wrong with the selected file");
        }

    }


    private void initializeBindings() {
        tBatchProcessingRootDirectory.textProperty().bindBidirectional(model.batchProcessingRootDirectoryProperty());
        tBatchProcessingCsvFolderName.textProperty().bindBidirectional(model.batchProcessingCsvDirectoryNameProperty());
        tBatchProcessingImgFolderName.textProperty().bindBidirectional(model.batchProcessingImageDirectoryNameProperty());
        tBatchProcessingXmlFolderName.textProperty().bindBidirectional(model.batchProcessingXmlDirectoryNameProperty());
        chkDoSingleFilamentTracking.selectedProperty().bindBidirectional(model.batchProcessingDoSingleFilamentTrackingProperty());


        tCsvOutputDirectory.textProperty().bindBidirectional(model.csvOutputDirectoryProperty());
        tDataDirectory.textProperty().bindBidirectional(model.dataDirectoryProperty());
        tImageDirectory.textProperty().bindBidirectional(model.imageDirectoryProperty());
        tImageOutputDirectory.textProperty().bindBidirectional(model.imageOutputDirectoryProperty());
        tProjectFile.textProperty().bindBidirectional(model.projectFileProperty());
        tProjectFile2.textProperty().bindBidirectional(model.projectFileProperty());

        pbProgressBatchProcessing.progressProperty().bind(model.batchProcessingCurrentProgressProperty());

        //tFocalAdhesionImagePath.textProperty().bindBidirectional(model.fileFocalAdhesionsProperty());
        //tStressFibersImagePath.textProperty().bindBidirectional(model.fileStressFibersProperty());


    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        if (getMainController().getModel().getProjectModel() != null) {
            model = getMainController().getModel().getProjectModel();
            //tFocalAdhesionImagePath.textProperty().unbind();
            //tStressFibersImagePath.textProperty().unbind();
        } else {
            getMainController().getModel().setProjectModel(model);
        }
        initializeBindings();


        //init area combo box
        PreprocessingFilterQueue.initCbArea(cbArea, (cp) -> getMainController().getModel().getProjectData().setPlugin(cp),
                () -> getMainController().getModel().getProjectData().getPlugin(), getMainController());

        readyProperty = new SimpleBooleanProperty();
        readyProperty.setValue(true);


        //init buttons disable attribute

        if (btnLoadProject != null) {
            btnLoadProject.disableProperty().bind(Bindings.or(readyProperty.not(), model.projectFileProperty().isEmpty()));
        }

        if (btnLoad != null) {
            btnLoad.disableProperty().bind(Bindings.or(model.imageDirectoryProperty().isEmpty(), readyProperty.not()));
        }

        if (btnBatchProcessing != null) {
            btnBatchProcessing.disableProperty().bind(Bindings.or(readyProperty.not(), model.batchProcessingRootDirectoryProperty().isEmpty()));
        }

        if (btnIlluminate != null) {
            //find a way to implement it as property to do a check not only once
            btnIlluminate.disableProperty().bind(readyProperty.not());
        }

        if (btnStore != null) {
            btnStore.disableProperty().bind(readyProperty.not());
        }
    }


    public ProjectModel getModel() {
        return model;
    }


}
