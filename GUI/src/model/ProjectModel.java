package model;

import javafx.beans.property.*;

public class ProjectModel {

    //imagefile,projectfile,datafiles,imageoutput,csvoutput,rootFolderOfImages
    //csv-foldername,image-foldername,xml-foldername
    //currentprogress

    private StringProperty imageDirectory;
    private StringProperty projectFile;
    private StringProperty dataDirectory;
    private StringProperty imageOutputDirectory;
    private StringProperty csvOutputDirectory;
    private StringProperty batchProcessingRootDirectory;
    private StringProperty batchProcessingCsvDirectoryName;
    private StringProperty batchProcessingImageDirectoryName;
    private StringProperty batchProcessingXmlDirectoryName;
    private DoubleProperty batchProcessingCurrentProgress;

    private BooleanProperty batchProcessingDoSingleFilamentTracking;


    private StringProperty fileFocalAdhesions;
    private StringProperty fileStressFibers;


    public ProjectModel() {
        imageDirectory = new SimpleStringProperty();
        projectFile = new SimpleStringProperty();
        dataDirectory = new SimpleStringProperty();
        imageOutputDirectory = new SimpleStringProperty();
        csvOutputDirectory = new SimpleStringProperty();
        batchProcessingRootDirectory = new SimpleStringProperty();
        batchProcessingCsvDirectoryName = new SimpleStringProperty("csv");
        batchProcessingImageDirectoryName = new SimpleStringProperty("img");
        batchProcessingXmlDirectoryName = new SimpleStringProperty("xml");
        batchProcessingCurrentProgress = new SimpleDoubleProperty(0);
        fileFocalAdhesions = new SimpleStringProperty();
        fileStressFibers = new SimpleStringProperty();
        batchProcessingDoSingleFilamentTracking = new SimpleBooleanProperty();
    }

    public boolean isBatchProcessingDoSingleFilamentTracking() {
        return batchProcessingDoSingleFilamentTracking.get();
    }

    public BooleanProperty batchProcessingDoSingleFilamentTrackingProperty() {
        return batchProcessingDoSingleFilamentTracking;
    }

    public void setBatchProcessingDoSingleFilamentTracking(boolean batchProcessingDoSingleFilamentTracking) {
        this.batchProcessingDoSingleFilamentTracking.set(batchProcessingDoSingleFilamentTracking);
    }

    public String getImageDirectory() {
        return imageDirectory.get();
    }

    public StringProperty imageDirectoryProperty() {
        return imageDirectory;
    }

    public void setImageDirectory(String imageDirectory) {
        this.imageDirectory.set(imageDirectory);
    }

    public String getProjectFile() {
        return projectFile.get();
    }

    public StringProperty projectFileProperty() {
        return projectFile;
    }

    public void setProjectFile(String projectFile) {
        this.projectFile.set(projectFile);
    }

    public String getDataDirectory() {
        return dataDirectory.get();
    }

    public StringProperty dataDirectoryProperty() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory.set(dataDirectory);
    }

    public String getImageOutputDirectory() {
        return imageOutputDirectory.get();
    }

    public StringProperty imageOutputDirectoryProperty() {
        return imageOutputDirectory;
    }

    public void setImageOutputDirectory(String imageOutputDirectory) {
        this.imageOutputDirectory.set(imageOutputDirectory);
    }

    public String getCsvOutputDirectory() {
        return csvOutputDirectory.get();
    }

    public StringProperty csvOutputDirectoryProperty() {
        return csvOutputDirectory;
    }

    public void setCsvOutputDirectory(String csvOutputDirectory) {
        this.csvOutputDirectory.set(csvOutputDirectory);
    }

    public String getBatchProcessingRootDirectory() {
        return batchProcessingRootDirectory.get();
    }

    public StringProperty batchProcessingRootDirectoryProperty() {
        return batchProcessingRootDirectory;
    }

    public void setBatchProcessingRootDirectory(String batchProcessingRootDirectory) {
        this.batchProcessingRootDirectory.set(batchProcessingRootDirectory);
    }

    public String getBatchProcessingCsvDirectoryName() {
        return batchProcessingCsvDirectoryName.get();
    }

    public StringProperty batchProcessingCsvDirectoryNameProperty() {
        return batchProcessingCsvDirectoryName;
    }

    public void setBatchProcessingCsvDirectoryName(String batchProcessingCsvDirectoryName) {
        this.batchProcessingCsvDirectoryName.set(batchProcessingCsvDirectoryName);
    }

    public String getBatchProcessingImageDirectoryName() {
        return batchProcessingImageDirectoryName.get();
    }

    public StringProperty batchProcessingImageDirectoryNameProperty() {
        return batchProcessingImageDirectoryName;
    }

    public void setBatchProcessingImageDirectoryName(String batchProcessingImageDirectoryName) {
        this.batchProcessingImageDirectoryName.set(batchProcessingImageDirectoryName);
    }

    public String getBatchProcessingXmlDirectoryName() {
        return batchProcessingXmlDirectoryName.get();
    }

    public StringProperty batchProcessingXmlDirectoryNameProperty() {
        return batchProcessingXmlDirectoryName;
    }

    public void setBatchProcessingXmlDirectoryName(String batchProcessingXmlDirectoryName) {
        this.batchProcessingXmlDirectoryName.set(batchProcessingXmlDirectoryName);
    }

    public double getBatchProcessingCurrentProgress() {
        return batchProcessingCurrentProgress.get();
    }

    public DoubleProperty batchProcessingCurrentProgressProperty() {
        return batchProcessingCurrentProgress;
    }

    public void setBatchProcessingCurrentProgress(double batchProcessingCurrentProgress) {
        this.batchProcessingCurrentProgress.set(batchProcessingCurrentProgress);
    }

    public String getFileFocalAdhesions() {
        return fileFocalAdhesions.get();
    }

    public StringProperty fileFocalAdhesionsProperty() {
        return fileFocalAdhesions;
    }

    public void setFileFocalAdhesions(String fileFocalAdhesions) {
        this.fileFocalAdhesions.set(fileFocalAdhesions);
    }

    public String getFileStressFibers() {
        return fileStressFibers.get();
    }

    public StringProperty fileStressFibersProperty() {
        return fileStressFibers;
    }

    public void setFileStressFibers(String fileStressFibers) {
        this.fileStressFibers.set(fileStressFibers);
    }
}
