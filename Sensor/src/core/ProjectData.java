package core;

import core.image.ImageWrapper;
import util.Annotations.NotNull;
import core.cell.CellPlugin;
import core.image.Entry;
import core.settings.Settings;
import util.IOUtils;
import util.ImageExporter;
import util.MixedUtils;
import util.PathScanner;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.Transient;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This Project Model should contain path's and files used in the project
 * it also contains the current settings
 * for re-usability on other devices the path's are stored relative to rootDir
 * in order to re use the serialized XML-File the rootDir has to be adjusted.
 */
public class ProjectData implements Serializable {

    //region relative Path's variables
    private String sRootDir;//all path's are relative to the root directory
    private List<String> sImageFiles;//relative paths -> image directory(source) not /img
    private List<String> sImageOutputFiles;//the files in /img directory
    private List<String> sXmlFiles;
    private List<String> sCsvFiles;

    private String sCsvPath;
    private String sProjectFile;
    private String sXmlPath;

    private String sImagePath;
    private String sImageOutputPath;
    private String sFilterQueueFile;

    private boolean initialized;

    private CellPlugin plugin;
    //endregion


    private Settings settings;

    public ProjectData() {
        sImageFiles = new ArrayList<>();
        sXmlFiles = new ArrayList<>();
        sCsvFiles = new ArrayList<>();
        sImageOutputFiles = new ArrayList<>();
        settings = new Settings();//initialises default parameters
        initialized = false;

        plugin = null;

    }

    //region basic Setter-Getter
    public List<String> getSImageOutputFiles() {
        return sImageOutputFiles;
    }

    public void setSImageOutputFiles(List<String> sImageOutputFiles) {
        this.sImageOutputFiles = sImageOutputFiles;
    }

    public List<String> getSCsvFiles() {
        return sCsvFiles;
    }

    public void setSCsvFiles(List<String> sCsvFiles) {
        this.sCsvFiles = sCsvFiles;
    }

    public String getSCsvPath() {
        return sCsvPath;
    }

    public void setSCsvPath(String sCsvPath) {
        this.sCsvPath = sCsvPath;
    }

    public String getSFilterQueueFile() {
        return sFilterQueueFile;
    }

