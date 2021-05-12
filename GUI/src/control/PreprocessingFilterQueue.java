package control;

import javafx.application.Platform;
import javafx.beans.property.*;
import fx.custom.SliderSpinner;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import core.FilterQueue;
import util.ProcessingUtils;
import core.cell.CellPlugin;
import core.cell.plugins.CellPluginBenjamin;
import core.cell.plugins.CellPluginSimple;
import core.cell.plugins.CellPluginThresholding;
import evaluation.Evaluator;
import core.image.BinaryImage;
import core.image.ImageWrapper;
import core.settings.Pre;
import core.settings.Config;
import utils.FilterUIFactory;


import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class PreprocessingFilterQueue extends AbstractControl {


    public enum FQUseCase {filament, focalAdhesion}


    private FilterQueue model;

    @FXML
    private Label lQualityScore;

    @FXML
    private SliderSpinner sMinArea;

    @FXML
    private SliderSpinner sScale;

    @FXML
    private SliderSpinner sMinContrast;

    @FXML
    private VBox boxFilters;

    @FXML
    private ComboBox<String> cbFilters;
    @FXML
    private ComboBox<CellPlugin> cbArea;


    @FXML
    private VBox containerMinContrast;
    @FXML
    private HBox containerQualityScore;
    @FXML
    private VBox containerAreaBCAdjuster;
    @FXML
    private VBox boxAddFilters;

    private ObjectProperty<TitledPane> draggingTab;

    public PreprocessingFilterQueue() {
        draggingTab = new SimpleObjectProperty<>();
        model = new FilterQueue();
    }

    @FXML
    private void initialize() {
        sScale.setValue(1d);
    }

    public void setUseCase(FQUseCase useCase) {
        Objects.requireNonNull(useCase, "PreprocessingFilterQueue exception: setUseCase(useCase==null)");
        containerMinContrast.managedProperty().bind(containerMinContrast.visibleProperty());
        containerQualityScore.managedProperty().bind(containerQualityScore.visibleProperty());
        containerAreaBCAdjuster.managedProperty().bind(containerAreaBCAdjuster.visibleProperty());
        if (useCase.equals(FQUseCase.filament)) {
            containerMinContrast.setVisible(true);
            containerQualityScore.setVisible(true);
            containerAreaBCAdjuster.setVisible(true);
            initialize(ProcessingUtils.getDefaultPreprocessingFilterQueue(getMainController().getModel().getProjectData().getSettings()));
        } else if (useCase.equals(FQUseCase.focalAdhesion)) {
            containerMinContrast.setVisible(false);
            containerQualityScore.setVisible(false);
            containerAreaBCAdjuster.setVisible(false);
        }
    }


    public void setApplicableFilters(ObservableList<String> applicableFilters) {
        //store variable, for all filters load class, create a ui button, add "onclick"
        //add onChange to observableList and update buttons
        boxAddFilters.getChildren().clear();

        ObjectProperty<HBox> hbox = new SimpleObjectProperty<>();
        AtomicInteger cnt = new AtomicInteger(0);

        Consumer<? super String> foreachAction = (clsName) -> {
            try {
                if (cnt.get() % 2 == 0) {
                    hbox.set(new HBox());
                    boxAddFilters.getChildren().add(hbox.get());
                }
                hbox.get().getChildren().add(FilterUIFactory.getButtonForFilter(clsName, boxFilters,
                        draggingTab, getMainController().getModel().getProjectData().getSettings(),
                        getModel())
                );
                cnt.incrementAndGet();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
        applicableFilters.forEach(foreachAction);
        applicableFilters.addListener((ListChangeListener<String>) c -> {
            //just do the whole list again
            boxAddFilters.getChildren().clear();
            c.getList().forEach(foreachAction);
        });
    }


    /**
     * This method should initialize the gui element with all content from the filter queue
     * @param queue
     */
    public void initialize(FilterQueue queue) {
        boxFilters.getChildren().clear();
        setModel(queue);
        getModel().getFilters().forEach(filter -> {
            try {
                FilterUIFactory.generateFilterUI(filter, boxFilters, queue, draggingTab, true, getMainController().getModel().getProjectData().getSettings());
            } catch (Exception e) {
                e.printStackTrace();
                getMainController().addDebugMessage(e);
            }
        });
    }



    @FXML
    private void onLoadFilterQueue(ActionEvent event) {
        String selected = cbFilters.valueProperty().get();
        if (selected == null) {
            getMainController().addDebugMessage("No Filter selected");
            return;
        }
        File filtersDirectory = Config.getInstance().getFiltersDirectory();
        if (filtersDirectory == null) {
            getMainController().addDebugMessage("Filters Directory not found/could not be created");
            return;
        }
        File inputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + selected);
        if (!inputFile.exists()) {
            getMainController().addDebugMessage("Selected Filter-File doesn't exist");
            return;
        }

        try {
            XMLDecoder decoder = new XMLDecoder(new FileInputStream(inputFile));
            FilterQueue queue = (FilterQueue) decoder.readObject();
            initialize(queue);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            getMainController().addDebugMessage(e);
        }


    }

    @FXML
    private void onSaveFilterQueue(ActionEvent event) {
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Save FilterQueue");
        textInputDialog.setHeaderText("Type in the name of your FilterQueue");
        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent()) {
            File filtersDirectory = Config.getInstance().getFiltersDirectory();
            if (filtersDirectory == null) {
                getMainController().addDebugMessage("Filters Directory not found/could not be created");
                return;
            }
            File outputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + result.get() + ".xml");
            try {
                XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
                encoder.writeObject(model);
                encoder.flush();
                encoder.close();
                initCbFilters();
            } catch (FileNotFoundException e) {
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }
        }
    }

    public int getMinContrast() {
        if (sMinContrast.getValue() == null) {
            if (getMainController() != null && getMainController().getModel() != null)
                return getMainController().getModel().getProjectData().getSettings().getValue(Pre.min_range);
            return 0;//get default value
        }
        return sMinContrast.getValue().intValue();
    }

    public DoubleProperty scaleProperty() {
        return sScale.valueProperty();
    }

    public DoubleProperty minContrastProperty() {
        return sMinContrast.valueProperty();
    }


    /**
     * Applies the FilterQueue on the ImageWrapper passed.
     *
     * @param wrapper
     */
    public void apply(ImageWrapper wrapper) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateProgress(0, 100);
                AtomicInteger currentProgress = new AtomicInteger(0);
                model.run(wrapper, (f) -> {
                    int progress = Math.round(f * 100);
                    currentProgress.set(currentProgress.get() + progress);
                    updateProgress(currentProgress.get(), 100);
                });
                updateProgress(100, 100);
                wrapper.notifyListeners();
                    return null;
                }
            };
            getMainController().runAsync(task);
    }


    private void initCbFilters() {
        cbFilters.getItems().clear();
        File filtersDirectory = Config.getInstance().getFiltersDirectory();
        if (filtersDirectory != null) {
            File[] files = filtersDirectory.listFiles();
            if (files != null) Arrays.stream(files).forEach(f -> cbFilters.getItems().add(f.getName()));
        }
    }


    /**
     * Initializes the Area Combo box, since its also used in Project this is made static and public
     *
     * @param cbArea comboBox which should get initialized
     * @param setter function interface for setting the value
     * @param getter function interface for getting the value
     */
    public static void initCbArea(ComboBox<CellPlugin> cbArea, Consumer<CellPlugin> setter, Supplier<CellPlugin> getter, MainControl mainControl) {
        ObservableList<CellPlugin> items = FXCollections.observableArrayList();
        items.add(new CellPluginSimple());
        items.add(new CellPluginBenjamin());

        cbArea.setItems(items);
        cbArea.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                setter.accept(newValue);
            }
        });
        if (mainControl.getModel().getProjectData().getPlugin() != null)
            cbArea.getSelectionModel().select(getter.get());
        else cbArea.getSelectionModel().selectLast();
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        sMinContrast.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(Pre.min_range));
        sScale.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(Pre.scale));
        initCbFilters();
        initCbArea(cbArea, (cp) -> getMainController().getModel().getProjectData().setPlugin(cp), () -> getMainController().getModel().getProjectData().getPlugin(), getMainController());

        sMinArea.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(Pre.min_area));


    }


    public FilterQueue getModel() {
        return model;
    }

    public void setModel(FilterQueue model) {
        if (getMainController() != null && getMainController().getModel() != null) {
            getMainController().getModel().setFilterQueueModel(model);
        }
        this.model = model;
    }

    public double getScale() {
        return 1d / sScale.getValue().intValue();
    }

    public void onOpenBCAdjuster(ActionEvent actionEvent) {
        getMainController().openPopUp("/view/PopUpBCAdjuster.fxml", "Brightness-/Contrast-Adjuster", (t) -> {
            t.setMainController(getMainController(), this);
        });
    }


    @FXML
    private void onUpdateArea(ActionEvent actionEvent) {
        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                try {
                    getMainController().getModel().getStackModel().getStackOrig().initializeShape(cbArea.getSelectionModel().getSelectedItem()).get();
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> getMainController().addDebugMessage(e));
                }
                getMainController().getModel().getStackModel().getStackOrig().notifyListeners();
                updateProgress(100, 100);
                return null;
            }
        });

    }


    @FXML
    private void onEvaluateArea(ActionEvent actionEvent) {
        getMainController().<Evaluation>openPopUp("/view/Evaluation.fxml", "Evaluate Area's", (control) -> {
            control.setToEval(getMainController().getModel().getStackModel().getStackOrig(),
                    (i) -> {
                        if (getMainController().getModel() == null || getMainController().getModel().getStackModel() == null ||
                                getMainController().getModel().getStackModel().getStackOrig() == null ||
                                getMainController().getModel().getStackModel().getStackOrig().getEntryList().size() == 0 ||
                                getMainController().getModel().getStackModel().getStackOrig().getEntryList().get(i).getShape() == null ||
                                getMainController().getModel().getStackModel().getStackOrig().getEntryList().get(i).getShape().getAggregatedArea() == null) {
                            return null;
                        }
                        BinaryImage aggArea = (BinaryImage) getMainController().getModel().getStackModel().getStackOrig().getEntryList().get(i).getShape().getAggregatedArea().clone();
                        if (aggArea != null) aggArea.exitMemoryState();
                        return aggArea;
                    },
                    () -> Evaluator.ShapeType.dotLike);

        });


    }


    @FXML
    private void onOpenAreaTracking(ActionEvent actionEvent) {
        getMainController().openPopUp("/view/PopUpCellEventTracking.fxml",
                "Cell-Event Tracking", (control) -> {


                });

    }


}
