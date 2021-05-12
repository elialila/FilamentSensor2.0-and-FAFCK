package control;


import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import core.settings.Settings;
import util.Annotations.NotNull;
import fx.custom.Area;
import fx.custom.StackImageView;
import interfaces.IFilamentUpdater;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.util.Pair;
import model.ImageDetailStackModel;
import core.cell.CellShape;
import core.image.BinaryImage;
import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.Pre;
import util.PointUtils;
import util.ProcessingUtils;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class ImageDetailStack extends AbstractControl implements IFilamentUpdater {

    //region FXML defines
    @FXML
    private SplitPane sPVertical;
    @FXML
    private SplitPane sPOrigPre;
    @FXML
    private SplitPane sPLineFilaments;
    @FXML
    private StackImageView svOriginal;
    @FXML
    private StackImageView svPreProcessed;
    @FXML
    private StackImageView svLineSensor;
    @FXML
    private StackImageView svFilaments;
    @FXML
    private HBox containerZoom;
    @FXML
    private VBox containerStackScroller;
    @FXML
    private SplitPane rootSplitPane;
    @FXML
    private VBox imgViewParentVBox;
    @FXML
    private Pane preprocessingFilterQueue;
    @FXML
    private PreprocessingFilterQueue preprocessingFilterQueueController;
    @FXML
    private LineSensor lineSensorController;
    @FXML
    private Pane lineSensor;
    @FXML
    private Filaments filamentsController;
    @FXML
    private Pane filaments;
    @FXML
    private Area area;
    @FXML
    private ToggleGroup tgFilter;
    @FXML
    private ToggleGroup tgShowMultipleImages;
    @FXML
    private ComboBox<String> cbShownImageStack;
    @FXML
    private CheckBox chkIncludeArea;
    @FXML
    private CheckBox chkPreview;
    //@FXML
    //private Polygon areaPolygon;
    //@FXML
    //private Pane panePolygon;
    //endregion

    //region defines
    private ImageDetailStackModel model;
    private IntegerProperty zoomProperty;
    private AtomicBoolean previewRunning;
    private AtomicBoolean updatePending;
    private Set<Pair<Integer, StackImageView>> selectedImageViews;
    //endregion

    public ImageDetailStack() {
        zoomProperty = new SimpleIntegerProperty(100);
        selectedImageViews = new HashSet<>();
        previewRunning = new AtomicBoolean(false);
        updatePending = new AtomicBoolean(false);
        //areaPolygon=new Polygon();
    }


    public ImageDetailStackModel getModel() {
        return model;
    }

    public void setModel(ImageDetailStackModel model) {
        this.model = model;
    }

    private void initSplitPaneDividers() {

        ChangeListener<Boolean> verticalChange = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                double divVertical = 0.5;
                if (svOriginal.isVisible() || svPreProcessed.isVisible()) divVertical += 0.5;
                if (svLineSensor.isVisible() || svFilaments.isVisible()) divVertical -= 0.5;
                sPVertical.setDividerPosition(0, divVertical);
            }
        };
        svOriginal.visibleProperty().addListener(verticalChange);
        svPreProcessed.visibleProperty().addListener(verticalChange);
        svLineSensor.visibleProperty().addListener(verticalChange);
        svFilaments.visibleProperty().addListener(verticalChange);

        ChangeListener<Boolean> horizontalChange1 = (observable, oldValue, newValue) -> {
            double divHorizontal = 0.5;
            if (svOriginal.isVisible()) divHorizontal += 0.5;
            if (svPreProcessed.isVisible()) divHorizontal -= 0.5;
            sPOrigPre.setDividerPosition(0, divHorizontal);
        };
        svOriginal.visibleProperty().addListener(horizontalChange1);
        svPreProcessed.visibleProperty().addListener(horizontalChange1);

        ChangeListener<Boolean> horizontalChange2 = (observable, oldValue, newValue) -> {
            double divHorizontal = 0.5;
            if (svLineSensor.isVisible()) divHorizontal += 0.5;
            if (svFilaments.isVisible()) divHorizontal -= 0.5;
            sPLineFilaments.setDividerPosition(0, divHorizontal);
        };
        svLineSensor.visibleProperty().addListener(horizontalChange2);
        svFilaments.visibleProperty().addListener(horizontalChange2);

    }


    @FXML
    private void initialize() {
        initSplitPaneDividers();

        svOriginal.managedProperty().bind(svOriginal.visibleProperty());
        svPreProcessed.managedProperty().bind(svPreProcessed.visibleProperty());
        svLineSensor.managedProperty().bind(svLineSensor.visibleProperty());
        svFilaments.managedProperty().bind(svFilaments.visibleProperty());

        svOriginal.synchronize(svPreProcessed);
        svPreProcessed.synchronize(svLineSensor);
        svLineSensor.synchronize(svFilaments);

        preprocessingFilterQueue.managedProperty().bind(preprocessingFilterQueue.visibleProperty());
        lineSensor.managedProperty().bind(lineSensor.visibleProperty());
        filaments.managedProperty().bind(filaments.visibleProperty());
        area.managedProperty().bind(area.visibleProperty());

        initRadioButtons();
        initOptimalWidthSeparator();

        svOriginal.zoomProperty().bindBidirectional(zoomProperty);

        svOriginal.currentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && getModel() != null) {
                getModel().setCurrentImage(newValue.intValue());
                getModel().getStackOrig().notifyListeners();
                getModel().getStackPreprocessed().notifyListeners();
                getModel().getStackLineSensor().notifyListeners();
                getModel().getStackFilaments().notifyListeners();
            }
        });

        tgShowMultipleImages.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int value = Integer.parseInt(newValue.getUserData().toString());
                manageVisibleAndManagedStateRadio(value);
            }
        });

        cbShownImageStack.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                manageVisibleAndManagedStateComboBox(newValue);
            }
        });
        cbShownImageStack.getSelectionModel().select(1);

        containerStackScroller.getChildren().add(svOriginal.getVBoxScroller());
        containerZoom.getChildren().add(svOriginal.getHBoxZoom());

    }

    private void initOptimalWidthSeparator() {
        tgFilter.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            double widthLeft = 0;
            if (preprocessingFilterQueue.isVisible()) {
                widthLeft = preprocessingFilterQueue.getWidth();
            } else if (lineSensor.isVisible()) {
                widthLeft = lineSensor.getWidth();
            } else if (filaments.isVisible()) {
                widthLeft = filaments.getWidth();
            } else if (area.isVisible()) {
                widthLeft = area.getWidth();
            }
            if (widthLeft > 0) {
                double divider = (widthLeft + 20) / rootSplitPane.getWidth();
                rootSplitPane.setDividerPosition(0, divider);
            }
            //System.out.println("ImageDetailStack::initialize() ---" + newValue);
            //System.out.println("ImageDetailStack::initialize() --- area.isVisible=" + area.isVisible());
        });
    }


    private void initRadioButtons() {

        preprocessingFilterQueue.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null)
                return tgFilter.getSelectedToggle().getUserData().toString().equals("pre");
            return false;
        }, tgFilter.selectedToggleProperty()));

        lineSensor.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null)
                return tgFilter.getSelectedToggle().getUserData().toString().equals("bin");
            return false;
        }, tgFilter.selectedToggleProperty()));

        filaments.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null)
                return tgFilter.getSelectedToggle().getUserData().toString().equals("fil");
            return false;
        }, tgFilter.selectedToggleProperty()));

        area.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null) {
                //System.out.println("ImageDetailStack::initialize() --- userData=" + tgFilter.getSelectedToggle().getUserData().toString());
                return tgFilter.getSelectedToggle().getUserData().toString().equals("area");
            }
            return false;
        }, tgFilter.selectedToggleProperty()));


        Toggle toggle = tgFilter.getToggles().stream().filter(t -> t.getUserData().toString().equals("pre")).findAny().orElse(null);
        if (toggle != null) tgFilter.selectToggle(toggle);
    }


    private void manageVisibleAndManagedStateComboBox(String newValue) {
        if (tgShowMultipleImages.selectedToggleProperty().getValue().getUserData().equals("2")) return;
        selectedImageViews.clear();

        svOriginal.visibleProperty().setValue(false);
        svPreProcessed.visibleProperty().setValue(false);
        svLineSensor.visibleProperty().setValue(false);
        svFilaments.visibleProperty().setValue(false);

        if (tgShowMultipleImages.selectedToggleProperty().getValue().getUserData().equals("1")) {
            svOriginal.visibleProperty().setValue(true);
            selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwOriginal, svOriginal));
        }
        comboBoxSwitch(newValue);

    }

    private void comboBoxSwitch(String value) {
        switch (value) {
            case "Original":

                svOriginal.visibleProperty().setValue(true);
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwOriginal, svOriginal));
                break;
            case "Pre Processed":

                svPreProcessed.visibleProperty().setValue(true);
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwPreProcessed, svPreProcessed));
                break;
            case "Line Sensor":

                svLineSensor.visibleProperty().setValue(true);
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwLineSensor, svLineSensor));
                break;
            case "Filaments":

                svFilaments.visibleProperty().setValue(true);
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwFilaments, svFilaments));
                break;
            default:
                break;
        }
    }


    private void manageVisibleAndManagedStateRadio(int newValue) {
        //manage visible and managed state of the four scrollpanes and the two HBoxes
        selectedImageViews.clear();

        svOriginal.visibleProperty().setValue(false);
        svPreProcessed.visibleProperty().setValue(false);
        svLineSensor.visibleProperty().setValue(false);
        svFilaments.visibleProperty().setValue(false);

        switch (newValue) {
            case 1:
                //orig and selected
                svOriginal.visibleProperty().setValue(true);
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwOriginal, svOriginal));
                //no break here, because in case 1 comboboxswitch has to be done too
            case 0:
                //only selected
                String selected = cbShownImageStack.selectionModelProperty().get().selectedItemProperty().getValue();
                comboBoxSwitch(selected);
                break;
            case 2:
                svOriginal.visibleProperty().setValue(true);
                svPreProcessed.visibleProperty().setValue(true);
                svLineSensor.visibleProperty().setValue(true);
                svFilaments.visibleProperty().setValue(true);

                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwOriginal, svOriginal));
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwPreProcessed, svPreProcessed));
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwLineSensor, svLineSensor));
                selectedImageViews.add(new Pair<>(ImageDetailStackModel.iwFilaments, svFilaments));
                //set all visible/managed
                break;
            default:
                break;
        }
    }



    private void initImages() throws Exception {
        selectedImageViews.add(new Pair<>(1, svPreProcessed));

        getModel().includeAreaOutlineProperty().bind(chkIncludeArea.selectedProperty());
        getModel().previewProperty().bind(chkPreview.selectedProperty());

        svPreProcessed.setImage(getModel().getImage(ImageDetailStackModel.iwPreProcessed));
        svOriginal.setImage(getModel().getImage(ImageDetailStackModel.iwOriginal));
        svFilaments.setImage(getModel().getImage(ImageDetailStackModel.iwFilaments));
        svLineSensor.setImage(getModel().getImage(ImageDetailStackModel.iwLineSensor));

        svOriginal.setMin(1);
        svOriginal.setMax(getModel().getSize());

        getModel().getStackOrig().addListener((observable, oldValue, newValue) -> {
            if (getModel().getCurrentImage() < 1) return;//-1 is used for invalidate
            if (newValue != null && newValue) {
                try {
                    svOriginal.setImage(getModel().getImage(ImageDetailStackModel.iwOriginal));
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
            }
        });

        getModel().getStackPreprocessed().addListener((observable, oldValue, newValue) -> {
            if (getModel().getCurrentImage() < 1) return;//-1 is used for invalidate
            if (newValue != null && newValue) {
                try {
                    svPreProcessed.setImage(getModel().getImage(ImageDetailStackModel.iwPreProcessed));
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
            }
        });

        getModel().getStackLineSensor().addListener((observable, oldValue, newValue) -> {
            if (getModel().getCurrentImage() < 1) return;//-1 is used for invalidate
            if (newValue != null && newValue) {
                try {
                    svLineSensor.setImage(getModel().getImage(ImageDetailStackModel.iwLineSensor));
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
            }
        });

        getModel().getStackFilaments().addListener((observable, oldValue, newValue) -> {
            if (getModel().getCurrentImage() < 1) return;//-1 is used for invalidate
            if (newValue != null && newValue) {
                try {
                    svFilaments.setImage(getModel().getImage(ImageDetailStackModel.iwFilaments));
                    initFilamentsPolyline(getModel().getStackFilaments().getEntryList().get(getModel().getCurrentImage() - 1));
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
            }
        });
        getModel().setReady();//set ready, to run all callbacks on the completable future
        tgShowMultipleImages.getToggles().stream().filter(f -> f.getUserData().equals("2")).findFirst().ifPresent(t -> tgShowMultipleImages.selectToggle(t));

        svOriginal.setCurrent(-1);//reset to update view
        svOriginal.setCurrent(1);
        filamentsController.initTable(new ArrayList<>());
    }


    public void initFilamentsPolyline(Entry wrapperEntry) {
        IFilamentUpdater.initFilamentsPolyline(wrapperEntry, svFilaments,
                Filaments.defaultFilament,
                Filaments.defaultKeepFalse,
                Filaments.defaultVerified,
                Filaments.defaultSelected,
                filamentsController::initTable, getMainController().getModel().getProjectData().getSettings());
    }

    /**
     * For external call if everything is already set
     */
    public void setImages() {
        try {
            getModel().setCurrentImage(1);
            if (getModel().getStackOrig().getWorker() != null) getModel().getStackOrig().getWorker().get();
            getModel().getStackOrig().initializeShape(getMainController().getModel().getProjectData().getPlugin());
            initImages();
        } catch (Exception e) {
            getMainController().addDebugMessage(e);
            e.printStackTrace();
        }
    }


    public void setImages(List<File> files) throws Exception {
        if (files == null || files.size() == 0) throw new IllegalArgumentException("No ImageList specified.");
        if (getModel() != null && getModel().getStackOrig() == null && preprocessingFilterQueueController != null) {
            ImageWrapper imageWrapper;

            if (files.size() == 1) {//stack file loading
                imageWrapper = new ImageWrapper(files.get(0), getMainController().getModel().getProjectData().getSettings());
            } else {
                imageWrapper = new ImageWrapper(files, getMainController().getModel().getProjectData().getSettings());
            }
            imageWrapper.initializeShape(getMainController().getModel().getProjectData().getPlugin());//switch the hardcoded class with a parameter
            getModel().setStackOrig(imageWrapper);
            getModel().setStackPreprocessed(imageWrapper.clone());
            getModel().setStackLineSensor(imageWrapper.clone());
            getModel().setStackFilaments(imageWrapper);
            getModel().setCurrentImage(1);
            initImages();
        } else if (getModel() != null && preprocessingFilterQueueController != null) {
            try {
                initImages();
            } catch (Exception e) {
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }

        }
    }


    private void updatePreview() throws Exception {
        if (previewRunning.get()) {
            updatePending.set(true);
            return;
        }
        previewRunning.set(true);
        updatePending.set(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(1000);
                //debug print settings (setting which will be tested is min range)
                System.out.println("ImageDetailStack::updatePreview() --- minRange(fqController)=" + preprocessingFilterQueueController.getMinContrast());
                System.out.println("ImageDetailStack::updatePreview() --- minRange(settings)=" + getMainController().getModel().getProjectData().getSettings().getValue(Pre.min_range));

                getModel().initPreview(getMainController().getModel().getProjectData(), lineSensorController.getModel().getFilterQueue(), preprocessingFilterQueueController.getMinContrast());
                svOriginal.setImage(getModel().getImage(ImageDetailStackModel.iwOriginal, true));
                svPreProcessed.setImage(getModel().getImage(ImageDetailStackModel.iwPreProcessed, true));
                svLineSensor.setImage(getModel().getImage(ImageDetailStackModel.iwLineSensor, true));
                svFilaments.setImage(getModel().getImage(ImageDetailStackModel.iwFilaments, true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            previewRunning.set(false);
            if (updatePending.get()) {
                try {
                    updatePreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onUpdatePreview(ActionEvent event) {
        try {
            updatePreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void onUpdateImages(ActionEvent event) {
        //update the whole stack (preprocessing stack, linesensor stack, filamentsensor stack)

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                Consumer<Float> reporter = (f) -> {//not really in use
                };
                try {
                    if (getModel().getStackOrig().getWorker() != null) getModel().getStackOrig().getWorker().get();
                    long time = System.currentTimeMillis();
                    ProcessingUtils.initializePreprocessing(getModel().getStackOrig(), preprocessingFilterQueueController.getScale(), getModel().getStackPreprocessed());
                    updateProgress(10, 100);
                    ProcessingUtils.preProcess(getModel().getStackPreprocessed(), preprocessingFilterQueueController.getModel(), reporter);
                    System.out.println("ImageDetailStack::onUpdateImages() --- orig::DiffSizes=" + getModel().getStackOrig().isDifferentSizes() + ",pre::DiffSizes=" + getModel().getStackPreprocessed().isDifferentSizes());
                    updateProgress(33, 100);
                    getMainController().addDebugMessage("Finished Updating Pre-Processing");
                    getModel().getStackPreprocessed().notifyListeners();

                    ProcessingUtils.lineSensor(getModel().getStackPreprocessed(), getModel().getStackLineSensor(), lineSensorController.getModel().getFilterQueue(), reporter);
                    updateProgress(66, 100);
                    getMainController().addDebugMessage("Finished Updating LineSensor");
                    //MechanicalActiveArea.calcArea(getModel().getStackLineSensor());
                    getModel().getStackLineSensor().notifyListeners();

                    ProcessingUtils.filamentSensor(getModel().getStackLineSensor(), getMainController().getModel().getProjectData().getSettings());
                    getMainController().addDebugMessage("Finished Updating Filaments");
                    getModel().getStackFilaments().notifyListeners();
                    getMainController().addDebugMessage("Finished Updating Table");
                    getMainController().addDebugMessage("Time taken: " + ((System.currentTimeMillis() - time) / 1000) + "s");
                } catch (Exception e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);
    }

    public void updateFilamentImage() {
        getModel().getStackFilaments().notifyListeners();
        System.out.println("ImageDetailStack::UpdateFilamentImage");
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        setModel(getMainController().getModel().getStackModel());
        //initStack();
        preprocessingFilterQueueController.setMainController(getMainController(), this);
        preprocessingFilterQueueController.setUseCase(PreprocessingFilterQueue.FQUseCase.filament);
        preprocessingFilterQueueController.setApplicableFilters(getMainController().getModel().getClsApplicableFilters());
        lineSensorController.setMainController(getMainController(), this);
        filamentsController.setMainController(getMainController(), this);

        getModel().setPreProcessing(preprocessingFilterQueueController.getModel());

        ChangeListener<Object> listener = (ob, ol, nv) -> {
            if (nv != null) {
                if (nv instanceof Boolean) {
                    if ((Boolean) nv) {
                        try {
                            if (getModel().isPreview()) {
                                updatePreview();
                            }
                            preprocessingFilterQueueController.getModel().setChanged(false);
                            lineSensorController.getModel().changedProperty().setValue(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                } else {
                    try {
                        if (getModel().isPreview()) {
                            updatePreview();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        preprocessingFilterQueueController.minContrastProperty().addListener(listener);
        preprocessingFilterQueueController.scaleProperty().addListener(listener);
        preprocessingFilterQueueController.getModel().changedProperty().addListener(listener);
        lineSensorController.getModel().changedProperty().addListener(listener);
        lineSensorController.getModel().setChanged(false);


        //panePolygon.visibleProperty().bind(getModel().includeAreaOutlineProperty());


    }

    @FXML
    private void onExportSettings(ActionEvent actionEvent) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the settings-xml-file should be stored");
        File settings = fileChooser.showDialog(((Button) actionEvent.getSource()).getScene().getWindow());

        TextInputDialog dialog = new TextInputDialog("exportSettings.xml");
        dialog.setTitle("File-Name-Chooser");
        dialog.setHeaderText("Enter file name:");
        dialog.setContentText("File-name:");


        Optional<String> result = dialog.showAndWait();

        result.ifPresent(name -> {
            if (!name.isEmpty()) {
                String finalName = name;
                if (!name.contains(".xml")) finalName += ".xml";
                try {
                    getMainController().getModel().getProjectData().getSettings().store(new File(settings.getAbsolutePath() + File.separator + finalName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    getMainController().addDebugMessage(e);
                }
                getMainController().addDebugMessage("Settings exported.");
            }
        });


    }

    @FXML
    private void onImportSettings(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("choose settings-xml file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"));
        File settings = fileChooser.showOpenDialog(((Button) actionEvent.getSource()).getScene().getWindow());
        if (settings != null) {
            try {
                Settings dp = Settings.load(settings);
                getMainController().getModel().getProjectData().getSettings().init(dp);
                getMainController().addDebugMessage("Settings imported.");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                getMainController().addDebugMessage(e);
            }
        }
    }
}