    public void setSFilterQueueFile(String sFilterQueueFile) {
        this.sFilterQueueFile = sFilterQueueFile;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public CellPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(CellPlugin plugin) {
        this.plugin = plugin;
    }

    public String getSXmlPath() {
        return sXmlPath;
    }

    public void setSXmlPath(String sDataPath) {
        this.sXmlPath = sDataPath;
    }

    public String getSImagePath() {
        return sImagePath;
    }

    public void setSImagePath(String sImagePath) {
        this.sImagePath = sImagePath;
    }

    public String getSRootDir() {
        return sRootDir;
    }

    public void setSRootDir(String sRootDir) {
        this.sRootDir = sRootDir;
    }

    public List<String> getSImageFiles() {
        return sImageFiles;
    }

    public void setSImageFiles(List<String> sImageFiles) {
        this.sImageFiles = sImageFiles;
    }

    public List<String> getSXmlFiles() {
        return sXmlFiles;
    }

    public void setSXmlFiles(List<String> sXmlFiles) {
        this.sXmlFiles = sXmlFiles;
    }

    public String getSProjectFile() {
        return sProjectFile;
    }

    public void setSProjectFile(String sProjectFile) {
        this.sProjectFile = sProjectFile;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public String getSImageOutputPath() {
        return sImageOutputPath;
    }

    public void setSImageOutputPath(String sImageOutputPath) {
        this.sImageOutputPath = sImageOutputPath;
    }

    //endregion

    //region virtual Setter-Getter(no basic attributes)
    @Transient
    public List<File> getImageFiles() {
        //create list, change to absolute path's, return list
        List<File> files = new ArrayList<>();
        sImageFiles.forEach(sFile -> {
            files.add(fromRelativePath(sFile));
        });
        return files;
    }

    @Transient
    public void setImageFiles(List<File> imageFiles) {
        List<String> sFiles = new ArrayList<>();
        imageFiles.forEach(file -> {
            sFiles.add(fromAbsolutePath(file));
        });
        this.sImageFiles = sFiles;
    }

    @Transient
    public List<File> getImageOutputFiles() {
        //create list, change to absolute path's, return list
        List<File> files = new ArrayList<>();
        sImageOutputFiles.forEach(sFile -> {
            files.add(fromRelativePath(sFile));
        });
        return files;
    }

    @Transient
    public void setImageOutputFiles(List<File> imageFiles) {
        List<String> sFiles = new ArrayList<>();
        imageFiles.forEach(file -> {
            sFiles.add(fromAbsolutePath(file));
        });
        this.sImageOutputFiles = sFiles;
    }


    @Transient
    public List<File> getXmlFiles() {
        List<File> files = new ArrayList<>();
        sXmlFiles.forEach(sFile -> {
            files.add(fromRelativePath(sFile));
        });
        return files;
    }

    @Transient
    public void setXmlFiles(List<File> xmlFiles) {
        List<String> sFiles = new ArrayList<>();
        xmlFiles.forEach(file -> {
            sFiles.add(fromAbsolutePath(file));
        });
        this.sXmlFiles = sFiles;
    }

    @Transient
    public List<File> getCsvFiles() {
        List<File> files = new ArrayList<>();
        sCsvFiles.forEach(sFile -> {
            files.add(fromRelativePath(sFile));
        });
        return files;
    }

    @Transient
    public void setCsvFiles(List<File> csvFiles) {
        List<String> sFiles = new ArrayList<>();
        csvFiles.forEach(file -> {
            sFiles.add(fromAbsolutePath(file));
        });
        this.sCsvFiles = sFiles;
    }


    @Transient
    public File getFilterQueueFile() {
        return fromRelativePath(sFilterQueueFile);
    }

    @Transient
    public void setFilterQueueFile(File filterQueueFile) {
        this.sFilterQueueFile = fromAbsolutePath(filterQueueFile);
    }

    @Transient
    public File getRootDir() {
        return new File(sRootDir);
    }

    @Transient
    public void setRootDir(File rootDir) {
        this.sRootDir = rootDir.getAbsolutePath();
    }

    @Transient
    public File getProjectFile() {
        return fromRelativePath(getSProjectFile());
    }

    @Transient
    public void setProjectFile(File projectFile) {
        this.sProjectFile = fromAbsolutePath(projectFile);
    }

    @Transient
    public void setXmlPath(File dataPath) {
        this.sXmlPath = fromAbsolutePath(dataPath);
    }

    @Transient
    public File getXmlPath() {
        return fromRelativePath(getSXmlPath());
    }


    @Transient
    public void setImagePath(File imagePath) {
        this.sImagePath = fromAbsolutePath(imagePath);
    }

    @Transient
    public File getImagePath() {
        return fromRelativePath(getSImagePath());
    }

    @Transient
    public void setCsvPath(File imagePath) {
        this.sCsvPath = fromAbsolutePath(imagePath);
    }

    @Transient
    public File getCsvPath() {
        return fromRelativePath(getSCsvPath());
    }

    @Transient
    public void setImageOutputPath(File imagePath) {
        this.sImageOutputPath = fromAbsolutePath(imagePath);
    }

    @Transient
    public File getImageOutputPath() {
        return fromRelativePath(getSImageOutputPath());
    }


    //endregion

    //region util methods

    /**
     * Gives Relative Path from File(relative to rootDir), rootDir has to be set
     *
     * @param file
     * @return
     */
    private String fromAbsolutePath(File file) {
        Objects.requireNonNull(sRootDir, "rootDir is null");
        return Paths.get(sRootDir).relativize(file.toPath()).toString().replace("\\", "/");
    }

    private File fromRelativePath(String sFile) {
        Objects.requireNonNull(sRootDir, "rootDir is null");
        return new File(getSRootDir() + File.separator + sFile);
    }
    //endregion

    //region Serialization
    public void store(@NotNull File location) throws FileNotFoundException {
        Objects.requireNonNull(location, "location is null");
        if (getSettings() != null) {
            settings.preSerialize();
        }
        XMLEncoder encoder = new XMLEncoder(new FileOutputStream(location));
        encoder.writeObject(this);
        encoder.flush();
        encoder.close();
        settings.cleanUp();
    }

    public static ProjectData load(@NotNull File location) throws FileNotFoundException {
        Objects.requireNonNull(location, "location is null");
        if (!location.exists()) throw new IllegalArgumentException("location does not exist");
        try {
            XMLDecoder decoder = new XMLDecoder(new FileInputStream(location));
            Object result = decoder.readObject();
            decoder.close();
            if (result instanceof ProjectData) {
                ((ProjectData) result).getSettings().postSerialize();
                return (ProjectData) result;
            }
            throw new IllegalArgumentException("Wrong Object Type in Serialized File:" + result.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Something is wrong with the provided File");
        }
    }
    //endregion

    /**
     * Initialises the File Lists with all files found in the corresponding directory of the given type
     * dataPath = xml files and imagePath = image files and csvPath = csv files
     */
    public void initLists() {
        Objects.requireNonNull(getSRootDir(), "rootDir is null");
        if (getSImagePath() != null) {
            if (!getImagePath().exists()) throw new IllegalArgumentException("ImagePath does not exist");
            if (!getImagePath().isDirectory()) throw new IllegalArgumentException("ImagePath is not a directory");
            getSImageFiles().clear();
            setImageFiles(PathScanner.getFilesInDirectory(getImagePath(), PathScanner.supportedImageExtensions, PathScanner.filteredNameSuffixes, true));
        }

        if (getSXmlPath() != null) {
            if (!getXmlPath().exists()) throw new IllegalArgumentException("DataPath does not exist");
            if (!getXmlPath().isDirectory()) throw new IllegalArgumentException("DataPath is not a directory");
            getSXmlFiles().clear();
            setXmlFiles(PathScanner.getFilesInDirectory(getXmlPath(), Collections.singletonList(".xml"), PathScanner.filteredNameSuffixes, true));
        }

        if (getSCsvPath() != null) {
            if (!getCsvPath().exists()) throw new IllegalArgumentException("CsvPath does not exist");
            if (!getCsvPath().isDirectory()) throw new IllegalArgumentException("CsvPath is not a directory");
            getSCsvFiles().clear();
            setCsvFiles(PathScanner.getFilesInDirectory(getXmlPath(), Collections.singletonList(".csv"), PathScanner.filteredNameSuffixes, true));
        }

        if (getSImageOutputPath() != null) {
            if (!getImageOutputPath().exists()) throw new IllegalArgumentException("ImageOutputPath does not exist");
            if (!getImageOutputPath().isDirectory())
                throw new IllegalArgumentException("ImageOutputPath is not a directory");
            getSImageOutputFiles().clear();
            setImageOutputFiles(PathScanner.getFilesInDirectory(getImagePath(), PathScanner.supportedImageExtensions, PathScanner.filteredNameSuffixes, true));
        }

    }


    @Override
    public String toString() {
        return "ProjectDataNew{" +
                "sRootDir='" + sRootDir + '\'' +
                ", sImageFiles=" + sImageFiles +
                ", sXmlFiles=" + sXmlFiles +
                ", sCsvFiles=" + sCsvFiles +
                ", sCsvPath='" + sCsvPath + '\'' +
                ", sProjectFile='" + sProjectFile + '\'' +
                ", sXmlPath='" + sXmlPath + '\'' +
                ", sImagePath='" + sImagePath + '\'' +
                ", sImageOutputPath='" + sImageOutputPath + '\'' +
                ", sFilterQueueFile='" + sFilterQueueFile + '\'' +
                ", initialized=" + initialized +
                ", cellPlugin=" + plugin +
                ", settings=" + settings +
                '}';
    }
}
