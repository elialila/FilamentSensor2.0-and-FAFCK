package control;

import core.cell.CellShape;
import core.image.Entry;
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
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import tracking.area.AreaTracker;
import tracking.area.CellEvent;
import tracking.area.DynamicArea;
import util.*;
import util.io.AreaCsvExport;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PopUpCellEventTracking extends AbstractControl {
    @FXML
    private TextArea taDebugAreaTracking;
    @FXML
    private TableView<DynamicArea> tableAreas;
    @FXML
    private TableColumn<DynamicArea, Integer> columnBirth;
    @FXML
    private TableColumn<DynamicArea, Integer> columnDeath;
    @FXML
    private TableColumn<DynamicArea, Double> columnLength;
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

    private AreaTracker tracking;
    private java.util.List<Color> colors;
    private final int cOffset = 634;//just a random number to shift the starting colors


    public PopUpCellEventTracking() {
        updatePending = new AtomicBoolean(false);
        isUpdating = new AtomicBoolean(false);
        tracking = new AreaTracker();
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

        ivAreas.currentProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null) getMainController().getModel().getStackModel().getStackOrig().notifyListeners();
        });

        getMainController().getModel().getStackModel().getStackOrig().addListener(((observableValue, aBoolean, t1) -> {
            if (tracking.getUniqueAreas() != null && tracking.getUniqueAreas().size() > 0) {
                //current start value = 1
                int current = ivAreas.getCurrent();

                BufferedImage bi = getMainController().getModel().getStackModel().getStackOrig().getImage(current);
                Settings dp = getMainController().getModel().getProjectData().getSettings();
                final boolean chkMin = dp.getValueAsBoolean(ATracking.chkExistsInMin);
                final boolean chkMax = dp.getValueAsBoolean(ATracking.chkExistsInMax);
                final int minLength = dp.getValue(ATracking.existsInMin);
                final int maxLength = dp.getValue(ATracking.existsInMax);
                ivAreas.setImage(SwingFXUtils.toFXImage(bi, null));

                //linear gradient used for selected color, to be able to differentiate it from normal color
                Stop[] stops = new Stop[]{new Stop(0, javafx.scene.paint.Color.GOLD), new Stop(1, javafx.scene.paint.Color.RED)};
                LinearGradient selected = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);

                initAreasRectangle(tracking.filterUniqueAreas(chkMin, minLength, chkMax, maxLength),
                        ivAreas,
                        selected,//javafx.scene.paint.Color.GOLD,
                        null);

            }
        }));

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
        //isUpdating.set(true);
        Settings dp = getMainController().getModel().getProjectData().getSettings();

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
                            if (getMainController().getModel().getStackModel().getStackOrig().getWorker() != null)
                                getMainController().getModel().getStackModel().getStackOrig().getWorker().get();//check if a worker is active
                            tracking.process(getMainController().getModel().getStackModel().getStackOrig(),
                                    getMainController().getModel().getProjectData().getSettings(),
                                    (progress) -> Platform.runLater(() -> updateProgress(progress, 100)));
                        }
                        if (tracking.getUniqueAreas().size() > 0) {
                            BufferedImage image = ImageExporter.getAreaTrackingOverview(tracking, chkMinLength, minLength, chkMax, maxLength);
                            Platform.runLater(() -> {
                                ivOverview.setImage(SwingFXUtils.toFXImage(image, null));
                                tUnfilteredTimeLines.setText(Integer.toString(tracking.getUniqueAreas().size()));
                                tFilteredTimeLines.setText(Integer.toString(tracking.filterUniqueAreas(chkMinLength, minLength, chkMax, maxLength).size()));
                                tImageSize.setText(image.getWidth() + " x " + image.getHeight());
                                //init table
                                tableAreas.setItems(FXCollections.observableList(tracking.filterUniqueAreas(chkMinLength, minLength, chkMax, maxLength)));
                                //update image
                                getMainController().getModel().getStackModel().getStackOrig().notifyListeners();

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


    protected Rectangle getAreaRectangle(javafx.util.Pair<CellShape, DynamicArea> pair, javafx.scene.paint.Paint stroke, final javafx.scene.paint.Paint strokeSelected, DoubleBinding scaleBinding) {

        CellShape shape = pair.getKey();
        DynamicArea dynArea = pair.getValue();

        double currentScale = scaleBinding.get();

        Rectangle rectangle = new Rectangle(shape.getBounds().getX() * currentScale, shape.getBounds().getY() * currentScale, shape.getBounds().getWidth() * currentScale, shape.getBounds().getHeight() * currentScale);
        //rectangle.setFill(javafx.scene.paint.Color.rgb(200, 200, 200, 1.0));
        rectangle.setFill(javafx.scene.paint.Color.rgb(200, 200, 200, 0.1));
        rectangle.setStyle("-fx-background-color:transparent;");//transparent
        scaleBinding.addListener((observable, oldValue, newValue) -> {
            rectangle.setX(shape.getBounds().getX() * newValue.doubleValue());
            rectangle.setY(shape.getBounds().getY() * newValue.doubleValue());
            rectangle.setWidth(shape.getBounds().getWidth() * newValue.doubleValue());
            rectangle.setHeight(shape.getBounds().getHeight() * newValue.doubleValue());
        });

        rectangle.setUserData(pair);
        rectangle.strokeProperty().bind(Bindings.createObjectBinding(() -> (dynArea.selectedProperty().get() ? strokeSelected : stroke), dynArea.selectedProperty()));
        rectangle.strokeWidthProperty().bind(scaleBinding.multiply(2));

        return rectangle;
    }

    protected void initAreasRectangle(java.util.List<DynamicArea> dynamicAreas, StackImageView svAreas, final javafx.scene.paint.Paint strokeSelected, @Annotations.Nullable Consumer<java.util.List<Rectangle>> callback) {
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

        Pane rectangles = new Pane();
        rectangles.setUserData("areas");

        AtomicInteger colorCounter = new AtomicInteger(10);

        DoubleBinding scaleBinding = svAreas.getScaleBinding();
        dynamicAreas.forEach(dynamicArea -> {

            Color groupColor = colors.get((colorCounter.getAndIncrement() + cOffset) % colors.size());
            javafx.scene.paint.Paint stroke = javafx.scene.paint.Color.rgb(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), ((double) groupColor.getAlpha()) / 255);

            CellEvent event = dynamicArea.getAreas().get(current);
            if (event != null) {


                if (event instanceof CellEvent.CellAliveEvent) {
                    CellShape shape = ((CellEvent.CellAliveEvent) event).getTarget();
                    rectangles.getChildren().add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof CellEvent.CellSplitEvent) {
                    List<CellShape> shapes = ((CellEvent.CellSplitEvent) event).getTarget();
                    shapes.forEach(shape -> rectangles.getChildren().
                            add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea),
                                    stroke, strokeSelected, scaleBinding))
                    );
                } else if (event instanceof CellEvent.CellDeTouchEvent) {
                    List<CellShape> shapes = ((CellEvent.CellDeTouchEvent) event).getTarget();
                    shapes.forEach(shape -> rectangles.getChildren().
                            add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea),
                                    stroke, strokeSelected, scaleBinding))
                    );
                } else if (event instanceof CellEvent.CellSingleSourceEvent) {//start and end event
                    CellShape shape = ((CellEvent.CellSingleSourceEvent) event).getSource();
                    rectangles.getChildren().add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof CellEvent.CellFusionEvent) {
                    CellShape shape = ((CellEvent.CellFusionEvent) event).getTarget();
                    rectangles.getChildren().add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                } else if (event instanceof CellEvent.CellTouchEvent) {
                    CellShape shape = ((CellEvent.CellTouchEvent) event).getTarget();
                    rectangles.getChildren().add(getAreaRectangle(new javafx.util.Pair<>(shape, dynamicArea), stroke, strokeSelected, scaleBinding));
                }
            }

        });

        rectangles.maxHeightProperty().bind(svAreas.getStackPane().heightProperty());
        rectangles.maxWidthProperty().bind(svAreas.getStackPane().widthProperty());
        rectangles.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(rectangles, Pos.TOP_LEFT);
        Platform.runLater(() -> {
            svAreas.getStackPane().getChildren().add(rectangles);
            if (callback != null)
                callback.accept(rectangles.getChildren().stream().map(i -> (Rectangle) i).collect(Collectors.toList()));
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
                    AreaCsvExport.exportDynamicAreaCsv(tracking, getMainController().getModel().getStackModel().getStackOrig(), csvFile, getMainController().getModel().getProjectData().getSettings());

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
                    AreaCsvExport.exportDynamicAreaCsvGroupedByTime(tracking, getMainController().getModel().getStackModel().getStackOrig(), csvFile, getMainController().getModel().getProjectData().getSettings());

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
        Settings dp = getMainController().getModel().getProjectData().getSettings();
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
                java.util.List<DynamicArea> filtered = tracking.filterUniqueAreas(chkMin, minLength, chkMax, maxLength);

                //iterate over all images in stack
                IntStream.range(0, getMainController().getModel().getStackModel().getStackOrig().getSize()).forEach(current -> {
                    //retrieve current stack entry
                    Entry entry = getMainController().getModel().getStackModel().getStackOrig().getEntryList().get(current);
                    //get the orig image as buffered image
                    BufferedImage bi = ImageExporter.getBufferedImage(entry.getProcessor());
                    //group the filtered DynamicFilaments by their lifespan

                    AtomicInteger colorCounter = new AtomicInteger(10);
                    //add areas
                    filtered.forEach(dynamicArea -> {
                        Color groupColor = colors.get((colorCounter.getAndIncrement() + cOffset) % colors.size());
                        CellEvent event = dynamicArea.getAreas().get(current);
                        if (event != null) {
                            if (event instanceof CellEvent.CellAliveEvent) {
                                CellShape shape = ((CellEvent.CellAliveEvent) event).getTarget();
                                ImageExporter.addArea(bi, shape, groupColor, true);
                            } else if (event instanceof CellEvent.CellSplitEvent) {
                                ((CellEvent.CellSplitEvent) event).getTarget().forEach(shape -> ImageExporter.addArea(bi, shape, groupColor, true));
                            } else if (event instanceof CellEvent.CellDeTouchEvent) {
                                ((CellEvent.CellDeTouchEvent) event).getTarget().forEach(shape -> ImageExporter.addArea(bi, shape, groupColor, true));
                            } else if (event instanceof CellEvent.CellSingleSourceEvent) {//start and end event
                                CellShape shape = ((CellEvent.CellSingleSourceEvent) event).getSource();
                                ImageExporter.addArea(bi, shape, groupColor, true);
                            } else if (event instanceof CellEvent.CellFusionEvent) {
                                CellShape shape = ((CellEvent.CellFusionEvent) event).getTarget();
                                ImageExporter.addArea(bi, shape, groupColor, true);
                            } else if (event instanceof CellEvent.CellTouchEvent) {
                                CellShape shape = ((CellEvent.CellTouchEvent) event).getTarget();
                                ImageExporter.addArea(bi, shape, groupColor, true);
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
