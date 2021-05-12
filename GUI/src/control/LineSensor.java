package control;

import core.Const;

import core.Misc;
import core.calculation.WrappedSiZer;
import fx.custom.SliderSpinner;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleGroup;
import model.LineSensorModel;
import core.settings.Settings;
import core.settings.Bin;
import core.settings.Ori;
import core.settings.Trace;
import core.settings.WiZer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Collectors;


public class LineSensor extends AbstractControl {

    @FXML
    private CheckBox chkIsAreaOrExt;
    @FXML
    private ToggleGroup tgBinarizationMethod;
    @FXML
    private SliderSpinner sMinMeanValue;
    @FXML
    private SliderSpinner sSigma;
    @FXML
    private SliderSpinner sMinStandardDeviation;
    @FXML
    private SliderSpinner sMinFilamentLength;
    @FXML
    private SliderSpinner sLengthStraightPieces;
    @FXML
    private SliderSpinner sMinAngleDifference;
    @FXML
    private SliderSpinner sTolerance;
    @FXML
    private SliderSpinner sMinArea;
    @FXML
    private SliderSpinner sMinFilaments;


    @FXML
    private CheckBox chkRestrictToAreaMask;
    @FXML
    private CheckBox chkThickenLines;
    @FXML
    private CheckBox chkRemoveBoundaryFilaments;
    @FXML
    private CheckBox chkCurvedFilaments;
    @FXML
    private CheckBox chkAsStraightPieces;
    @FXML
    private CheckBox chkLogScale;

    @FXML
    private ComboBox<String> cbSamples;


    private LineSensorModel model;

    private AbstractControl parent;


    public LineSensor() {
        model = new LineSensorModel();
    }

