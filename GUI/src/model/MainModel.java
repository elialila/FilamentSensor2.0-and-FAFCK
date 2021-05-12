package model;


import filters.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import core.FilterQueue;
import core.ProjectData;
import util.PathScanner;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainModel {

    private ObjectProperty<ProjectData> projectData;
    private ObjectProperty<ImageDetailStackModel> stackModel;
    private ObjectProperty<ProjectModel> projectModel;
    private ObjectProperty<FilterQueue> filterQueueModel;
    private ObjectProperty<LineSensorModel> lineSensorModel;

    private ObservableList<String> clsApplicableFilters;

    private PreferencesBatchProcessingModel preferencesBatchProcessing;

    public MainModel() {
        preferencesBatchProcessing = new PreferencesBatchProcessingModel();
        stackModel = new SimpleObjectProperty<>();
        projectModel = new SimpleObjectProperty<>(new ProjectModel());
        projectData = new SimpleObjectProperty<>(new ProjectData());
        filterQueueModel = new SimpleObjectProperty<>();
        lineSensorModel = new SimpleObjectProperty<>();
        clsApplicableFilters = FXCollections.observableArrayList();
        clsApplicableFilters.add(FilterGauss.class.getName());
        clsApplicableFilters.add(FilterLaPlace.class.getName());
        clsApplicableFilters.add(FilterLineGauss.class.getName());
        clsApplicableFilters.add(FilterAreaMask.class.getName());
        clsApplicableFilters.add(FilterEnhanceContrast.class.getName());
        clsApplicableFilters.add(FilterCrossCorrelation.class.getName());


    }


    public void initializeLoadProject() throws Exception {
        File projectFile = null;
        if (getProjectModel().getProjectFile() != null) {
            projectFile = new File(getProjectModel().getProjectFile());
            if (!projectFile.exists()) projectFile = null;
        }
        if (projectFile == null) {
            throw new Exception("Project File doesn't exist");
        }

        setProjectData(ProjectData.load(projectFile));

        if (getProjectData().getImagePath() != null) {
            getProjectModel().setImageDirectory(getProjectData().getImagePath().getAbsolutePath());
        }
        if (getProjectData().getXmlPath() != null) {
            getProjectModel().setDataDirectory(getProjectData().getXmlPath().getAbsolutePath());
        }
        getProjectData().setInitialized(true);

    }


    public void initializeProjectData() throws Exception {

        File directory = new File(getProjectModel().getImageDirectory());
        if (!directory.isDirectory() && !directory.getName().contains(PathScanner.OME_TIF)) {
            throw new Exception("Image \"Directory\" not a directory or a stack-file(.ome.tif)");
        }

        File projectFile = null;
        File dataDirectory = null;


        if (getProjectModel().getProjectFile() != null) {
            projectFile = new File(getProjectModel().getProjectFile());
            if (!projectFile.exists()) projectFile = null;
        }
        if (getProjectModel().getDataDirectory() != null) {
            dataDirectory = new File(getProjectModel().getDataDirectory());
            if (!dataDirectory.exists() || !dataDirectory.isDirectory()) dataDirectory = null;
        }

        List<File> files = new ArrayList<>();
        files.add(directory);
        if (projectFile != null) files.add(projectFile);
        if (dataDirectory != null) files.add(dataDirectory);
        File[] params = new File[files.size()];
        params = files.toArray(params);
        setProjectData(new ProjectData());
        getProjectData().setRootDir(PathScanner.getCommonParent(params));
        if (projectFile != null) getProjectData().setProjectFile(projectFile);
        if (dataDirectory != null) getProjectData().setXmlPath(dataDirectory);

        if (directory.getName().contains(PathScanner.OME_TIF)) {
            getProjectData().setImagePath(directory.getParentFile());
            getProjectData().setImageFiles(Collections.singletonList(directory));
            getProjectData().setInitialized(true);
        } else {
            getProjectData().setImagePath(directory);
            getProjectData().initLists();
            getProjectData().setInitialized(true);
        }
        //set root dir and other params

    }

    public ObservableList<String> getClsApplicableFilters() {
        return clsApplicableFilters;
    }

    public LineSensorModel getLineSensorModel() {
        return lineSensorModel.get();
    }

    public ObjectProperty<LineSensorModel> lineSensorModelProperty() {
        return lineSensorModel;
    }

    public void setLineSensorModel(LineSensorModel lineSensorModel) {
        this.lineSensorModel.set(lineSensorModel);
    }

    public FilterQueue getFilterQueueModel() {
        return filterQueueModel.get();
    }

    public ObjectProperty<FilterQueue> filterQueueModelProperty() {
        return filterQueueModel;
    }

    public void setFilterQueueModel(FilterQueue filterQueueModel) {
        this.filterQueueModel.set(filterQueueModel);
    }

    public ImageDetailStackModel getStackModel() {
        return stackModel.get();
    }

    public ObjectProperty<ImageDetailStackModel> stackModelProperty() {
        return stackModel;
    }

    public void setStackModel(ImageDetailStackModel stackModel) {
        this.stackModel.set(stackModel);
    }

    public ProjectData getProjectData() {
        return projectData.get();
    }

    public ObjectProperty<ProjectData> projectDataProperty() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData.set(projectData);
    }

    public PreferencesBatchProcessingModel getPreferencesBatchProcessing() {
        return preferencesBatchProcessing;
    }

    public ProjectModel getProjectModel() {
        return projectModel.get();
    }

    public ObjectProperty<ProjectModel> projectModelProperty() {
        return projectModel;
    }

    public void setProjectModel(ProjectModel projectModel) {
        this.projectModel.set(projectModel);
    }

}
