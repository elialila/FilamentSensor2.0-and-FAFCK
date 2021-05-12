package fa.control;

import control.*;
import core.settings.Export;
import fa.model.ExtMainModel;
import fa.model.SeparatorModel;
import focaladhesion.DataExtractor;
import focaladhesion.FocalAdhesion;
import focaladhesion.FocalAdhesionContainer;
import focaladhesion.FocalAdhesionProcessor;
import fx.custom.Area;
import fx.custom.GenericPopup;
import fx.custom.SliderSpinner;
import fx.custom.StackImageView;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.AutoThresholder;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import interfaces.IFilamentUpdater;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polyline;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import model.ProjectModel;
import core.FilterQueue;
import evaluation.Evaluator;
import filters.FilterAutomaticThreshold;
import filters.FilterInvert;
import filters.FilterManualThreshold;
import core.image.BinaryImage;
import core.image.Entry;
import core.image.ImageDependency;
import core.image.ImageWrapper;
import core.settings.FocAdh;
import util.Annotations.Nullable;
import util.IOUtils;
import util.ImageExporter;
import util.MixedUtils;
import util.PathScanner;
import util.io.FilamentCsvExport;
import util.io.FocalAdhesionCSVExport;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class FocalAdhesionProcessing extends AbstractControl implements IFilamentUpdater {


    //region FXML-Defines
    @FXML
    private CheckBox chkOnlyFoundFA;
    @FXML
    private CheckBox chkHideMultiUsedFAs;
    @FXML
    private CheckBox chkHideMultiVerifiedFibers;
    @FXML
    private CheckBox chkHideSingleUsedFAs;
    @FXML
    private CheckBox chkHideSingleVerifiedFibers;
    @FXML
    private CheckBox chkHideNonVerifiedFibers;
    @FXML
    private CheckBox chkHideUnusedFAs;

    @FXML
    private CheckBox chkIncludeNumbers;
    @FXML
    private SplitPane rootSplitPane;
    @FXML
    private Slider neighborHoodSize;
    @FXML
    private SliderSpinner sMinSize;
    @FXML
    private SliderSpinner sMaxSize;
    @FXML
    private SliderSpinner sMaxClusterAmount;
    @FXML
    private StackImageView stackViewFAOrig;
    @FXML
    private StackImageView stackViewFAPreProcessed;
    @FXML
    private StackImageView stackViewFAThresh;
    @FXML
    private StackImageView stackViewSFOrig;
    @FXML
    private ToggleGroup tgFilter;
    @FXML
    private ToggleGroup tgExport;
    @FXML
    private Pane faView;
    @FXML
    private FAView faViewController;
    @FXML
    private Area area;
    @FXML
    private HBox containerZoom;
    @FXML
    private HBox containerScroller;
    @FXML
    private VBox vBoxAreaParent;
    @FXML
    private CheckBox chkBothEnds;
    @FXML
    private CheckBox chkDoClosing;
    @FXML
    private CheckBox chkDoFillHoles;
    @FXML
    private ComboBox<FocalAdhesionProcessor.FAVerificationMethod> cbMethod;
    @FXML
    private SplitPane splitPaneImageTop;
    @FXML
    private SplitPane splitPaneImageBottom;
    @FXML
    private Node faPreProcessing;
    @FXML
    private PreprocessingFilterQueue faPreProcessingController;
    @FXML
    private TableView<SeparatorModel> tableSeparators;
    @FXML
    private TableColumn<SeparatorModel, Integer> columnSeparatorNumber;
    @FXML
    private TableColumn<SeparatorModel, String> columnSeparatorDelete;
    //endregion

    //region defines
    private ImageWrapper wrapperFAOrig;//original focal adhesion wrapper
    private ImageWrapper wrapperFAPreProcessed;//pre processed from FilterQueue
    private ImageWrapper wrapperFAThresh;//thresholded wrapper based on pre processed wrapper
    private ImageWrapper wrapperSFOrig;//original (including filaments)
    private FilterManualThreshold manualThreshold;
    private FilterAutomaticThreshold automaticThreshold;
    private FilterQueue queue;
    private BooleanProperty syncFilamentTable;
    private BooleanProperty syncFATable;
    private AtomicBoolean queueRunning;
    private AtomicBoolean updatePending;
    private AtomicBoolean thresholdUpdating;


    private ObjectProperty<ImageWrapper> wrapperOverlayPopup;
    private AtomicBoolean isPopupOverlayOpen;
    //endregion


    public FocalAdhesionProcessing() {
        manualThreshold = new FilterManualThreshold();
        automaticThreshold = new FilterAutomaticThreshold();
        thresholdUpdating = new AtomicBoolean(false);

        syncFATable = new SimpleBooleanProperty();
        syncFilamentTable = new SimpleBooleanProperty();
        queue = new FilterQueue();

        queueRunning = new AtomicBoolean(false);
        updatePending = new AtomicBoolean(false);
        isPopupOverlayOpen = new AtomicBoolean(false);
        wrapperOverlayPopup = new SimpleObjectProperty<>();

    }


    @FXML
    private void initialize() {
        cbMethod.setItems(FXCollections.observableList(Arrays.asList(FocalAdhesionProcessor.FAVerificationMethod.values())));
        cbMethod.getSelectionModel().select(FocalAdhesionProcessor.FAVerificationMethod.ellipse);
        vBoxAreaParent.managedProperty().bind(vBoxAreaParent.visibleProperty());
        faView.managedProperty().bind(faView.visibleProperty());
        faPreProcessing.managedProperty().bind(faPreProcessing.visibleProperty());
        initOptimalWidthSeparator();
        initRadioButtons();
        neighborHoodSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.intValue() % 2 == 0) {
                    neighborHoodSize.setValue(newValue.intValue() + 1);
                } else {
                    neighborHoodSize.setValue(newValue.intValue());
                }
            }

        });
        neighborHoodSize.setValue(1);
        ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
            if (syncFilamentTable.get() && syncFATable.get()) {
                faViewController.initTableVerifier(getEllipses(), getFilamentLines());
                syncFATable.set(false);
            }
        };
        syncFilamentTable.addListener(listener);
        syncFATable.addListener(listener);

        manualThreshold.thresholdProperty().bind(area.getThresholdProperty());
        automaticThreshold.methodProperty().bind(area.methodProperty());


        stackViewFAOrig.synchronize(stackViewFAPreProcessed);
        stackViewFAPreProcessed.synchronize(stackViewFAThresh);
        stackViewFAThresh.synchronize(stackViewSFOrig);

        //stackViewFAOrig.synchronize(stackViewSFOrig);
        //stackViewSFOrig.synchronize(stackViewFAThresh);


        stackViewFAOrig.currentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                notifyStackListeners();
            }
        });

        HBox tmpZoom = stackViewSFOrig.getHBoxZoom();
        HBox.setHgrow(tmpZoom, Priority.ALWAYS);
        VBox tmpScroller = stackViewSFOrig.getVBoxScroller();
        HBox.setHgrow(tmpScroller, Priority.ALWAYS);
        containerZoom.getChildren().add(tmpZoom);
        containerScroller.getChildren().add(tmpScroller);


        area.methodProperty().addListener((observable, oldValue, newValue) -> {
            if (!area.isManualThreshold() && !thresholdUpdating.get()) {
                thresholdUpdating.set(true);
                //only update on automatic threshold
                //threshold whole stack
                queue.clear();
                queue.add(automaticThreshold);
                //if(!Prefs.blackBackground)
                queue.add(new FilterInvert());
                if (wrapperFAPreProcessed != null)
                    wrapperFAPreProcessed.updateDependencies(wrapperFAPreProcessed, false).whenComplete((f, ex) -> {
                        if (ex != null) ex.printStackTrace();
                        wrapperFAThresh.notifyListeners();
                        thresholdUpdating.set(false);
                    });
            }
        });
        area.getThresholdProperty().addListener((observable, oldValue, newValue) -> {
            if (area.isManualThreshold() && area.thresholdExists() && !thresholdUpdating.get()) {
                thresholdUpdating.set(true);
                //only update on manual threshold
                //threshold whole stack
                queue.clear();
                queue.add(manualThreshold);
                //if(!Prefs.blackBackground)
                queue.add(new FilterInvert());
                if (wrapperFAPreProcessed != null)
                    wrapperFAPreProcessed.updateDependencies(wrapperFAPreProcessed, false).whenComplete((f, ex) -> {
                        if (ex != null) ex.printStackTrace();
                        wrapperFAThresh.notifyListeners();
                        thresholdUpdating.set(false);
                    });
            }
        });
        initTableSeparator();
    }

    private void initOptimalWidthSeparator() {
        tgFilter.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            double widthLeft = 0;
            if (area.isVisible()) {
                widthLeft = area.getWidth();
            } else if (faView.isVisible()) {
                widthLeft = faView.getWidth();
            }
            if (widthLeft > 0) {
                double divider = (widthLeft + 20) / rootSplitPane.getWidth();
                rootSplitPane.setDividerPosition(0, divider);
            }
        });
    }

    private void initRadioButtons() {
        faView.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null)
                return tgFilter.getSelectedToggle().getUserData().toString().equals("fa");
            return false;
        }, tgFilter.selectedToggleProperty()));

        vBoxAreaParent.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null) {
                //System.out.println("ImageDetailStack::initialize() --- userData=" + tgFilter.getSelectedToggle().getUserData().toString());
                return tgFilter.getSelectedToggle().getUserData().toString().equals("area");
            }
            return false;
        }, tgFilter.selectedToggleProperty()));

        faPreProcessing.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            if (tgFilter != null && tgFilter.getSelectedToggle() != null)
                return tgFilter.getSelectedToggle().getUserData().toString().equals("pre");
            return false;
        }, tgFilter.selectedToggleProperty()));
        tgFilter.getToggles().stream().filter(t -> t.getUserData().toString().equals("area")).findAny().ifPresent(toggle -> tgFilter.selectToggle(toggle));
        tgExport.getToggles().stream().filter(t -> t.getUserData().toString().equals("all")).findAny().ifPresent(toggleExport -> tgExport.selectToggle(toggleExport));
    }

    @FXML
    private void onValidateFilaments(ActionEvent event) {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void call() {
                getMainController().addDebugMessage("start validation process");
                updateProgress(0, 100);
                if (wrapperSFOrig.getEntryList().stream().
                        noneMatch(entry -> entry.getDataFilament().getFilaments().size() > 0)) {
                    //show error message and skip next part
                    getMainController().addDebugMessage("Error: Process Filaments first!");
                } else {
                    wrapperSFOrig.getEntryList().forEach(entry -> entry.getDataFilament().getFilaments().forEach(fc -> {
                                fc.setVerified(false);
                                fc.setVerifier(null);
                            })
                    );
                    FocalAdhesionProcessor processor = new FocalAdhesionProcessor();
                    FocalAdhesionProcessor.FAVerificationMethod method = cbMethod.getSelectionModel().getSelectedItem();
                    if (method != null) {
                        try {
                            processor.run(wrapperSFOrig, wrapperFAThresh, getMainController().getModel().getProjectData().getSettings(), method, (i) -> {
                                updateProgress(i, 100);
                            });
                        } catch (Exception e) {
                            getMainController().addDebugMessage(e);
                            e.printStackTrace();
                        }
                    }
                    wrapperSFOrig.notifyListeners();
                    initFocalAdhesionEllipse();
                }
                getMainController().addDebugMessage("finished validation process");
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(t);
    }

    @FXML
    private void onProcessFilaments(ActionEvent event) {
        //delegate this task to the ImageDetailStack
        ImageDetailStack control = (ImageDetailStack) getMainController().getRegisteredControl("control.ImageDetailStack");
        if (control != null) {
            control.onUpdateImages(event);
        }
    }


    /**
     * Initialises the whole functionality for drawing separators and "storing" them
     */
    private void initTableSeparator() {
        tableSeparators.setItems(FXCollections.observableArrayList());

        tableSeparators.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (oldValue != null) {
                    //change color back
                    oldValue.getLine().setStroke(javafx.scene.paint.Paint.valueOf("red"));
                }
                newValue.getLine().setStroke(javafx.scene.paint.Paint.valueOf("yellow"));
                //change color of newValue
            }
        });

        columnSeparatorNumber.prefWidthProperty().bind(tableSeparators.widthProperty().divide(2));
        columnSeparatorDelete.prefWidthProperty().bind(tableSeparators.widthProperty().divide(2));
        columnSeparatorNumber.setCellValueFactory((i) -> i.getValue().idProperty().asObject());
        columnSeparatorDelete.setCellValueFactory(new PropertyValueFactory<>("DUMMY"));
        Callback<TableColumn<SeparatorModel, String>, TableCell<SeparatorModel, String>> cellFactory
                =
                new Callback<TableColumn<SeparatorModel, String>, TableCell<SeparatorModel, String>>() {
                    @Override
                    public TableCell call(final TableColumn<SeparatorModel, String> param) {
                        final TableCell<SeparatorModel, String> cell = new TableCell<SeparatorModel, String>() {
                            final Button btn = new Button("Remove");

                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    btn.setOnAction(event -> {
                                        SeparatorModel separator = getTableView().getItems().get(getIndex());
                                        //update image
                                        //only delete from table if it is removed from stackview
                                        //image is stored in separator model to correctly remove from map<list>
                                        if (stackViewFAThresh.updateDrawing(separator.getPoints(), separator.getImage()))
                                            tableSeparators.getItems().remove(separator);
                                    });
                                    setGraphic(btn);
                                }
                                setText(null);
                            }
                        };
                        return cell;
                    }
                };
        columnSeparatorDelete.setCellFactory(cellFactory);
        stackViewFAThresh.setOnLineCreated((listPoint, line) -> {
            tableSeparators.getItems().add(new SeparatorModel(listPoint, line, stackViewFAOrig.getCurrent()));
        });
        stackViewFAThresh.setOnLineUpdated((list, line) ->
                tableSeparators.getItems().stream().
                        filter(model -> model.getPoints().equals(list)).
                        findAny().orElse(new SeparatorModel()).setLine(line)
        );
        stackViewFAThresh.setEnableDrawing(true);
    }


    public void setImages(Consumer<Void> callback) {
        //reset stuff
        tableSeparators.setItems(FXCollections.observableArrayList());
        SeparatorModel.reset();//reset separator counter
        stackViewFAThresh.getLineMap().clear();


        ProjectModel focalAdhesionModel = getMainController().getModel().getProjectModel();
        File f1 = new File(focalAdhesionModel.getFileFocalAdhesions());
        if (!f1.exists()) {
            getMainController().addDebugMessage("FocalAdhesionImageFile doesn't exist\n" + f1.getAbsolutePath());
            return;
        }
        File f2 = new File(focalAdhesionModel.getFileStressFibers());
        if (!f2.exists()) {
            getMainController().addDebugMessage("StressFibersImageFile doesn't exist");
            return;
        }

        getMainController().addDebugMessage("Prepare started!");
        vBoxAreaParent.setDisable(true);
        faViewController.setMainController(getMainController(), this);
        faPreProcessingController.setMainController(getMainController(), this);
        faPreProcessingController.setUseCase(PreprocessingFilterQueue.FQUseCase.focalAdhesion);
        faPreProcessingController.setApplicableFilters(((ExtMainModel) getMainController().getModel()).getApplicableFiltersFa());

        faPreProcessingController.getModel().changedProperty().addListener((observable, oldValue, newValue) -> {
            //queue got changed
            if (newValue) {
                updateQueue();
                faPreProcessingController.getModel().setChanged(false);
            }
        });

        chkHideUnusedFAs.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideUnusedFAs));
        chkHideNonVerifiedFibers.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideNonVerifiedFibers));


        chkHideSingleUsedFAs.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideSingleUsedFAs));
        chkHideSingleVerifiedFibers.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideSingleVerifiedFibers));


        chkHideMultiUsedFAs.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideMultiUsedFAs));
        chkHideMultiVerifiedFibers.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Export.hideMultiVerifiedFibers));

        chkOnlyFoundFA.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(FocAdh.showOnlyFoundFA));

        ChangeListener<Boolean> listener = (observableValue, aBoolean, t1) -> {
            wrapperSFOrig.notifyListeners();
            updateColorCodedPopup();
        };

        chkHideNonVerifiedFibers.selectedProperty().addListener(listener);
        chkHideUnusedFAs.selectedProperty().addListener(listener);
        chkHideSingleUsedFAs.selectedProperty().addListener(listener);
        chkHideSingleVerifiedFibers.selectedProperty().addListener(listener);
        chkHideMultiUsedFAs.selectedProperty().addListener(listener);
        chkHideMultiVerifiedFibers.selectedProperty().addListener(listener);
        chkOnlyFoundFA.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            //update wrapper thresh
            if (thresholdUpdating.get()) return;
            thresholdUpdating.set(true);
            //only update on manual threshold
            if (wrapperFAPreProcessed != null)
                wrapperFAPreProcessed.updateDependencies(wrapperFAPreProcessed, false).whenComplete((f, ex) -> {
                    if (ex != null) ex.printStackTrace();
                    wrapperFAThresh.notifyListeners();
                    thresholdUpdating.set(false);
                });
        });


        chkIncludeNumbers.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            updateColorCodedPopup();
        });


        Task<Void> t = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(20, 100);
                prepare(f1, f2);
                updateProgress(75, 100);
                Platform.runLater(() -> {
                    getMainController().addDebugMessage("Prepare finished!");
                    vBoxAreaParent.setDisable(false);
                    updateProgress(100, 100);
                });
                return null;
            }
        };
        CompletableFuture<Void> future = getMainController().runAsync(t);
        if (callback != null) future.thenAccept(callback);
    }

    //update pre processing on changing parameters
    private void updateQueue() {
        if (queueRunning.get()) {
            updatePending.set(true);
            return;
        }
        queueRunning.set(true);
        updatePending.set(false);
        try {
            Thread.sleep(50);
            wrapperFAOrig.updateDependencies(wrapperFAOrig, false).whenComplete((v, t) -> {
                getMainController().addDebugMessage("Pre-Processing completed -updatePending=" + updatePending.get());
                notifyStackListeners();
                queueRunning.set(false);
                if (updatePending.get()) {
                    try {
                        updateQueue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
    }


    private void createImageDependency() {
        //establishes dependency between orig and pre processed
        wrapperFAOrig.addImageDependency(new ImageDependency(wrapperFAPreProcessed,
                (src, tgt) -> src.cloneImage(tgt),
                (v) -> v,
                (wrapper) -> {
                    faPreProcessingController.getModel().run(wrapper, (f) -> {
                    });
                }
                , true));

        wrapperFAPreProcessed.addImageDependency(new ImageDependency(wrapperFAThresh,
                (src, tgt) -> {
                    if (area.isSingleImageOnly()) {
                        //only clone current image
                        src.cloneImage(tgt, stackViewFAOrig.getCurrent() - 1);
                    } else {
                        src.cloneImage(tgt);
                    }
                },
                (v) -> v,
                (wrapper) -> {
                    if (area.isSingleImageOnly()) {
                        //only threshold current image
                        queue.run(wrapper, stackViewFAOrig.getCurrent() - 1, (f) -> {
                        });
                    } else {
                        //only Found option only works with full stack (it disables the thresholding so it makes no sense, to enable single image)
                        if (wrapper.getEntryList().stream().noneMatch(e -> e.getCorrelationData() != null) || !chkOnlyFoundFA.isSelected()) {
                            //no correlation data, do normal thresholding
                            queue.run(wrapper, (f) -> {
                            });
                        } else {
                            ImageExporter.ommitNonFocalAdhesion(wrapper, FocalAdhesionProcessor.FAVerificationMethod.pixel);
                        }
                    }
                }
                , true));

        wrapperFAThresh.addImageDependency(new ImageDependency(wrapperFAOrig,
                (src, tgt) -> {
                }, (v) -> v,
                (wrapper) -> {
                    //this dependency is for extracting the focal adhesions
                    DataExtractor.extractFocalAdhesionData(wrapperFAThresh,
                            getMainController().getModel().getProjectData().getSettings(),
                            stackViewFAThresh.getLineMap(),
                            (p) -> {
                            });
                    //copy focal adhesions into wrapper orig
                    for (int i = 0; i < wrapperSFOrig.getEntryList().size() && i < wrapperFAThresh.getEntryList().size(); i++) {
                        wrapperSFOrig.getEntryList().get(i).setCorrelationData(wrapperFAThresh.getEntryList().get(i).getCorrelationData());
                    }
                }
                , true));
    }


    private void prepare(File fFocalAdhesion, File fStressFibers) {
        wrapperFAOrig = new ImageWrapper(fFocalAdhesion, getMainController().getModel().getProjectData().getSettings());
        wrapperSFOrig = new ImageWrapper(fStressFibers, getMainController().getModel().getProjectData().getSettings());

        //set ranges of filter slider
        final int size = wrapperFAOrig.getWidth() * wrapperFAOrig.getHeight() / 2;

        //init min,max,fa-amount filters (init here because they are depending on image size)
        sMinSize.valueProperty().unbind();
        sMaxSize.valueProperty().unbind();
        sMaxClusterAmount.valueProperty().unbind();

        sMinSize.initRanges(1, size, 1);
        sMaxSize.initRanges(1, size, 1);
        sMaxClusterAmount.initRanges(1, size, 1);

        sMaxClusterAmount.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(FocAdh.maxClusterAmount));
        sMinSize.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(FocAdh.minSize));
        sMaxSize.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(FocAdh.maxSize));


        chkBothEnds.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(FocAdh.bothEnds));
        neighborHoodSize.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(FocAdh.neighborHoodSize));

        chkDoClosing.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(FocAdh.doClosing));
        chkDoFillHoles.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(FocAdh.doFillHoles));


        wrapperFAPreProcessed = wrapperFAOrig.clone();
        wrapperFAThresh = wrapperFAOrig.clone();
        getMainController().getModel().getStackModel().setStackOrig(wrapperSFOrig);
        getMainController().getModel().getStackModel().setStackFilaments(wrapperSFOrig);
        getMainController().getModel().getStackModel().setStackPreprocessed(wrapperSFOrig.clone());
        getMainController().getModel().getStackModel().setStackLineSensor(wrapperSFOrig.clone());

        Platform.runLater(() -> {

            wrapperFAOrig.addListener((observable, oldValue, newValue) -> {
                if (stackViewFAOrig.getCurrent() < 1) return;//-1 is used for "invalidate"
                stackViewFAOrig.setImage(SwingFXUtils.toFXImage(wrapperFAOrig.getImage(stackViewFAOrig.getCurrent()), null));
            });

            wrapperSFOrig.addListener((observable, oldValue, newValue) -> {
                if (stackViewFAOrig.getCurrent() < 1) return;
                //overlay the threshed focal adhesions in color green
                BufferedImage tmp = wrapperSFOrig.getImage(stackViewSFOrig.getCurrent());
                if (wrapperFAThresh.getEntryList().get(stackViewSFOrig.getCurrent() - 1).getProcessor().isBinary())
                    ImageExporter.addFocalAdhesions(tmp, wrapperFAThresh.getEntryList().get(stackViewSFOrig.getCurrent() - 1).getProcessor(), Color.green, 0);
                stackViewSFOrig.setImage(SwingFXUtils.toFXImage(tmp, null));
                initFilamentsPolyline(wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1));
                initFocalAdhesionEllipse();
            });

            wrapperFAPreProcessed.addListener((observable, oldValue, newValue) -> {
                if (stackViewFAOrig.getCurrent() < 1) return;
                stackViewFAPreProcessed.setImage(SwingFXUtils.toFXImage(wrapperFAPreProcessed.getImage(stackViewFAPreProcessed.getCurrent()), null));
            });

            wrapperFAThresh.addListener((observable, oldValue, newValue) -> {
                if (stackViewFAOrig.getCurrent() < 1) return;
                BufferedImage bufferedImage = wrapperFAThresh.getImage(stackViewFAThresh.getCurrent());
                ImageExporter.addFocalAdhesions(bufferedImage, Color.YELLOW, wrapperSFOrig.getEntryList().get(stackViewFAThresh.getCurrent() - 1));
                stackViewFAThresh.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                Platform.runLater(this::initFocalAdhesionEllipse);
            });

            getMainController().getModel().getStackModel().currentImageProperty().bindBidirectional(stackViewSFOrig.currentProperty());

            stackViewSFOrig.setMax(wrapperSFOrig.getSize());
            stackViewSFOrig.setMin(0);
            stackViewSFOrig.setMin(1);
            stackViewSFOrig.setCurrent(1);

            //create basic dependencies between FAWrappers
            createImageDependency();

            area.setMethod(null);//reset value: because otherwise the change event won't get fired
            // (for the use-case that several stacks are processed after each other)
            area.setMethod(AutoThresholder.Method.Otsu);
            notifyStackListeners();

            initFocalAdhesionEllipse();
            faViewController.initTableFA(new ArrayList<>());
            faViewController.initTableVerifier(new ArrayList<>(), new ArrayList<>());

        });

    }

    private void notifyStackListeners() {
        if (wrapperSFOrig != null) wrapperSFOrig.notifyListeners();
        if (wrapperFAThresh != null) wrapperFAThresh.notifyListeners();
        if (wrapperFAPreProcessed != null) wrapperFAPreProcessed.notifyListeners();
        if (wrapperFAOrig != null) wrapperFAOrig.notifyListeners();
    }


    @Override
    public void updateFilamentImage() {
        try {
            notifyStackListeners();
            System.out.println("ImageDetailStack::UpdateFilamentImage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void initFilamentsPolyline(Entry wrapperEntry) {
        IFilamentUpdater.initFilamentsPolyline(wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1),
                stackViewSFOrig, Filaments.defaultFilament, Filaments.defaultKeepFalse,
                Filaments.defaultVerified, Filaments.defaultSelected,
                (lines) -> {
                    try {
                        ((Filaments) getMainController().getRegisteredControl("control.Filaments")).initTable(lines);
                    } catch (ClassCastException e) {
                        throw new RuntimeException(e);
                    }
                    //filamentsController.initTable(lines);
                    syncFilamentTable.set(true);
                }
                , getMainController().getModel().getProjectData().getSettings());
    }


    private List<Polyline> getFilamentLines() {
        return stackViewSFOrig.getStackPane().getChildren().
                stream().filter(p -> p instanceof Pane).
                flatMap(p -> ((Pane) p).getChildren().stream()).filter(p -> p instanceof Polyline).
                map(p -> (Polyline) p).collect(Collectors.toList());
    }

    private List<Ellipse> getEllipses() {
        return stackViewFAOrig.getStackPane().getChildren().stream().filter(p -> p instanceof Pane).
                flatMap(p -> ((Pane) p).getChildren().stream()).filter(i -> i instanceof Ellipse).
                map(i -> (Ellipse) i).collect(Collectors.toList());
    }


    private void initFocalAdhesionEllipse(Entry wrapperEntry, StackImageView stackImageView,
                                          javafx.scene.paint.Color keep, javafx.scene.paint.Color notKept,
                                          javafx.scene.paint.Color selected,
                                          @Nullable Consumer<List<Ellipse>> callback) {
        //remove all FA Ellipses
        Platform.runLater(() ->
                stackImageView.getStackPane().getChildren().
                        removeAll(
                                stackImageView.getStackPane().getChildren().
                                        stream().filter(e -> e instanceof Pane && Objects.equals(e.getUserData(), "ellipses")).collect(Collectors.toList())
                        )
        );
        //return when no data is found
        if (wrapperEntry == null ||
                wrapperEntry.getCorrelationData() == null ||
                !(wrapperEntry.getCorrelationData() instanceof FocalAdhesionContainer) ||
                ((FocalAdhesionContainer) wrapperEntry.getCorrelationData()).getData().size() <= 0) return;


        FocalAdhesionContainer focalAdhesionContainer = (FocalAdhesionContainer) wrapperEntry.getCorrelationData();
        List<FocalAdhesion> listFocalAdhesion = focalAdhesionContainer.getFilteredData(wrapperEntry.getDataFilament(), getMainController().getModel().getProjectData().getSettings());

        System.out.println("FocalAdhesionProcessing::initFAEllipse() --- after getFilteredData()");

        Pane ellipses = new Pane();
        ellipses.setUserData("ellipses");
        for (FocalAdhesion adhesion : listFocalAdhesion) {

            Ellipse ellipse = new Ellipse();//new Ellipse(adhesion.getCenter().getX()*scale,adhesion.getCenter().getY()*scale,adhesion.getXAxisLength()*scale/2,adhesion.getYAxisLength()*scale/2);
            ellipse.setFill(javafx.scene.paint.Color.TRANSPARENT);
            ellipse.setRotate(Math.toDegrees(adhesion.getOrientation()));
            //properties can be bound via "custom"-property: create scale as a property and create a properties.multiply and set it
            DoubleBinding scaleBinding = stackImageView.getScaleBinding();
            ellipse.centerXProperty().bind(scaleBinding.multiply(adhesion.getCenter().getX()));
            ellipse.centerYProperty().bind(scaleBinding.multiply(adhesion.getCenter().getY()));
            ellipse.radiusXProperty().bind(scaleBinding.multiply(adhesion.getLengthMainAxis() / 2));
            ellipse.radiusYProperty().bind(scaleBinding.multiply(adhesion.getLengthSideAxis() / 2));
            ellipse.strokeWidthProperty().bind(scaleBinding.multiply(2d));
            ellipse.strokeProperty().bind(Bindings.createObjectBinding(() ->
                    adhesion.selectedProperty().get() ? selected : adhesion.keepProperty().get() ? keep : notKept, adhesion.selectedProperty(), adhesion.keepProperty()));
            ellipse.setUserData(adhesion);
            ellipse.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getSource() instanceof Ellipse) {//onclick funktioniert
                    Ellipse source = (Ellipse) mouseEvent.getSource();
                    if (source.getUserData() instanceof FocalAdhesion) {
                        ((FocalAdhesion) source.getUserData()).setKeep(!((FocalAdhesion) source.getUserData()).isKeep());
                        //flip the keep state
                    }
                }
            });
            //this part can be removed, since focal adhesion has no verified (but it could be colored if it verifies a filament
            /*
            fil.verifiedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    if (newValue) filament.setStroke(strokeVerified);
                    else if (fil.isKeep()) filament.setStroke(stroke);
                    else filament.setStroke(strokeKeepFalse);
                }
            });
            */
            ellipses.getChildren().add(ellipse);
        }
        ellipses.maxHeightProperty().bind(stackImageView.getStackPane().heightProperty());
        ellipses.maxWidthProperty().bind(stackImageView.getStackPane().widthProperty());
        ellipses.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(ellipses, Pos.TOP_LEFT);
        Platform.runLater(() -> {
            stackImageView.getStackPane().getChildren().add(ellipses);
            if (callback != null) callback.accept(ellipses.getChildren().stream().filter(i -> i instanceof Ellipse).
                    map(i -> (Ellipse) i).collect(Collectors.toList())
            );
        });//wrap it with javaFx thread
    }


    public void initFocalAdhesionEllipse() {
        initFocalAdhesionEllipse(wrapperSFOrig.getEntryList().get(stackViewFAThresh.getCurrent() - 1),
                stackViewFAOrig, javafx.scene.paint.Color.GREEN, javafx.scene.paint.Color.RED, javafx.scene.paint.Color.YELLOW,
                (ellipses) -> {
                    syncFATable.set(true);
                    faViewController.initTableFA(ellipses);
                });
    }

    @FXML
    private void onExportFocalAdhesionCsv(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                Platform.runLater(() -> getMainController().addDebugMessage("Start Export Focal Adhesion CSV"));
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    wrapperSFOrig.getEntryList().forEach(entry -> {
                        try {
                            FocalAdhesionCSVExport.exportFocalAdhesionCSV(imageFile, entry, getMainController().getModel().getProjectData().getSettings());
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                getMainController().addDebugMessage(e);
                            });
                            e.printStackTrace();
                        }
                    });
                } else {
                    try {
                        FocalAdhesionCSVExport.exportFocalAdhesionCSV(imageFile, wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1), getMainController().getModel().getProjectData().getSettings());
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> getMainController().addDebugMessage("Finished Export Focal Adhesion CSV"));
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);

    }

    /**
     * XML Files are not Filtered by unusedFA and nonVerifiedFiber
     *
     * @param event
     */
    @FXML
    private void onExportFocalAdhesionXml(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the xml should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                Platform.runLater(() -> getMainController().addDebugMessage("Start Export Focal Adhesion XML"));
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    wrapperSFOrig.getEntryList().forEach(entry -> {
                        try {
                            IOUtils.writeXML(entry, imageFile, new File(entry.getPath()), "serialized");
                        } catch (FileNotFoundException e) {
                            Platform.runLater(() -> {
                                getMainController().addDebugMessage(e);
                            });
                            e.printStackTrace();
                        }
                    });
                } else {
                    try {
                        Entry entry = wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1);
                        IOUtils.writeXML(entry, imageFile, new File(entry.getPath()), "serialized");
                    } catch (FileNotFoundException e) {
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> getMainController().addDebugMessage("Finished Export Focal Adhesion XML"));
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);
    }


    @FXML
    public void onExportFocalAdhesionImage(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the image should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);

                ImageWrapper wrapper = wrapperFAThresh.clone();//output the thresholded image as base,
                if (chkOnlyFoundFA.isSelected()) {
                    ImageExporter.ommitNonFocalAdhesion(wrapper, FocalAdhesionProcessor.FAVerificationMethod.pixel);
                }

                // it has to contain all filament data and fa data
                //for filtering purpose
                for (int i = 0; i < wrapper.getSize(); i++) {
                    wrapper.getEntryList().get(i).setDataFilament(wrapperSFOrig.getEntryList().get(i).getDataFilament());
                }
                //wrapper is in std form black foreground white background - invert
                //could be inverted by code below (white foreground black background) - not wanted currently
                /*FilterQueue queue=new FilterQueue();
                queue.add(new FilterInvert());
                queue.run(wrapper,(f)->{});//invert image
                */

                ImageExporter.addFocalAdhesions(wrapper, Color.green, Color.yellow, chkIncludeNumbers.isSelected(), Color.blue);
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    FileSaver fileSaver = new FileSaver(wrapper.getImage());
                    if (wrapper.getSize() > 1)
                        fileSaver.saveAsTiffStack(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, PathScanner.OME_TIF, "stack_adhesion").getAbsolutePath());
                    else
                        fileSaver.saveAsPng(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, ".png", "adhesion").getAbsolutePath());
                } else {
                    ImageExporter.exportImage(wrapper.getImage(stackViewFAThresh.getCurrent()),
                            IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(stackViewFAThresh.getCurrent() - 1).getPath()), imageFile, ".png", "adhesion")
                    );
                }
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);
    }

    @FXML
    public void onProcessFocalAdhesion(ActionEvent event) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                getMainController().addDebugMessage("focal adhesion processing started");

                System.out.println("Focal Adhesion Processing Started");
                System.out.println("doClosing:" + getMainController().getModel().getProjectData().getSettings().getValue(FocAdh.doClosing));

                wrapperFAOrig.updateDependencies(wrapperFAOrig, true).whenComplete((f, ex) -> {
                    if (ex != null) getMainController().addDebugMessage(ex);
                    notifyStackListeners();
                }).get();

                updateProgress(100, 100);
                getMainController().addDebugMessage("focal adhesion processing finished");
                return null;
            }
        };
        getMainController().runAsync(task);
    }


    @FXML
    public void onExportOverlayImage(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the image should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                ImageWrapper wrapper = wrapperSFOrig.clone();
                ImageExporter.addFilaments(wrapper, Color.red, Color.magenta, true);
                ImageExporter.addFocalAdhesions(wrapper, Color.green, Color.yellow, chkIncludeNumbers.isSelected(), Color.cyan);
                //ImageExporter.addFocalAdhesions(wrapper, wrapperFocalAdhesionThresh, Color.green, 0);

                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    FileSaver fileSaver = new FileSaver(wrapper.getImage());
                    if (wrapper.getSize() > 1)
                        fileSaver.saveAsTiffStack(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, PathScanner.OME_TIF, "stack_adhesion_overlay").getAbsolutePath());
                    else
                        fileSaver.saveAsPng(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, ".png", "adhesion_overlay").getAbsolutePath());
                } else {
                    ImageExporter.exportImage(wrapper.getImage(stackViewFAOrig.getCurrent()),
                            IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, ".png", "adhesion_overlay")
                    );
                }

                wrapper.closeImage();
                updateProgress(100, 100);
                return null;
            }
        });
    }


    @FXML
    private void onSplitWindow(ActionEvent actionEvent) {
        //store original position of items on UI and remove them from current UI
        int posFAOrig = splitPaneImageTop.getItems().indexOf(stackViewFAOrig);
        splitPaneImageTop.getItems().remove(stackViewFAOrig);

        int posFAPre = splitPaneImageTop.getItems().indexOf(stackViewFAPreProcessed);
        splitPaneImageTop.getItems().remove(stackViewFAPreProcessed);

        int posFAThresh = splitPaneImageBottom.getItems().indexOf(stackViewFAThresh);
        splitPaneImageTop.getItems().remove(stackViewFAThresh);

        int posFibers = splitPaneImageBottom.getItems().indexOf(stackViewSFOrig);
        splitPaneImageBottom.getItems().remove(stackViewSFOrig);

        try {
            //open popups with UI elements
            if (posFAOrig >= 0)
                GenericPopup.openPopup("Focal Adhesion Original", stackViewFAOrig, splitPaneImageTop, 0);
            if (posFAPre >= 0)
                GenericPopup.openPopup("Focal Adhesion Pre Processed", stackViewFAPreProcessed, splitPaneImageTop, -1);
            if (posFAThresh >= 0)
                GenericPopup.openPopup("Focal Adhesion Thresh", stackViewFAThresh, splitPaneImageBottom, 0);
            if (posFibers >= 0)
                GenericPopup.openPopup("Stress Fibers Original", stackViewSFOrig, splitPaneImageBottom, -1);
        } catch (IOException e) {
            e.printStackTrace();
            //re-create ui before opening popups
            splitPaneImageTop.getItems().add(stackViewFAOrig);
            splitPaneImageTop.getItems().add(stackViewFAPreProcessed);

            splitPaneImageBottom.getItems().add(stackViewFAThresh);
            splitPaneImageBottom.getItems().add(stackViewSFOrig);
        }
    }

    @FXML
    private void onExportVerifierTable(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                Platform.runLater(() -> getMainController().addDebugMessage("Start Export Focal Adhesion CSV"));
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    wrapperSFOrig.getEntryList().forEach(entry -> {
                        try {
                            IOUtils.exportVerifierTable(imageFile, entry, getMainController().getModel().getProjectData().getSettings());
                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                getMainController().addDebugMessage(e);
                            });
                            e.printStackTrace();
                        }
                    });
                } else {
                    try {
                        IOUtils.exportVerifierTable(imageFile, wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1), getMainController().getModel().getProjectData().getSettings());
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> getMainController().addDebugMessage("Finished Export Focal Adhesion CSV"));
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);
    }

    @FXML
    private void onEvaluateFa(ActionEvent actionEvent) {
        getMainController().<Evaluation>openPopUp("/view/Evaluation.fxml", "Evaluate Focal Adhesion's", (control) -> {
            ImageWrapper evalMask = wrapperSFOrig.clone();
            ImageExporter.ommitNonFocalAdhesion(evalMask, FocalAdhesionProcessor.FAVerificationMethod.pixel);
            control.setToEval(evalMask,
                    (i) -> new BinaryImage(evalMask.getEntryList().get(i).getProcessor().convertToByteProcessor()),
                    () -> Evaluator.ShapeType.dotLike);
        });

    }


    @FXML
    private void onEvaluateFiber(ActionEvent actionEvent) {
        getMainController().<Evaluation>openPopUp("/view/Evaluation.fxml", "Evaluate Filaments", (control) -> {
            ImageWrapper evalMask = ImageExporter.getFilamentWrapperAsMask(wrapperSFOrig);
            control.setToEval(evalMask,
                    (i) -> new BinaryImage(evalMask.getEntryList().get(i).getProcessor().convertToByteProcessor())
                    , () -> Evaluator.ShapeType.lineLike);
        });
    }


    @FXML
    private void onExportColorCodedFAFiberImage(ActionEvent actionEvent) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the image should be stored");
        final File imageFile = fileChooser.showDialog(((Button) actionEvent.getSource()).getScene().getWindow());
        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // color code fibers verified by 1 FA, by >1 FA's, non used FA's, non verified Fibers etc.
                updateProgress(10, 100);
                ImageWrapper wrapper = ImageExporter.getColorCodedImage(wrapperSFOrig, true, chkIncludeNumbers.isSelected());
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    FileSaver fileSaver = new FileSaver(wrapper.getImage());
                    if (wrapper.getSize() > 1)
                        fileSaver.saveAsTiffStack(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, PathScanner.OME_TIF, "stack_cc_overlay").getAbsolutePath());
                    else
                        fileSaver.saveAsPng(IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, ".png", "cc_overlay").getAbsolutePath());
                } else {
                    ImageExporter.exportImage(wrapper.getImage(stackViewFAOrig.getCurrent()),
                            IOUtils.getOutFileFromImageFile(new File(wrapper.getEntryList().get(0).getPath()), imageFile, ".png", "cc_overlay")
                    );
                }

                wrapper.closeImage();
                updateProgress(100, 100);
                Platform.runLater(() -> getMainController().addDebugMessage("Finished Export Color-coded Image"));

                return null;
            }
        });


    }

    @FXML
    private void onOpenImageLegend(ActionEvent actionEvent) {

        ImageProcessor ip = new ColorProcessor(500, 180);
        ImagePlus imp = new ImagePlus("Legend", ip);
        List<Color> colorList = MixedUtils.getColorsForColorCoding();
        Color cFiberVerifiedByOne = colorList.get(0),//Color.magenta,
                cFiberVerifiedByGreaterOne = colorList.get(1),
                cNonVerifiedFibers = colorList.get(2),
                cUnusedFA = colorList.get(5),
                cSingleVerificationFA = colorList.get(3),
                cMultiVerificationFA = colorList.get(4);
        ImageExporter.printColorCodedFAImageLegend(ip, cFiberVerifiedByOne, cFiberVerifiedByGreaterOne, cNonVerifiedFibers, cUnusedFA, cSingleVerificationFA, cMultiVerificationFA, 0);

        ImageView ivLegend = new ImageView();
        ivLegend.setImage(SwingFXUtils.toFXImage(imp.getBufferedImage(), null));
        try {
            GenericPopup.openPopup("Legend", ivLegend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private CompletableFuture<Void> updateColorCodedPopup() {
        if (isPopupOverlayOpen.get()) {
            return getMainController().runAsync(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(10, 100);
                    synchronized (FocalAdhesionProcessing.class) {
                        wrapperOverlayPopup.setValue(ImageExporter.getColorCodedImage(wrapperSFOrig, true, chkIncludeNumbers.isSelected()));
                    }
                    updateProgress(100, 100);
                    return null;
                }
            });
        }
        return null;
    }


    @FXML
    private void onOpenPopUpColorCodedOverlay(ActionEvent actionEvent) {
        isPopupOverlayOpen.set(true);
        //store the created stackview somewhere to update the image
        try {
            CompletableFuture<Void> future = updateColorCodedPopup();
            if (future != null) future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (wrapperOverlayPopup.get() == null) return;
        StackImageView sivPopup = new StackImageView();
        sivPopup.setMax(wrapperOverlayPopup.get().getSize());

        if (wrapperOverlayPopup.get().getSize() == 1) {
            sivPopup.setHideScroller(true);
        }
        sivPopup.currentProperty().addListener((observableValue, number, t1) -> {
            if (t1 != null && wrapperOverlayPopup.get() != null) {
                sivPopup.setImage(SwingFXUtils.toFXImage(wrapperOverlayPopup.get().getImage(t1.intValue()), null));
            }
        });
        wrapperOverlayPopup.addListener((observableValue, imageWrapper, t1) -> {
            if (t1 != null) {
                sivPopup.setImage(SwingFXUtils.toFXImage(t1.getImage(sivPopup.getCurrent()), null));
            }
        });
        sivPopup.setImage(SwingFXUtils.toFXImage(wrapperOverlayPopup.get().getImage(1), null));

        try {
            GenericPopup.openPopup("Color-coded Overlay", sivPopup, event -> {
                isPopupOverlayOpen.set(false);
                wrapperOverlayPopup.setValue(null);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @FXML
    private void onExportGroupedCsvs(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the csv's should be stored");
        final File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                Platform.runLater(() -> getMainController().addDebugMessage("Start Export Focal Adhesion CSV"));
                if (tgExport.getSelectedToggle().getUserData().toString().equals("all")) {
                    wrapperSFOrig.getEntryList().forEach(entry -> {
                        try {

                            FocalAdhesionCSVExport.exportFocalAdhesionCSVGrouped(imageFile, entry, getMainController().getModel().getProjectData().getSettings());
                            FilamentCsvExport.exportFilamentsCSVGrouped(imageFile, entry, getMainController().getModel().getProjectData().getSettings());

                        } catch (IOException e) {
                            Platform.runLater(() -> {
                                getMainController().addDebugMessage(e);
                            });
                            e.printStackTrace();
                        }
                    });
                } else {
                    try {
                        FocalAdhesionCSVExport.exportFocalAdhesionCSVGrouped(imageFile, wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1), getMainController().getModel().getProjectData().getSettings());
                        FilamentCsvExport.exportFilamentsCSVGrouped(imageFile, wrapperSFOrig.getEntryList().get(stackViewSFOrig.getCurrent() - 1), getMainController().getModel().getProjectData().getSettings());

                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            getMainController().addDebugMessage(e);
                        });
                        e.printStackTrace();
                    }
                }
                Platform.runLater(() -> getMainController().addDebugMessage("Finished Export Focal Adhesion CSV"));
                updateProgress(100, 100);
                return null;
            }
        };
        getMainController().runAsync(task);


    }

}
