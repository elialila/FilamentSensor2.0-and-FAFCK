package core.image;

import ij.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import core.settings.Settings;
import core.settings.Load;
import core.settings.Pre;
import util.*;
import util.Annotations.NotNull;
import util.Annotations.Nullable;
import ij.io.Opener;
import ij.plugin.CanvasResizer;
import ij.plugin.Duplicator;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import core.cell.DataFilaments;
import util.StackCreator;
import core.cell.*;
import filters.FilterScale;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageWrapper implements Cloneable {

    /**
     * Used for checking if input images have different sizes (its important for area calculation),
     * conversion to stack make the sizes equal and copies images to center.
     */
    private boolean differentSizes;

    private ImagePlus image;

    private List<Entry> entries;
    private File stackFile = null;//this is normally not used, just in case of a single stack-file(.ome.tif) being loaded


    private CompletableFuture<Void> worker;

    private double currentScale;
    private final List<ChangeListener<Boolean>> listeners;
    private final List<ImageDependency> dependantImages;


    private final BooleanProperty changed;
    private Dimension2D targetMinDimension;

    private Settings parameters;

    //region Constructors
    public ImageWrapper() {
        entries = new ArrayList<>();
        dependantImages = new ArrayList<>();
        listeners = new ArrayList<>();
        changed = new SimpleBooleanProperty();
        currentScale = 1;
    }

    public ImageWrapper(ImagePlus image, Settings parameters) {
        this();
        this.image = image;
        setParameters(parameters);
        List<File> files = new ArrayList<>();
        IntStream.range(0, 1).forEach(i -> files.add(new File(new File(".").getAbsolutePath() + i + ".tif")));
        initializeEntryList(files);
    }


    public ImageWrapper(List<File> files, Settings parameters) throws Exception {
        this();
        Collections.sort(files);
        setParameters(parameters);
        List<ImagePlus> images = getImagePlus(1, files, parameters);
        initStack(images);
        initializeEntryList(files);
    }


    //extends the constructor above with dimension "injection" to pad the stack up to at least the dimension (useful when having lots of different sized images)
    // in a batch process;
    public ImageWrapper(List<File> files, Settings parameters, Dimension2D dimension) throws Exception {
        this();
        Collections.sort(files);
        setParameters(parameters);
        List<ImagePlus> images = getImagePlus(1, files, parameters);
        //get one of the images and pad it to the target dimension (copy the actual image to mid)
        this.targetMinDimension = dimension;
        ImagePlus zero = images.get(0);
        int nWidth = (int) ((targetMinDimension.getWidth() > zero.getWidth()) ? targetMinDimension.getWidth() : zero.getWidth());
        int nHeight = (int) ((targetMinDimension.getHeight() > zero.getHeight()) ? targetMinDimension.getHeight() : zero.getHeight());
        int xOff = (int) ((targetMinDimension.getWidth() - zero.getWidth()) / 2);
        int yOff = (int) ((targetMinDimension.getHeight() - zero.getHeight()) / 2);
        if (xOff > 0 || yOff > 0) {//no need to call this part when the image is larger than dimension
            CanvasResizer canvasResizer = new CanvasResizer();
            zero.setProcessor(canvasResizer.expandImage(zero.getProcessor(), nWidth, nHeight, Math.max(xOff, 0), Math.max(yOff, 0)));
            zero.updateImage();//needed or not?
        }
        initStack(images);
        initializeEntryList(files);

    }


    public ImageWrapper(File file, Settings parameters) {
        this();
        setParameters(parameters);
        Opener opener = new Opener();
        image = opener.openImage(file.getAbsolutePath());
        image.setTitle(file.getAbsolutePath());
        convertImage(image, parameters);
        List<File> files = new ArrayList<>();

        if (file.getName().contains(PathScanner.OME_TIF)) {
            stackFile = file;
            image.setTitle(file.getName());
            //this is a ome.tif file (it contains a stack)
            int stackSize = image.getStackSize();
            if (stackSize > 1) {
                //change File+i+.png to file.getAbsolutePath()+File+i+.png
                IntStream.range(0, stackSize).forEach(i -> files.add(new File(file.getAbsolutePath().replace(".ome.tif", "_") + i + ".tif")));
            }
        } else {
            files.add(file);//only add orig file on non ome.tif
        }
        initializeEntryList(files);
    }

    //endregion

    //region Getter-Setter
    public Dimension2D getTargetMinDimension() {
        return targetMinDimension;
    }

    public File getStackFile() {
        return stackFile;
    }

    public void setStackFile(File stackFile) {
        this.stackFile = stackFile;
    }

    public Settings getParameters() {
        return parameters;
    }

    public void setParameters(Settings dp) {
        this.parameters = dp;
    }

    public CompletableFuture<Void> getWorker() {
        return worker;
    }

    public ImagePlus getImage() {
        return image;
    }

    public BufferedImage getImage(int n) {
        return getImage(n, null);
    }

    public int getWidth() {
        return getImage().getWidth();
    }

    public int getHeight() {
        return getImage().getHeight();
    }

    public int getSize() {
        if (getImage() == null) return 0;
        return getImage().getStackSize();
    }

    public double getCurrentScale() {
        return currentScale;
    }

    public int getMinArea() {
        return (int) (((double) image.getWidth() * image.getHeight()) * getParameters().getValueAsDouble(Pre.min_area));
    }

    public boolean isDifferentSizes() {
        return differentSizes;
    }

    /**
     * @return Returns an entry-list with Quadruple of {File-Path,ImageProcessor,CellShape,DataFilament}
     */
    public List<Entry> getEntryList() {
        return entries;
    }
    //endregion

    //region Initialization-Methods

    /**
     * Initialize EntryList, create Entry for each file
     *
     * @param files sorted list of files
     */
    private void initializeEntryList(final List<File> files) {
        files.forEach(file -> {
            int idx = files.indexOf(file);
            ImageProcessor processor;
            if (files.size() > 1) {
                try {
                    processor = image.getStack().getProcessor(idx + 1);
                } catch (Exception e) {
                    //on this part the only occuring exception should be outofrange
                    processor = null;
                }
            } else {
                processor = image.getProcessor();
            }
            if (processor != null) entries.add(new Entry(file.getAbsolutePath(), processor, null, new DataFilaments()));
        });
    }

    public void loadEntries(List<File> xmlFiles) throws Exception {
        if (entries.size() == 0) throw new Exception("entries.size() == 0");
        getEntryList().forEach(entry -> {

            String fName = new File(entry.getPath()).getName();
            for (String ext : PathScanner.supportedImageExtensions) {
                fName = fName.replace(ext, "");
            }
            String finalName = fName;
            File result = xmlFiles.stream().filter(f -> f.getName().contains(finalName)).findAny().orElse(null);
            if (result != null) {
                try {
                    Entry fEntry = IOUtils.loadXML(result);
                    entry.setDataFilament(fEntry.getDataFilament());
                    entry.setInteriorContainer(fEntry.getInteriorContainer());
                    entry.setOrientationFieldContainer(fEntry.getOrientationFieldContainer());
                    entry.setShape(fEntry.getShape());
                } catch (Exception e) {
                    //ignore? or respond to exception?
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This method updates the ImageProcessor inside the EntryList.
     * This is used if changes happens to the processor(convert stack, for example gray to color).
     */
    public void updateProcessors() {
        for (int i = 0; i < getEntryList().size(); i++) {
            if (getEntryList().size() > 1)
                getEntryList().get(i).setProcessor(getImage().getStack().getProcessor(i + 1));
            else {
                getEntryList().get(i).setProcessor(getImage().getProcessor());
            }
        }
    }

    private void checkSizes(List<ImagePlus> images) {
        //found differing sizes in image and shape
        Comparator<Dimension> dimensionComparator = (d1, d2) -> {
            int res1 = Integer.compare((int) d1.getWidth(), (int) d2.getWidth());
            if (res1 == 0) return Integer.compare((int) d1.getHeight(), (int) d2.getHeight());
            return res1;
        };
        List<Dimension> dims = images.stream().filter(Objects::nonNull).map(imagePlus -> new Dimension(imagePlus.getWidth(), imagePlus.getHeight())).sorted(dimensionComparator).collect(Collectors.toList());
        differentSizes = (dimensionComparator.compare(dims.get(0), dims.get(dims.size() - 1)) != 0);
    }


    private List<ImagePlus> getImagePlus(double scale, List<File> files, Settings dp) {

        List<ImagePlus> images = new ArrayList<>();
        files.forEach(file -> {
            ImagePlus imagePlus = IJ.openImage(file.getAbsolutePath());
            convertImage(imagePlus, dp);
            imagePlus.setTitle(file.getAbsolutePath());
            imagePlus.getFileInfo().fileName = file.getName();
            imagePlus.getFileInfo().directory = file.getParent();
            images.add(imagePlus);
        });
        if (getCurrentScale() != 1) {//if scale is not default scale (default == 1)
            FilterScale filter = new FilterScale();
            filter.setScaleFactor(scale);
            images.parallelStream().forEach(filter::run);
        }
        return images;
    }

    private void initStack(List<ImagePlus> images) throws Exception {
        //check sizes
        checkSizes(images);

        //ImagePlus[] ip = new ImagePlus[images.size()];
        StackCreator stackCreator = new StackCreator();
        String name = new File(images.get(0).getTitle()).getName();
        for (String ext : PathScanner.supportedImageExtensions) {
            name = name.replace(ext, "");
        }
        image = stackCreator.convertImagesToStack(images.toArray(new ImagePlus[0]), StackCreator.COPY_CENTER, false, true, true, name);

        //06.11.2019 iReg test set saved as stack --- sizes are equalized, it does work
        // --- strange behaviour of not copying center should be somewhere else

    }

    private void convertImage(ImagePlus image, Settings dp) {
        //if the image is a one channel red, green or blue image the convertToGray8 does not really convert it
        //it just transforms to 8bit -> this can cause problems later when exporting, during RGB conversion.
        //because those images are also isColorLut()==true
        //for now the solution (ImageConversionUtils.grayLUT() will be used on toRGB method)
        //image->grayLUT->toRGB
        if ((!image.getProcessor().isGrayscale() || image.getProcessor().getBitDepth() > 8) && !dp.getValueAsBoolean(Load.keepBitRange)) {
            ImageConverter converter = new ImageConverter(image);
            converter.convertToGray8();//gray 8 bit because --> many methods are working with values 0-255
        }
    }

    /**
     * Initializes Shapes of the ImageWrapper
     *
     * @param plugin plugin which handles the area calculation
     * @param <T>    Type of CellPlugin
     * @return CompletableFuture to handle callbacks and the state of the calculation
     * @throws ExecutionException   uses getWorker().get() in case of a current worker is still processing
     * @throws InterruptedException uses getWorker().get() in case of a current worker is still processing
     */
    public <T extends CellPlugin> CompletableFuture<Void> initializeShape(T plugin) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(plugin, "ImageWrapper::initializeShape() --- plugin is null");
        if (getWorker() != null) getWorker().get();//wait till worker is done
        if (getParameters() == null)
            throw new RuntimeException("ImageWrapper::initializeShape() --- parameters are null");
        final int minRange = getParameters().getValue(Pre.min_range);

        if (isDifferentSizes()) {//if image derived from stack-file there should never be different sizes
            //different sized
            //calculate from original image and copy to center after that
            List<ImagePlus> images = getImagePlus(getCurrentScale(), getEntryList().stream().map(e -> new File(e.getPath())).sorted(Comparator.comparing(File::getAbsolutePath)).collect(Collectors.toList()), getParameters());
            //imp.getOriginalFileInfo() contains the correct directory and file name
            worker = CompletableFuture.runAsync(() -> {
                /*for (int i = 0; i < images.size(); i++) {
                    ImagePlus tmp = images.get(i);
                    Entry entry = getEntryList().stream().filter(e -> e.getPath().contains(tmp.getOriginalFileInfo().fileName)).findAny().orElse(null);
                    if (entry != null) {
                        double minArea = getParameters().getValueAsDouble(Pre.min_area) * tmp.getWidth() * tmp.getHeight();
                        entry.setShape(CellPlugin.getCellData(tmp.getProcessor(), null, minRange, (int) minArea, plugin));
                    }
                }*/
                images.parallelStream().
                        map(i -> new Pair<>(i, getEntryList().stream().filter(e -> e.getPath().contains(i.getOriginalFileInfo().fileName)).
                                findAny().orElse(null))).filter(p -> p.getValue() != null).forEach(pair ->
                        pair.getValue().setShape(CellPlugin.getCellData(pair.getKey().getProcessor(), null, minRange, getMinArea(), plugin))
                );

            }).thenCompose(v -> {
                copyShapeToCenter();
                return CompletableFuture.completedFuture(null);
            });
            getWorker().exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        } else {
            //equal sized
            //just do normal processing
            worker = CompletableFuture.runAsync(() ->
                    getEntryList().parallelStream().forEach(entry ->
                            entry.setShape(CellPlugin.getCellData(entry.getProcessor(), null,
                                    minRange, getMinArea(), plugin))));
        }

        return getWorker();
    }


    /**
     * Initializes Shapes of the ImageWrapper, only valid for equal sized images
     *
     * @param plugin    plugin which handles the area calculation
     * @param lastShape container of the last shape(if existed)
     * @param <T>       Type of CellPlugin
     * @return CompletableFuture to handle callbacks and the state of the calculation
     * @throws ExecutionException   uses getWorker().get() in case of a current worker is still processing
     * @throws InterruptedException uses getWorker().get() in case of a current worker is still processing
     */
    public <T extends CellPlugin> CompletableFuture<Void> initializeShapeRestricted(@NotNull T plugin, @Nullable ShapeContainer lastShape) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(plugin, "ImageWrapper::initializeShapeRestricted() --- plugin is null");
        if (getWorker() != null) getWorker().get();//wait till worker is done
        if (isDifferentSizes())
            throw new IllegalArgumentException("ImageWrapper::initializeShapeRestricted() --- Images have to be equal sized!");
        if (getParameters() == null)
            throw new RuntimeException("ImageWrapper::initializeShape() --- parameters are null");
        final int minRange = getParameters().getValue(Pre.min_range);
        //there is no point in calculating new shapes which area is restricted by the previous shape, when those images aren't equal sized
        worker = CompletableFuture.runAsync(() -> {
            List<Entry> entryList = getEntryList();
            for (int i = 0; i < entryList.size(); i++) {
                Entry entry = entryList.get(i);
                if (i == 0) {
                    entry.setShape(CellPlugin.getCellData(entry.getProcessor(), lastShape, minRange, getMinArea(), plugin));
                } else {
                    Entry last = entryList.get(i - 1);
                    entry.setShape(CellPlugin.getCellData(entry.getProcessor(), last.getShape(), minRange, getMinArea(), plugin));
                }
            }
        });
        return worker;
    }


    private void copyShapeToCenter() {
        //found differing sizes in image and shape
        final int newWidth = getImage().getWidth(), newHeight = getImage().getHeight();
        Consumer<CellShape> resize = (cell) -> {
            final int stepX = (newWidth - cell.getBinaryImage().getWidth()) / 2;
            final int stepY = (newHeight - cell.getBinaryImage().getHeight()) / 2;
            cell.setBounds(new Rectangle((int) cell.getBounds().getX() + stepX,
                    (int) cell.getBounds().getY() + stepY,
                    (int) cell.getBounds().getWidth(),
                    (int) cell.getBounds().getHeight())
            );
            cell.setBinaryImage(((BinaryImage) cell.getBinaryImage()).copyToCenter(newWidth, newHeight));
        };

        getEntryList().parallelStream().forEach(entry -> {
            //relocate the bounds of area and ext area
            entry.getShape().getAreas().forEach(resize);
            entry.getShape().getExtAreas().forEach(resize);
            entry.getShape().clearCache();//clear cache of aggregated areas, otherwise there could be an exception
            //due to stored smaller images
        });
    }

    //endregion


    /**
     * @param n        [1,stackSize]
     * @param filament can be null
     * @return get BufferedImage from ImageStack
     */
    public BufferedImage getImage(int n, @Nullable Color filament) {
        if (n > getEntryList().size() || n < 1) throw new IllegalArgumentException("(n<1||n>size):" + n);
        List<Entry> entryList = getEntryList();
        if (entryList.size() == 0) return null;
        Entry entry = entryList.get(n - 1);
        BufferedImage output = ImageExporter.getBufferedImage(entry.getProcessor());
        if (filament != null) {
            ImageExporter.addFilaments(output, entry.getDataFilament(), filament);
        }
        return output;
    }


    /**
     * @param factor decimal 1=orig size, 0.5 half, 2 doubled size ...
     */
    public void scale(double factor) {
        if (currentScale == factor) {
            return;
        }
        currentScale = factor;
        worker.cancel(true);
        FilterScale filterScale = new FilterScale(factor);
        image = null;
        try {
            filterScale.run(getImage());
            getEntryList().forEach(entry -> entry.setShape(null));
            notifyListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addListener(ChangeListener<Boolean> changeListener) {
        listeners.add(changeListener);
    }

    public void removeListener(ChangeListener<Boolean> changeListener) {
        listeners.remove(changeListener);
    }

    public void addImageDependency(ImageDependency dependency) {
        dependantImages.add(dependency);
    }

    public void removeImageDependency(ImageDependency dependency) {
        dependantImages.remove(dependency);
    }

    /**
     * Updates Dependent ImageWrapper's if existent
     *
     * @param sourceOfUpdates original source of the update chain
     * @param chainUpdate     true: updates recursive; false: just updates the directly dependent object
     * check if currently updating, if yes note that another update should be done but don't start a second update
     * this should be done in the dependency object
     */
    public CompletableFuture<Void> updateDependencies(ImageWrapper sourceOfUpdates, boolean chainUpdate) {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        ObjectProperty<CompletableFuture<Void>> tmp = new SimpleObjectProperty<>(future);
        //a first try to call each updateDependency after finishing the previous
        //overall its still async but the calls are chained and not parallel
        dependantImages.forEach(dependency -> {
            tmp.set(tmp.get().thenCompose((v) -> dependency.updateDependency(this, sourceOfUpdates, chainUpdate)));
            //dependency.updateDependency(this,chainUpdate);//these update should probably not be stacked async, they should
            //be called serial (since internal code of those dependencies could be parallel)
        });
        return tmp.get();
    }


    public void notifyListeners() {
        listeners.forEach(this::accept);
    }

    private void accept(ChangeListener<Boolean> cL) {
        cL.changed(changed, false, true);
    }


    public void closeImage() {
        getImage().close();
        image = null;
    }


    /**
     * Deep copy of image data, no copies of listeners, shallow copy of shapes, shallow copy of files
     *
     * @return copy of current ImageWrapper
     */
    public ImageWrapper clone() {
        //drop any selections otherwise only the selected part will be copied
        //especially the case if a ome.tif file is saved and there were some roi's left
        this.getImage().deleteRoi();
        ImageWrapper copy = new ImageWrapper();
        cloneData(copy);
        return copy;
    }

    /**
     * This should just clone the image data
     *
     * @param copy object which should get data from current ImageWrapper
     */
    public void cloneImage(ImageWrapper copy) {
        Duplicator duplicator = new Duplicator();
        copy.image = duplicator.run(image);
        //copy image and update/init the entries in the entryList
        if (copy.entries == null || copy.entries.size() == 0) {
            copy.initializeEntryList(getEntryList().stream().
                    map(e -> new File(e.getPath())).
                    sorted(Comparator.comparing(File::getAbsolutePath)).
                    collect(Collectors.toList())
            );
        } else {
            copy.updateProcessors();
        }
        //if exists check size()
        //then replace image processor's
    }

    /**
     * This should just clone the image data
     *
     * @param copy     object which should get data from current ImageWrapper
     * @param imageIdx index of image which should be cloned (for stack usage), index start with zero
     */
    public void cloneImage(ImageWrapper copy, int imageIdx) {
        //n is starting with 1 at getStack.setProcessor
        copy.image.getStack().setProcessor(getEntryList().get(imageIdx).getProcessor().duplicate(), imageIdx + 1);
        if (copy.entries == null || copy.entries.size() == 0) {
            copy.initializeEntryList(getEntryList().stream().
                    map(e -> new File(e.getPath())).
                    sorted(Comparator.comparing(File::getAbsolutePath)).
                    collect(Collectors.toList())
            );
        } else {
            copy.updateProcessors();
        }
    }


    /**
     * Deep copies the image-data but keeps listeners and entry data(shallow copy)
     *
     * @param copy object which should get data from current ImageWrapper
     */
    public void cloneData(ImageWrapper copy) {

        Duplicator duplicator = new Duplicator();
        copy.image = duplicator.run(image);
        copy.entries.clear();
        //clone entry list and replace imageprocessors
        copy.initializeEntryList(getEntryList().stream().
                map(e -> new File(e.getPath())).
                sorted(Comparator.comparing(File::getAbsolutePath)).
                collect(Collectors.toList())
        );
        //soft copy DataFilaments&Shapes
        //since both lists should have the same order
        for (int i = 0; i < getEntryList().size(); i++) {
            copy.getEntryList().get(i).setShape(getEntryList().get(i).getShape());
            copy.getEntryList().get(i).setDataFilament(getEntryList().get(i).getDataFilament());
            copy.getEntryList().get(i).setInteriorContainer(getEntryList().get(i).getInteriorContainer());
            copy.getEntryList().get(i).setOrientationFieldContainer(getEntryList().get(i).getOrientationFieldContainer());
            copy.getEntryList().get(i).setCorrelationData(getEntryList().get(i).getCorrelationData());
        }
        copy.currentScale = currentScale;
        copy.setParameters(getParameters());
        copy.differentSizes = differentSizes;

    }


    /**
     * Gives a Subset of the ImageWrapper
     *
     * @param startIdx starts at 1
     * @param length length of sub-set
     * @return subset from startIdx to startIdx+length
     */
    public ImageWrapper getSubset(int startIdx, int length) {
        ImageWrapper subset = this.clone();
        subset.entries = new ArrayList<>(this.entries.subList(startIdx - 1, startIdx - 1 + length));

        for (int i = startIdx + length; i <= this.getSize(); i++) {
            subset.getImage().getStack().deleteLastSlice();
        }

        for (int i = 1; i < startIdx; i++) {
            subset.getImage().getStack().deleteSlice(1);
        }

        return subset;
    }


}
