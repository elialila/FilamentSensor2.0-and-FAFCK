package control;

import core.Const;
import core.filaments.AbstractFilament;
import core.filaments.FilamentChain;
import core.settings.Settings;
import core.settings.SFTracking;
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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import tracking.filament.DataTracking;
import tracking.filament.DynamicFilament;
import core.image.Entry;
import util.*;
import util.io.FilamentCsvExport;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class PopUpSingleFilamentTracking extends AbstractControl {
    @FXML
    private TextArea taSingleFilamentDebug;
    @FXML
    private TableView<DynamicFilament> tableFilaments;
    @FXML
    private TableColumn<DynamicFilament, Boolean> columnKeep;
    @FXML
    private TableColumn<DynamicFilament, Integer> columnBirth;
    @FXML
    private TableColumn<DynamicFilament, Integer> columnDeath;
    @FXML
    private TableColumn<DynamicFilament, Double> columnLength;
    @FXML
    private CheckBox chkMaxTimeExist;
    @FXML
    private SliderSpinner sMaxExistFilament;
    @FXML
    private RadioButton radioParam;
    @FXML
    private RadioButton radioFilaments;
    @FXML
    private VBox vBoxParameters;
    @FXML
    private VBox vBoxFilaments;
    @FXML
    private ToggleGroup tgFilter;

    @FXML
    private StackImageView ivFilaments;
    @FXML
    private Text tImageSize;
    @FXML
    private Text tUnfilteredTimeLines;
    @FXML
    private Text tFilteredTimeLines;
    @FXML
    private CheckBox chkMinTimeExist;
    @FXML
    private SliderSpinner sMinExistFilament;
    @FXML
    private StackImageView ivOverview;
    @FXML
    private ProgressBar pbProgress;
    @FXML
    private SliderSpinner sMaxDist;
    @FXML
    private SliderSpinner sFactorAngle;
    @FXML
    private SliderSpinner sFactorLength;
    @FXML
    private SliderSpinner sLength;

    private final AtomicBoolean updatePending;
    private final AtomicBoolean isUpdating;

    private DataTracking tracking;
    private List<Color> colors;
    private final int cOffset = 634;//just a random number to shift the starting colors


    public PopUpSingleFilamentTracking() {
        updatePending = new AtomicBoolean(false);
        isUpdating = new AtomicBoolean(false);
        tracking = new DataTracking();
    }

    @FXML
    private void initialize() {
        colors = MixedUtils.getColors();
        vBoxParameters.visibleProperty().bind(tgFilter.selectedToggleProperty().isEqualTo(radioParam));
        vBoxFilaments.visibleProperty().bind(tgFilter.selectedToggleProperty().isEqualTo(radioFilaments));
        tgFilter.selectToggle(radioParam);
        initTable();
    }

    private void message(String message) {
        Platform.runLater(() -> taSingleFilamentDebug.appendText(message + "\n"));
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        sMinExistFilament.initRanges(1, getMainController().getModel().getStackModel().getSize(), 1);
        sMaxExistFilament.initRanges(1, getMainController().getModel().getStackModel().getSize(), 1);
        getMainController().getModel().getProjectData().getSettings().setProperty(SFTracking.existsInMax, getMainController().getModel().getStackModel().getSize());
        ivFilaments.setMax(getMainController().getModel().getStackModel().getSize());


        sMaxDist.setMax(getMainController().getModel().getStackModel().getStackOrig().getWidth());
        sLength.setMax(1000);//just for now, maybe some other value in the future

        sMaxDist.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.max_dist));
        sFactorAngle.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.factor_angle));
        sFactorLength.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.factor_length));
        sLength.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.length));
        chkMinTimeExist.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(SFTracking.chkExistsInMin));
        chkMaxTimeExist.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(SFTracking.chkExistsInMax));

        sMinExistFilament.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.existsInMin));
        sMaxExistFilament.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsDouble(SFTracking.existsInMax));


        ChangeListener<Number> listener = (observable, oldValue, newValue) -> {
            update(true);
        };
        sMaxDist.valueProperty().addListener(listener);
        sFactorAngle.valueProperty().addListener(listener);
        sFactorLength.valueProperty().addListener(listener);
        sLength.valueProperty().addListener(listener);


        //this does not need an update, actually it just has to redo the image
        sMinExistFilament.valueProperty().addListener((observableValue, number, t1) -> update(false));
        chkMinTimeExist.selectedProperty().addListener((observableValue, aBoolean, t1) -> update(false));

        sMaxExistFilament.valueProperty().addListener((observableValue, number, t1) -> update(false));
        chkMaxTimeExist.selectedProperty().addListener((observableValue, aBoolean, t1) -> update(false));

        ivFilaments.currentProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null) getMainController().getModel().getStackModel().getStackOrig().notifyListeners();
        });

        getMainController().getModel().getStackModel().getStackOrig().addListener(((observableValue, aBoolean, t1) -> {
            if (tracking.getTrackedFilaments() != null && tracking.getTrackedFilaments().size() > 0) {
                int current = ivFilaments.getCurrent();
                BufferedImage bi = getMainController().getModel().getStackModel().getStackOrig().getImage(current);
                Settings dp = getMainController().getModel().getProjectData().getSettings();
                final boolean chkMin = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
                final boolean chkMax = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
                final int minLength = dp.getValue(SFTracking.existsInMin);
                final int maxLength = dp.getValue(SFTracking.existsInMax);
                ivFilaments.setImage(SwingFXUtils.toFXImage(bi, null));

                //linear gradient used for selected color, to be able to differentiate it from normal color
                Stop[] stops = new Stop[]{new Stop(0, javafx.scene.paint.Color.GOLD), new Stop(1, javafx.scene.paint.Color.RED)};
                LinearGradient selected = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);

                initFilamentsPolyline(tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength),
                        ivFilaments,
                        javafx.scene.paint.Color.DARKRED,
                        javafx.scene.paint.Color.GREEN,
                        selected,//javafx.scene.paint.Color.GOLD,
                        null);

                /*tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength).stream().
                        collect(Collectors.groupingBy(DynamicFilament::getLength)).forEach((key, value) -> {
                    Color groupColor = colors.get((key.intValue() + cOffset) % colors.size());
                    List<AbstractFilament> filaments = value.stream().filter(df -> df.getFilaments().get(current - 1) != null).flatMap(df -> df.getFilaments().get(current - 1).getFilaments().stream()).collect(Collectors.toList());
                    ImageExporter.addFilaments(bi, filaments, groupColor);
                });
                ivFilaments.setImage(SwingFXUtils.toFXImage(bi,null));*/
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
        /*if (!isUpdating.get()) {
            runTracking(reCalc).thenAccept((v) -> {
                if (updatePending.get()) {
                    updatePending.set(false);
                    update(reCalc);
                }
            });
        } else {
            updatePending.set(true);
        }*/
    }

    private void initTable() {
        tableFilaments.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableFilaments.setEditable(true);
        columnKeep.setCellValueFactory(cellData -> cellData.getValue().keepProperty());
        columnKeep.setCellFactory(CheckBoxTableCell.forTableColumn(columnKeep));

        //columnBirth.setCellValueFactory(new PropertyValueFactory<>("birth"));
        //columnDeath.setCellValueFactory(new PropertyValueFactory<>("death"));
        //columnLength.setCellValueFactory(new PropertyValueFactory<>("length"));

        columnBirth.setCellValueFactory(cellData -> cellData.getValue().birthProperty().asObject());
        columnDeath.setCellValueFactory(cellData -> cellData.getValue().deathProperty().asObject());
        columnLength.setCellValueFactory(cellData -> cellData.getValue().lengthProperty().asObject());

        //for highlighting filaments
        tableFilaments.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
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
        final double maxDist = dp.getValueAsDouble(SFTracking.max_dist);
        final double factorAngle = dp.getValueAsDouble(SFTracking.factor_angle);
        final double factorLength = dp.getValueAsDouble(SFTracking.factor_length);
        final int length = dp.getValue(SFTracking.length);

        final boolean chkMinLength = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
        final int minLength = dp.getValue(SFTracking.existsInMin);

        final boolean chkMax = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
        final int maxLength = dp.getValue(SFTracking.existsInMax);

        final boolean combineMultiMatches = dp.getValueAsBoolean(SFTracking.combineMultiMatches);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    long time = System.currentTimeMillis();
                    boolean solitarySuccess = true;
                    boolean trackingSuccess = true;
                    if (reCalc) tracking.loadData(getMainController().getModel().getStackModel().getStackOrig());
                    updateProgress(10, 100);
                    if (reCalc) {
                        solitarySuccess = tracking.findSolitaryFilaments(maxDist, factorAngle, factorLength, length);
                    }
                    if (solitarySuccess) {

                        updateProgress(50, 100);
                        if (reCalc) {
                            trackingSuccess = tracking.trackFilaments(maxDist, factorAngle, factorLength, length, combineMultiMatches);
                        }
                        if (trackingSuccess) {
                            message("Filament Tracking Done!");

                            //link keep properties
                            //links each filament from filamentchain keep property with the dynamic filament (if dynamic filament keep is set to false all childs will do the same)
                            if (tracking.getTrackedFilaments().size() > 0) {

                                tracking.getTrackedFilaments().forEach(dynamicFilament -> {
                                    dynamicFilament.keepProperty().addListener((observableValue, oldValue, newValue) -> {
                                        dynamicFilament.getFilaments().values().stream().flatMap(f -> ((FilamentChain) f).getFilaments().stream()).forEach(fil -> fil.setKeep(newValue));
                                    });
                                });
                                //end link keep properties


                                BufferedImage image = ImageExporter.getFilamentTrackingOverview(tracking, chkMinLength, minLength, chkMax, maxLength);
                                Platform.runLater(() -> {
                                    ivOverview.setImage(SwingFXUtils.toFXImage(image, null));
                                    tUnfilteredTimeLines.setText(Integer.toString(tracking.getTrackedFilaments().size()));
                                    tFilteredTimeLines.setText(Integer.toString(tracking.filterTrackedFilaments(chkMinLength, minLength, chkMax, maxLength).size()));
                                    tImageSize.setText(image.getWidth() + " x " + image.getHeight());
                                    //init table
                                    tableFilaments.setItems(FXCollections.observableList(tracking.filterTrackedFilaments(chkMinLength, minLength, chkMax, maxLength)));
                                    //update image
                                    getMainController().getModel().getStackModel().getStackOrig().notifyListeners();

                                });
                            } else {
                                message("Press Track first - Time slot filters do not run calculations");
                            }
                        } else {
                            message("FilamentTracking not succeeded");
                        }
                    } else {
                        message("FilamentTracking not succeeded");
                    }
                    message("Filament-Tracking Time taken:" + ((double) System.currentTimeMillis() - time) / 1000 + "s");
                } catch (Exception e) {
                    e.printStackTrace();
                    message("FilamentTracking Finished with Exception");
                }
                updateProgress(100, 100);
                isUpdating.set(false);
                return null;
            }
        };
        Platform.runLater(() -> pbProgress.progressProperty().bind(task.progressProperty()));//since this can be called
        //on non fx thread wrap it
        message("Start Filament Tracking");
        return CompletableFuture.runAsync(task);


    }


    /**
     * @param dynamicFilaments
     * @param svFilaments
     * @param strokeKeepFalse
     * @param strokeVerified
     * @param strokeSelected
     * @param callback
     */
    protected void initFilamentsPolyline(List<DynamicFilament> dynamicFilaments, StackImageView svFilaments, final javafx.scene.paint.Paint strokeKeepFalse, final javafx.scene.paint.Paint strokeVerified, final javafx.scene.paint.Paint strokeSelected, @Annotations.Nullable Consumer<List<Polyline>> callback) {

        final int current = ivFilaments.getCurrent();

        Platform.runLater(() ->
                svFilaments.getStackPane().getChildren().
                        removeAll(
                                svFilaments.getStackPane().getChildren().
                                        stream().filter(e -> e instanceof Pane && Objects.equals(e.getUserData(), "filaments")).collect(Collectors.toList())
                        )
        );
        if (dynamicFilaments == null ||
                dynamicFilaments.size() == 0) return;

        Pane polylines = new Pane();
        polylines.setUserData("filaments");

        dynamicFilaments.stream().
                collect(Collectors.groupingBy(DynamicFilament::getLength)).forEach((key, value) -> {
            Color groupColor = colors.get((key.intValue() + cOffset) % colors.size());
            javafx.scene.paint.Paint stroke = javafx.scene.paint.Color.rgb(groupColor.getRed(), groupColor.getGreen(), groupColor.getBlue(), ((double) groupColor.getAlpha()) / 255);

            List<Pair<DynamicFilament, AbstractFilament>> filaments = value.stream().filter(df -> df.getFilaments().get(current - 1) != null).flatMap(df -> ((FilamentChain) df.getFilaments().get(current - 1)).getFilaments().stream().map(fil -> new Pair<>(df, fil))).collect(Collectors.toList());// new Pair<>(df, ((FilamentChain)df.getFilaments().get(current - 1)).getFilaments())).collect(Collectors.toList());

            DoubleBinding scaleBinding = svFilaments.getScaleBinding();

            for (Pair<DynamicFilament, AbstractFilament> pair : filaments) {

                DynamicFilament dynFilament = pair.getKey();
                AbstractFilament fil = pair.getValue();


                //init
                double[] points = new double[fil.getPoints().size() * 2];
                List<Point> pointList = fil.getPoints();
                for (int i = 0; i < pointList.size(); i++) {
                    points[2 * i] = pointList.get(i).getX() * scaleBinding.get();
                    points[2 * i + 1] = pointList.get(i).getY() * scaleBinding.get();
                }
                Polyline filament = new Polyline(points);
                //scaleBinding listener, if changes appear -> redo the list
                //apply zoom
                scaleBinding.addListener((observable, oldValue, newValue) -> {
                    filament.getPoints().clear();
                    AbstractFilament abstractFilament = ((Pair<DynamicFilament, AbstractFilament>) filament.getUserData()).getValue();
                    abstractFilament.getPoints().forEach(p -> {
                        filament.getPoints().add(p.getX() * newValue.doubleValue());
                        filament.getPoints().add(p.getY() * newValue.doubleValue());
                    });
                });


                filament.setUserData(pair);
                filament.strokeProperty().bind(Bindings.createObjectBinding(() ->
                                dynFilament.selectedProperty().get() ? strokeSelected : dynFilament.keepProperty().get() ?
                                        ((fil.verifiedProperty().get()) ? strokeVerified : stroke) : strokeKeepFalse,
                        dynFilament.selectedProperty(), dynFilament.keepProperty(), fil.verifiedProperty())
                );
                filament.setOnMouseClicked(mouseEvent -> {
                    if (mouseEvent.getSource() instanceof Polyline) {//onclick funktioniert
                        //System.out.println("PopUpSingleFilamentTracking::getPolylines() --- onClick()");
                        Polyline source = (Polyline) mouseEvent.getSource();
                        if (source.getUserData() instanceof Pair) {
                            Pair<DynamicFilament, AbstractFilament> userData = (Pair<DynamicFilament, AbstractFilament>) source.getUserData();
                            userData.getKey().setKeep(!userData.getKey().isKeep());
                            //do we want to set all of the filaments keep state too?
                            //((Pair<DynamicFilament, AbstractFilament>) source.getUserData()).getKey().setKeep(!((Pair<DynamicFilament, AbstractFilament>) source.getUserData()).getKey().isKeep());
                            //((AbstractFilament) source.getUserData()).setKeep(!((AbstractFilament) source.getUserData()).isKeep());
                            //flip the keep state
                        }
                    }
                });
                filament.strokeWidthProperty().bind(scaleBinding.multiply(fil.getWidth() / Const.MF));
                polylines.getChildren().add(filament);
            }

        });

        polylines.maxHeightProperty().bind(svFilaments.getStackPane().heightProperty());
        polylines.maxWidthProperty().bind(svFilaments.getStackPane().widthProperty());
        polylines.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(polylines, Pos.TOP_LEFT);
        Platform.runLater(() -> {
            svFilaments.getStackPane().getChildren().add(polylines);
            if (callback != null)
                callback.accept(polylines.getChildren().stream().map(i -> (Polyline) i).collect(Collectors.toList()));
        });//wrap it with javaFx thread
    }

    @FXML
    private void onExportDynFilamentCSV(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        File csvFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateProgress(10, 100);
                    FilamentCsvExport.exportDynamicFilamentCsv(tracking, getMainController().getModel().getStackModel().getStackOrig(), csvFile, getMainController().getModel().getProjectData().getSettings());

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
    private void onExportFilamentsCSV(ActionEvent event) {
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
                    FilamentCsvExport.exportDynamicFilamentCsvGroupedByTime(tracking, getMainController().getModel().getStackModel().getStackOrig(), csvFile, getMainController().getModel().getProjectData().getSettings());

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
    private void onExportDynFilamentsImage(ActionEvent event) {
        //this part should export the image with all dynamic filaments grouped by their lifespan(color coded)
        //image export should be 1 image per image/time slot
        Settings dp = getMainController().getModel().getProjectData().getSettings();
        final boolean chkMin = dp.getValueAsBoolean(SFTracking.chkExistsInMin);
        final boolean chkMax = dp.getValueAsBoolean(SFTracking.chkExistsInMax);
        final int minLength = dp.getValue(SFTracking.existsInMin);
        final int maxLength = dp.getValue(SFTracking.existsInMax);

        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        File imageDirectory = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                updateProgress(10, 100);
                List<DynamicFilament> filtered = tracking.filterTrackedFilaments(chkMin, minLength, chkMax, maxLength).
                        stream().filter(DynamicFilament::isKeep).collect(Collectors.toList());

                //@todo if one image has no filament, the single filament tracking will fail with exception
                //@todo fix that
                //iterate over all images in stack
                IntStream.range(0, getMainController().getModel().getStackModel().getStackOrig().getSize()).forEach(current -> {
                    //retrieve current stack entry
                    Entry entry = getMainController().getModel().getStackModel().getStackOrig().getEntryList().get(current);
                    //get the orig image as buffered image
                    BufferedImage bi = ImageExporter.getBufferedImage(entry.getProcessor());
                    //group the filtered DynamicFilaments by their lifespan
                    filtered.stream().collect(Collectors.groupingBy(DynamicFilament::getLength)).forEach((key, value) -> {
                        //retrieve a color for each lifespan (should be unique)
                        Color groupColor = colors.get((key.intValue() + cOffset) % colors.size());
                        //get all filaments with specific life span (current key of our grouped data) for the image that is processed
                        List<AbstractFilament> filaments = value.stream().filter(df -> df.getFilaments().get(current) != null).flatMap(df -> ((FilamentChain) df.getFilaments().get(current)).getFilaments().stream()).collect(Collectors.toList());// new Pair<>(df, ((FilamentChain)df.getFilaments().get(current - 1)).getFilaments())).collect(Collectors.toList());
                        //add filaments onto the image
                        ImageExporter.addFilaments(bi, filaments, groupColor);
                    });
                    try {
                        //export the image with all filaments added
                        ImageExporter.exportImage(bi, IOUtils.getOutFileFromImageFile(new File(entry.getPath()), imageDirectory, ".png", "cc_sf_filament"));
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
