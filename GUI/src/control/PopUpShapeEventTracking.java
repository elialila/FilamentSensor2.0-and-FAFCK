package control;

import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.ATracking;
import core.settings.Settings;
import fx.custom.SliderSpinner;
import fx.custom.StackImageView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import tracking.shape.ShapeEvent;
import tracking.shape.ShapeTracker;
import tracking.shape.TimeTable;
import tracking.shape.TrackingShape;
import util.Annotations;
import util.IOUtils;
import util.ImageExporter;
import util.MixedUtils;
import util.io.CsvExport;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PopUpShapeEventTracking<T extends TrackingShape> extends AbstractControl {
    @FXML
    private TextArea taDebugAreaTracking;
    @FXML
    private TableView<TimeTable<T>> tableAreas;
    @FXML
    private TableColumn<TimeTable<T>, Integer> columnBirth;
    @FXML
    private TableColumn<TimeTable<T>, Integer> columnDeath;
    @FXML
    private TableColumn<TimeTable<T>, Double> columnLength;
    @FXML
    private CheckBox chkMaxTimeExist;
    @FXML
    private SliderSpinner sMaxExistArea;
    @FXML
    private RadioButton radioParam;
    @FXML
    private RadioButton radioAreas;
    @FXML
    private VBox vBoxParameters;
    @FXML
    private VBox vBoxAreas;
    @FXML
    private ToggleGroup tgFilter;

    @FXML
    private StackImageView ivAreas;
    @FXML
    private Text tImageSize;
    @FXML
    private Text tUnfilteredTimeLines;
    @FXML
    private Text tFilteredTimeLines;
    @FXML
    private CheckBox chkMinTimeExist;
    @FXML
    private SliderSpinner sMinExistArea;
    @FXML
    private StackImageView ivOverview;
    @FXML
    private ProgressBar pbProgress;

    @FXML
    private SliderSpinner sIntersectTolerance;

    private final AtomicBoolean updatePending;
    private final AtomicBoolean isUpdating;

    private final ShapeTracker<T> tracking;
    private List<Color> colors;
    private final int cOffset = 634;//just a random number to shift the starting colors

    private Supplier<List<List<T>>> trackingDataSupplier = null;
    private ImageWrapper wrapper;


    public PopUpShapeEventTracking() {
        updatePending = new AtomicBoolean(false);
        isUpdating = new AtomicBoolean(false);
        tracking = new ShapeTracker<>();
    }

    @FXML
    private void initialize() {
        colors = MixedUtils.getColors();
        vBoxParameters.visibleProperty().bind(tgFilter.selectedToggleProperty().isEqualTo(radioParam));
        vBoxAreas.visibleProperty().bind(tgFilter.selectedToggleProperty().isEqualTo(radioAreas));
        tgFilter.selectToggle(radioParam);
        initTable();
    }

    private void message(String message) {
        Platform.runLater(() -> taDebugAreaTracking.appendText(message + "\n"));
    }


    public void setTrackingDataSupplier(Supplier<List<List<T>>> supplier) {
        this.trackingDataSupplier = supplier;
    }

    public void setImage(ImageWrapper image) {
        this.wrapper = image;
    }

    public void initializeAfterSetMainController() {

        if (trackingDataSupplier == null || wrapper == null)
            throw new RuntimeException("Initialization not done correctly.");

        ivAreas.currentProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null) wrapper.notifyListeners();
        });

        wrapper.addListener(((observableValue, aBoolean, t1) -> {
            if (tracking.getUniqueShapes() != null && tracking.getUniqueShapes().size() > 0) {
                //current start value = 1
                int current = ivAreas.getCurrent();

                BufferedImage bi = wrapper.getImage(current);
                Settings dp = getMainController().getModel().getProjectData().getSettings();
                final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
                final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
                final int minLength = dp.getValue(ATracking.existsInMin);
                final int maxLength = dp.getValue(ATracking.existsInMax);
                ivAreas.setImage(SwingFXUtils.toFXImage(bi, null));

                //linear gradient used for selected color, to be able to differentiate it from normal color
                Stop[] stops = new Stop[]{new Stop(0, javafx.scene.paint.Color.GOLD), new Stop(1, javafx.scene.paint.Color.RED)};
                LinearGradient selected = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);

                initAreasShapes(tracking.filterUniqueShapes(chkMin, minLength, chkMax, maxLength),
                        ivAreas,
                        selected,//javafx.scene.paint.Color.GOLD,
                        null);

            }
        }));

    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        sMinExistArea.initRanges(1, getMainController().getModel().getStackModel().getSize(), 1);
        sMaxExistArea.initRanges(1, getMainController().getModel().getStackModel().getSize(), 1);
        getMainController().getModel().getProjectData().getSettings().setProperty(ATracking.existsInMax, getMainController().getModel().getStackModel().getSize());
        ivAreas.setMax(getMainController().getModel().getStackModel().getSize());

        sIntersectTolerance.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(ATracking.intersectTolerance));
        chkMinTimeExist.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(ATracking.chkExistsInMin));
        chkMaxTimeExist.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(ATracking.chkExistsInMax));

        sMinExistArea.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(ATracking.existsInMin));
        sMaxExistArea.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(ATracking.existsInMax));

        ChangeListener<Number> listener = (observable, oldValue, newValue) -> {
            update(true);
        };
        sIntersectTolerance.valueProperty().addListener(listener);

        //this does not need an update, actually it just has to redo the image
        sMinExistArea.valueProperty().addListener((observableValue, number, t1) -> update(false));
        chkMinTimeExist.selectedProperty().addListener((observableValue, aBoolean, t1) -> update(false));

        sMaxExistArea.valueProperty().addListener((observableValue, number, t1) -> update(false));
        chkMaxTimeExist.selectedProperty().addListener((observableValue, aBoolean, t1) -> update(false));


        //don't update on popup open
        //update(true);
    }


    private void update(boolean reCalc) {
        if (isUpdating.compareAndSet(false, true)) {
            runTracking(reCalc).thenAccept((v) -> {
                if (updatePending.get()) {
                    updatePending.set(false);
                    update(reCalc);
                }
            });
        } else {
            updatePending.set(true);
        }
    }

    private void initTable() {
        tableAreas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableAreas.setEditable(true);

        columnBirth.setCellValueFactory(cellData -> cellData.getValue().birthProperty().asObject());
        columnDeath.setCellValueFactory(cellData -> cellData.getValue().deathProperty().asObject());
        columnLength.setCellValueFactory(cellData -> cellData.getValue().lengthProperty().asObject());

        //for highlighting filaments
        tableAreas.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.selectedProperty().setValue(false);
            }
            if (newValue != null) {
                newValue.selectedProperty().setValue(true);
            }
        });

    }


    private CompletableFuture<Void> runTracking(boolean reCalc) {
        if (trackingDataSupplier == null) {
            message("Something went wrong, there is no Data Supplier!");
            return CompletableFuture.completedFuture(null);
        }


        //isUpdating.set(true);
        final Settings dp = getMainController().getModel().getProjectData().getSettings();

        final boolean chkMinLength = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final int minLength = dp.getValue(ATracking.existsInMin);

        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int maxLength = dp.getValue(ATracking.existsInMax);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    long time = System.currentTimeMillis();

                    try {
                        if (reCalc) {
                            tracking.process(trackingDataSupplier.get(),
                                    getMainController().getModel().getProjectData().getSettings(),
                                    (progress) -> Platform.runLater(() -> updateProgress(progress, 100)));
                        }
                        if (tracking.getUniqueShapes().size() > 0) {
                            BufferedImage image = ImageExporter.getShapeTrackingOverview(tracking, chkMinLength, minLength, chkMax, maxLength);
                            Platform.runLater(() -> {
                                ivOverview.setImage(SwingFXUtils.toFXImage(image, null));
                                tUnfilteredTimeLines.setText(Integer.toString(tracking.getUniqueShapes().size()));
                                tFilteredTimeLines.setText(Integer.toString(tracking.filterUniqueShapes(chkMinLength, minLength, chkMax, maxLength).size()));
                                tImageSize.setText(image.getWidth() + " x " + image.getHeight());
                                //init table
                                tableAreas.setItems(FXCollections.observableList(tracking.filterUniqueShapes(chkMinLength, minLength, chkMax, maxLength)));
                                //update image
                                wrapper.notifyListeners();

                            });
                        } else {
                            message("Press Track first - Time slot filters do not run calculations");
                        }

                    } catch (Exception e) {
                        message("AreaTracking not succeeded");
                        message(e.getMessage());
                    }
                    message("Area-Tracking Time taken:" + ((double) System.currentTimeMillis() - time) / 1000 + "s");
                } catch (Exception e) {
                    e.printStackTrace();
                    message("AreaTracking Finished with Exception");
                }
                updateProgress(100, 100);
                isUpdating.set(false);
                return null;
            }
        };
        Platform.runLater(() -> pbProgress.progressProperty().bind(task.progressProperty()));//since this can be called
        //on non fx thread wrap it
        message("Start Area Tracking");
        return CompletableFuture.runAsync(task);
    }


    protected Shape getAreaShape(javafx.util.Pair<T, TimeTable<T>> pair, javafx.scene.paint.Paint stroke, final javafx.scene.paint.Paint strokeSelected, DoubleBinding scaleBinding) {
        TimeTable<T> dynArea = pair.getValue();
        //Shape FX should already have the scaling included, only set visual parameters
        Shape shapeFX = pair.getKey().getFXBounds(getMainController().getModel().getProjectData().getSettings(), scaleBinding);
        shapeFX.setFill(javafx.scene.paint.Color.rgb(200, 200, 200, 0.1));
        shapeFX.setStyle("-fx-background-color:transparent;");//transparent
        shapeFX.setUserData(pair);
        shapeFX.strokeProperty().bind(Bindings.createObjectBinding(() -> (dynArea.selectedProperty().get() ? strokeSelected : stroke), dynArea.selectedProperty()));
        shapeFX.strokeWidthProperty().bind(scaleBinding.multiply(2));

        return shapeFX;
    }

    protected void initAreasShapes(List<TimeTable<T>> dynamicAreas, StackImageView svAreas, final javafx.scene.paint.Paint strokeSelected, @Annotations.Nullable Consumer<List<Rectangle>> callback) {
        //imageView.getCurrent() starting value=1; AreaTracker works with starting time=0;
        final int current = ivAreas.getCurrent() - 1;//subtract 1 to match times
        Platform.runLater(() ->
                svAreas.getStackPane().getChildren().removeAll(
                        svAreas.getStackPane().getChildren().
                                stream().filter(e -> e instanceof Pane && Objects.equals(e.getUserData(), "areas")).collect(Collectors.toList())
                )
        );
        if (dynamicAreas == null ||
                dynamicAreas.size() == 0) return;

        Pane shapesFX = new Pane();
        shapesFX.setUserData("areas");

        AtomicInteger colorCounter = new AtomicInteger(10);

        DoubleBinding scaleBinding = svAreas.getScaleBinding();
        dynamicAreas.forEach(dynamicArea -> {

            Color groupColor = colors.get((colorCounter.getAndIncrement() + cOffset) % colors.size());
            javafx.scene.paint.Paint stroke = javafx.scene.paint.Color.rgb(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), ((double) groupColor.getAlpha()) / 255);

            ShapeEvent<T> event = dynamicArea.getShapes().get(current);
            if (event != null) {


                if (event instanceof ShapeEvent.ShapeAliveEvent) {
                    T shape = ((ShapeEvent.ShapeAliveEvent<T>) event).getTarget();
                    shapesFX.getChildren().add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof ShapeEvent.ShapeSplitEvent) {
                    List<T> shapes = ((ShapeEvent.ShapeSplitEvent<T>) event).getTarget();
                    shapes.forEach(shape -> shapesFX.getChildren().
                            add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea),
                                    stroke, strokeSelected, scaleBinding))
                    );
                } else if (event instanceof ShapeEvent.ShapeDeTouchEvent) {
                    List<T> shapes = ((ShapeEvent.ShapeDeTouchEvent<T>) event).getTarget();
                    shapes.forEach(shape -> shapesFX.getChildren().
                            add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea),
                                    stroke, strokeSelected, scaleBinding))
                    );
                } else if (event instanceof ShapeEvent.ShapeSingleSourceEvent) {//start and end event
                    T shape = ((ShapeEvent.ShapeSingleSourceEvent<T>) event).getSource();
                    shapesFX.getChildren().add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof ShapeEvent.ShapeFusionEvent) {
                    T shape = ((ShapeEvent.ShapeFusionEvent<T>) event).getTarget();
                    shapesFX.getChildren().add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof ShapeEvent.ShapeTouchEvent) {
                    T shape = ((ShapeEvent.ShapeTouchEvent<T>) event).getTarget();
                    shapesFX.getChildren().add(getAreaShape(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                }
            }

        });

        shapesFX.maxHeightProperty().bind(svAreas.getStackPane().heightProperty());
        shapesFX.maxWidthProperty().bind(svAreas.getStackPane().widthProperty());
        shapesFX.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(shapesFX, Pos.TOP_LEFT);
        Platform.runLater(() -> {
            svAreas.getStackPane().getChildren().add(shapesFX);
            if (callback != null)
                callback.accept(shapesFX.getChildren().stream().map(i -> (Rectangle) i).collect(Collectors.toList()));
        });//wrap it with javaFx thread

    }


    @FXML
    private void onExportDynAreasCSV(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        File csvFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateProgress(10, 100);
                    CsvExport.exportTimeTableCsvGroupedByShape(tracking, wrapper, csvFile, getMainController().getModel().getProjectData().getSettings());
                    message("Export Successful");
                } catch (IOException e) {
                    message(e.getMessage());
                    e.printStackTrace();
                }
                updateProgress(100, 100);
                return null;
            }
        }, pbProgress);


    }

    @FXML
    private void onExportAreasCSV(ActionEvent event) {
        //this part should export grouped by time (for each image a csv?) and contain a dynamic filament number
        //for each filament (if its inside the dynamic filament, otherwise -1, null, or some other placeholder)
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        File csvFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateProgress(10, 100);
                    CsvExport.exportTimeTableCsvGroupedByTime(tracking, wrapper, csvFile, getMainController().getModel().getProjectData().getSettings());
                    message("Export Successful");
                } catch (IOException e) {
                    message(e.getMessage());
                    e.printStackTrace();
                }
                updateProgress(100, 100);
                return null;
            }
        }, pbProgress);


    }

    @FXML
    private void onExportDynAreasImage(ActionEvent event) {
        //this part should export the image with all dynamic filaments grouped by their lifespan(color coded)
        //image export should be 1 image per image/time slot
        final Settings dp = getMainController().getModel().getProjectData().getSettings();
        final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
        final int minLength = dp.getValue(ATracking.existsInMin);
        final int maxLength = dp.getValue(ATracking.existsInMax);

        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        File imageDirectory = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                updateProgress(10, 100);
                List<TimeTable<T>> filtered = tracking.filterUniqueShapes(chkMin, minLength, chkMax, maxLength);

                //iterate over all images in stack
                IntStream.range(0, wrapper.getSize()).forEach(current -> {
                    //retrieve current stack entry
                    Entry entry = wrapper.getEntryList().get(current);
                    //get the orig image as buffered image
                    BufferedImage bi = ImageExporter.getBufferedImage(entry.getProcessor());
                    //group the filtered DynamicFilaments by their lifespan

                    AtomicInteger colorCounter = new AtomicInteger(10);
                    //add areas
                    filtered.forEach(dynamicArea -> {
                        Color groupColor = colors.get((colorCounter.getAndIncrement() + cOffset) % colors.size());
                        ShapeEvent<T> event = dynamicArea.getShapes().get(current);
                        if (event != null) {
                            if (event instanceof ShapeEvent.ShapeAliveEvent) {
                                T shape = ((ShapeEvent.ShapeAliveEvent<T>) event).getTarget();
                                ImageExporter.addShape(bi, shape, groupColor, true, dp);
                            } else if (event instanceof ShapeEvent.ShapeSplitEvent) {
                                ((ShapeEvent.ShapeSplitEvent<T>) event).getTarget().forEach(shape -> ImageExporter.addShape(bi, shape, groupColor, true, dp));
                            } else if (event instanceof ShapeEvent.ShapeDeTouchEvent) {
                                ((ShapeEvent.ShapeDeTouchEvent<T>) event).getTarget().forEach(shape -> ImageExporter.addShape(bi, shape, groupColor, true, dp));
                            } else if (event instanceof ShapeEvent.ShapeSingleSourceEvent) {//start and end event
                                T shape = ((ShapeEvent.ShapeSingleSourceEvent<T>) event).getSource();
                                ImageExporter.addShape(bi, shape, groupColor, true, dp);
                            } else if (event instanceof ShapeEvent.ShapeFusionEvent) {
                                T shape = ((ShapeEvent.ShapeFusionEvent<T>) event).getTarget();
                                ImageExporter.addShape(bi, shape, groupColor, true, dp);
                            } else if (event instanceof ShapeEvent.ShapeTouchEvent) {
                                T shape = ((ShapeEvent.ShapeTouchEvent<T>) event).getTarget();
                                ImageExporter.addShape(bi, shape, groupColor, true, dp);
                            }
                        }

                    });
                    try {
                        //export the image with all filaments added
                        ImageExporter.exportImage(bi, IOUtils.getOutFileFromImageFile(new File(entry.getPath()), imageDirectory, ".png", "cc_dynArea"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                updateProgress(100, 100);
                message("Export Successful");
                return null;
            }
        }, pbProgress);


    }

    @FXML
    private void onTrack(ActionEvent actionEvent) {
        update(true);
    }
}