    @FXML
    private void initialize() {
        tgBinarizationMethod.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.isSelected()) {
                model.binarizationMethodProperty().setValue(newValue.getUserData().toString());
            }
        });
        sMinMeanValue.valueProperty().bindBidirectional(model.minMeanValueProperty());
        sSigma.valueProperty().bindBidirectional(model.sigmaProperty());
        sMinStandardDeviation.valueProperty().bindBidirectional(model.minStandardDeviationProperty());
        sMinFilamentLength.valueProperty().bindBidirectional(model.minFilamentLengthProperty());
        sLengthStraightPieces.valueProperty().bindBidirectional(model.lengthStraightPiecesProperty());
        sMinAngleDifference.valueProperty().bindBidirectional(model.minAngleDifferenceProperty());
        sTolerance.valueProperty().bindBidirectional(model.toleranceProperty());
        sMinArea.valueProperty().bindBidirectional(model.minAreaProperty());
        sMinFilaments.valueProperty().bindBidirectional(model.minFilamentsProperty());


        chkIsAreaOrExt.selectedProperty().bindBidirectional(model.areaOrExtProperty());
        chkRestrictToAreaMask.selectedProperty().bindBidirectional(model.restrictToAreaMaskProperty());
        chkThickenLines.selectedProperty().bindBidirectional(model.thickenLinesProperty());
        chkRemoveBoundaryFilaments.selectedProperty().bindBidirectional(model.removeBoundaryFilamentsProperty());
        chkCurvedFilaments.selectedProperty().bindBidirectional(model.curvedFilamentsProperty());
        chkAsStraightPieces.selectedProperty().bindBidirectional(model.asStraightPiecesProperty());
        chkLogScale.selectedProperty().bindBidirectional(model.logScaleProperty());

        cbSamples.getItems().setAll(Const.makeWizerMap().keySet());
        cbSamples.valueProperty().bindBidirectional(model.wizerSamplesProperty());
        cbSamples.getSelectionModel().selectFirst();


        getModel().initChangeListener();


    }


    @FXML
    private void onOrientations(ActionEvent event) {
        /*if (getParent() instanceof ImageDetail) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(0, 100);
                    getMainController().addDebugMessage("Start Orientations");
                    try {
                        List<BufferedImage> result = ((ImageDetail) getParent()).getModel().getDataImage().calculateOrientationFields(true);
                        if (result != null) {
                            //result should be shown in some kind of popup
                            Platform.runLater(() -> getMainController().<PopUpViewImages>openPopUp("/view/PopUpViewImages.fxml", "Orientations PopUp", controller -> controller.setImages(result.stream().map(bi -> SwingFXUtils.toFXImage(bi, null)).collect(Collectors.toList()), true)));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        getMainController().addDebugMessage(e);
                    }
                    getMainController().addDebugMessage("Finished Orientations");
                    updateProgress(100, 100);
                    return null;
                }
            };
            getMainController().runAsync(task);
        }*/

    }


    @FXML
    private void onViewWidthMap(ActionEvent event) {
        getMainController().<PopUpWidthMap>openPopUp("/view/PopUpWidthMap.fxml", "WidthMap-PopUp", (controller) -> {
            controller.initTolerance(getMainController().getModel().getProjectData().getSettings().getValue(Trace.tolerance));
        });
    }


    @FXML
    private void onWiZer(ActionEvent event) {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(0, 100);
                    getMainController().addDebugMessage("Start WiZer");
                    //check missing requirements
                    if (getMainController().getModel().getStackModel() == null ||
                            getMainController().getModel().getStackModel().getFilaments() == null ||
                            getMainController().getModel().getStackModel().getFilaments().getFilaments() == null ||
                            getMainController().getModel().getStackModel().getFilaments().getFilaments().size() == 0) {
                        getMainController().addDebugMessage("No Filaments currently stored, please process filaments first");
                        updateProgress(100, 100);
                        return null;
                    }
                    getMainController().addDebugMessage("Updated WiZer Parameters");
                    WrappedSiZer sizerer = new WrappedSiZer(getMainController().getModel().getStackModel().getFilaments().getFilaments());
                    BufferedImage[] result = sizerer.calculateWiZer(900, 600, 0.05, getMainController().getModel().getProjectData().getSettings());

                    getMainController().addDebugMessage("Calculated WiZer");
                    if (result != null) {
                        getMainController().addDebugMessage("Start Converting WiZer Images");
                        Platform.runLater(() -> {
                            getMainController().<PopUpViewImages>openPopUp("/view/PopUpViewImages.fxml", "Orientations PopUp", controller -> controller.setImages(Arrays.stream(result).map(bi -> SwingFXUtils.toFXImage(bi, null)).collect(Collectors.toList()), false));
                            getMainController().addDebugMessage("Finished Converting WiZer Images");
                        });
                    }
                    getMainController().addDebugMessage("Finished WiZer");
                    updateProgress(100, 100);
                    return null;
                }
            };
            getMainController().runAsync(task);
    }

    @Override
    protected void afterSetMainController(AbstractControl parent) {
        setParent(parent);
        initializeModel();
        getMainController().getModel().setLineSensorModel(model);

    }

    private void initializeModel() {
        Settings parameters = getMainController().getModel().getProjectData().getSettings();

        getModel().restrictToAreaMaskProperty().bindBidirectional(parameters.getPropertyAsBoolean(Bin.restrict));
        getModel().areaOrExtProperty().bindBidirectional(parameters.getPropertyAsBoolean(Bin.is_area_or_ext));

        getModel().asStraightPiecesProperty().bindBidirectional(parameters.getPropertyAsBoolean(Trace.split));
        getModel().curvedFilamentsProperty().bindBidirectional(parameters.getPropertyAsBoolean(Trace.curve));
        getModel().logScaleProperty().bindBidirectional(parameters.getPropertyAsBoolean(WiZer.log_view));

        getModel().lengthStraightPiecesProperty().bindBidirectional(parameters.getPropertyAsDouble(Trace.step));
        getModel().minAngleDifferenceProperty().bindBidirectional(parameters.getPropertyAsDouble(Trace.minangle));
        getModel().minAreaProperty().bindBidirectional(parameters.getPropertyAsDouble(Ori.min_area));
        getModel().minFilamentLengthProperty().bindBidirectional(parameters.getPropertyAsDouble(Trace.minlen));
        getModel().minFilamentsProperty().bindBidirectional(parameters.getPropertyAsDouble(Ori.min_filaments));
        getModel().minMeanValueProperty().bindBidirectional(parameters.getPropertyAsDouble(Bin.minmean));


        getModel().removeBoundaryFilamentsProperty().bindBidirectional(parameters.getPropertyAsBoolean(Trace.no_boundary));
        getModel().sigmaProperty().bindBidirectional(parameters.getPropertyAsDouble(Bin.sigma));
        getModel().thickenLinesProperty().bindBidirectional(parameters.getPropertyAsBoolean(Bin.thicken));
        getModel().toleranceProperty().bindBidirectional(parameters.getPropertyAsDouble(Trace.tolerance));



        getModel().binarizationMethodProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("directions")) {
                    parameters.setProperty(Bin.method, 1);
                    getModel().setMinStandardDeviation(((double) parameters.getValue(Bin.rod_significance)) / Bin.rod_significance.getScale());
                } else if (newValue.equals("area")) {
                    parameters.setProperty(Bin.method, 0);
                    getModel().setMinStandardDeviation(((double) parameters.getValue(Bin.area_significance)) / Bin.area_significance.getScale());
                }
            }
        });
        getModel().setBinarizationMethod((parameters.getValue(Bin.method) == 1) ? "directions" : "area");


        getModel().wizerSamplesProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                parameters.setProperty(WiZer.sample_size, Const.makeWizerMap().get(newValue));
            }
        });
        getModel().setWizerSamples(Misc.getKeyByValue(Const.makeWizerMap(), parameters.getValue(WiZer.sample_size)));
    }


    public AbstractControl getParent() {
        return parent;
    }

    public void setParent(AbstractControl parent) {
        this.parent = parent;
    }

    public LineSensorModel getModel() {
        return model;
    }

    public void setModel(LineSensorModel model) {
        this.model = model;
    }
}
