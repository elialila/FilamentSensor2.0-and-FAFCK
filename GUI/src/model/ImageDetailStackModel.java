package model;

import core.FilterQueue;
import core.ProjectData;
import core.cell.DataFilaments;
import javafx.beans.property.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import core.cell.ShapeContainer;
import core.image.Entry;
import core.image.ImageWrapper;
import util.ImageExporter;
import util.ProcessingUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class ImageDetailStackModel {

    public final static int iwOriginal = 0;
    public final static int iwPreProcessed = 1;
    public final static int iwLineSensor = 2;
    public final static int iwFilaments = 3;


    private ImageWrapper stackOrig;
    private ImageWrapper stackPreprocessed;
    private ImageWrapper stackLineSensor;
    private ImageWrapper stackFilaments;


    private ObjectProperty<Color> filamentColor;

    private IntegerProperty currentImage;

    private BooleanProperty includeAreaOutline;
    private BooleanProperty preview;


    private ObjectProperty<FilterQueue> preProcessing;


    private ImageWrapper previewOrig;
    private ImageWrapper previewPreprocessed;
    private ImageWrapper previewLineSensor;
    private ImageWrapper previewFilaments;

    private CompletableFuture<Boolean> ready;


    public ImageDetailStackModel() {
        ready = new CompletableFuture<>();
        filamentColor = new SimpleObjectProperty<>();
        currentImage = new SimpleIntegerProperty();
        includeAreaOutline = new SimpleBooleanProperty();
        preview = new SimpleBooleanProperty();
        includeAreaOutlineProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) stackOrig.notifyListeners();
        });
        preProcessing = new SimpleObjectProperty<>();
    }

    public ImageDetailStackModel(ImageWrapper stack) {
        this();
        this.stackOrig = stack;
        this.stackPreprocessed = stack;//init with referenceCopy, on any filters it will create a deep copy
        this.stackLineSensor = stack;
        this.stackFilaments = stack;
    }

    public CompletableFuture<Boolean> getReady() {
        return ready;
    }

    public void setReady() {
        ready.complete(true);
    }


    public void initPreview(ProjectData projectData, FilterQueue lineSensor, int minRange) {

        previewOrig = stackOrig.getSubset(getCurrentImage(), 1);
        try {
            previewOrig.initializeShape(projectData.getPlugin()).get();
            previewPreprocessed = new ImageWrapper();
            ProcessingUtils.initializePreprocessing(previewOrig, stackOrig.getCurrentScale(), previewPreprocessed);
            ProcessingUtils.preProcess(previewPreprocessed, getPreProcessing(), (f) -> {
            });
            previewLineSensor = previewPreprocessed.clone();
            ProcessingUtils.lineSensor(previewPreprocessed, previewLineSensor, lineSensor, (f) -> {
            });
            previewFilaments = previewOrig.clone();

            //filaments removed from preview because off low performance (long waiting times are bad for previewing)
            //ProcessingUtils.filamentSensor(previewLineSensor, previewFilaments, parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void setStackPreprocessed(ImageWrapper stack) {
        this.stackPreprocessed = stack;
    }

    public ImageWrapper getStackPreprocessed() {
        return this.stackPreprocessed;
    }

    public ImageWrapper getStackOrig() {
        return stackOrig;
    }

    public void setStackOrig(ImageWrapper stackOrig) {
        this.stackOrig = stackOrig;
    }

    public ImageWrapper getStackLineSensor() {
        return stackLineSensor;
    }

    public void setStackLineSensor(ImageWrapper stackLineSensor) {
        this.stackLineSensor = stackLineSensor;
    }

    public ImageWrapper getStackFilaments() {
        return stackFilaments;
    }

    public void setStackFilaments(ImageWrapper stackFilaments) {
        this.stackFilaments = stackFilaments;
    }

    public Image getImage(int iwIndex, boolean preview) throws Exception {
        if (!preview) return getImage(iwIndex);
        Image image = null;
        switch (iwIndex) {
            case iwOriginal:
                image = getImage(1, previewOrig, false, isIncludeAreaOutline());
                break;
            case iwPreProcessed:
                image = getImage(1, previewPreprocessed,false,false);
                break;
            case iwLineSensor:
                image = getImage(1, previewLineSensor,false,false);
                break;
            case iwFilaments:
                image = getImage(1, previewFilaments,true,false);
                break;
            default:
                break;
        }
        return image;
    }


    public Image getImage(int iwIndex) throws Exception {
        Image image = null;
        int i = getCurrentImage();

        switch (iwIndex) {
            case iwOriginal:
                image = getImage(i, stackOrig, false, isIncludeAreaOutline());
                break;
            case iwPreProcessed:
                image = getImage(i, stackPreprocessed, false, false);
                break;
            case iwLineSensor:
                image = getImage(i, stackLineSensor, false, false);
                break;
            case iwFilaments:
                image = getImage(i, stackFilaments, true, false);
                break;
            default:
                break;
        }
        return image;
    }


    private Image getImage(int i, ImageWrapper stack, boolean fil, boolean area) throws Exception {
        if (stack == null) throw new Exception("stack == null");
        if (i <= 0) return null;
        BufferedImage bufferedImage = null;
        String title=null;

        List<Entry> entryList = stack.getEntryList();
        if (i > entryList.size())
            throw new IllegalArgumentException("getImage(" + i + ") out of Bounds(" + entryList.size() + ")");
        bufferedImage = stack.getImage(i, (fil) ? getFilamentColor() : null);

        if (area && bufferedImage != null) {
            ShapeContainer shape = entryList.get(i - 1).getShape();
            if (shape != null) bufferedImage = ImageExporter.addArea(bufferedImage, shape);
        }
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }





    public DataFilaments getFilaments() {
        return stackFilaments.getEntryList().get(getCurrentImage() - 1).getDataFilament();
    }


    public int getSize() {
        if (getStackOrig() == null) return 0;
        return getStackOrig().getSize();
    }

    public Color getFilamentColor() {
        return filamentColor.get();
    }

    public ObjectProperty<Color> filamentColorProperty() {
        return filamentColor;
    }

    public void setFilamentColor(Color filamentColor) {
        this.filamentColor.set(filamentColor);
    }

    public int getCurrentImage() {
        return currentImage.get();
    }

    public IntegerProperty currentImageProperty() {
        return currentImage;
    }

    public void setCurrentImage(int currentImage) {
        this.currentImage.set(currentImage);


    }

    public boolean isIncludeAreaOutline() {
        return includeAreaOutline.get();
    }

    public BooleanProperty includeAreaOutlineProperty() {
        return includeAreaOutline;
    }

    public void setIncludeAreaOutline(boolean includeAreaOutline) {
        this.includeAreaOutline.set(includeAreaOutline);
    }

    public boolean isPreview() {
        return preview.get();
    }

    public BooleanProperty previewProperty() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview.set(preview);
    }

    public FilterQueue getPreProcessing() {
        return preProcessing.get();
    }

    public ObjectProperty<FilterQueue> preProcessingProperty() {
        return preProcessing;
    }

    public void setPreProcessing(FilterQueue preProcessing) {
        this.preProcessing.set(preProcessing);
    }
}
