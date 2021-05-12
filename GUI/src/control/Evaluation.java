package control;

import fx.custom.SliderSpinner;

import fx.custom.StackImageView;
import ij.ImagePlus;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import evaluation.EvaluationData;
import evaluation.Evaluator;
import core.image.BinaryImage;
import core.image.ImageWrapper;
import core.settings.Eval;
import util.IOUtils;
import util.ImageExporter;
import utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.UIUtils.handleDirectoryChooser;

public class Evaluation extends AbstractControl {


    private DoubleProperty widthWrapping;


    @FXML
    private CheckBox chkThickenLines;
    @FXML
    private SliderSpinner sMatchMinPixels;
    @FXML
    private TableColumn colPixelParent;
    @FXML
    private TableColumn colObjectParent;
    @FXML
    private ScrollPane root;

    @FXML
    private TableColumn<EvaluationDataModel, Integer> colNr;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colWhiteEval;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colWhiteTruth;
    @FXML
    private TableColumn<EvaluationDataModel, Double> colHitRate;
    @FXML
    private TableColumn<EvaluationDataModel, Double> colMissRate;
    @FXML
    private TableColumn<EvaluationDataModel, Double> colFnRate;
    @FXML
    private TableColumn<EvaluationDataModel, Double> colFpRate;

    @FXML
    private TableColumn<EvaluationDataModel, Integer> colWhiteMatches;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colFpMatches;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colFnMatches;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colObjectsEval;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colObjectsTruth;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colObjectsFound;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colObjectsMissed;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colObjectsFp;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colMultiMatchesOneToN;
    @FXML
    private TableColumn<EvaluationDataModel, Integer> colMultiMatchesNToOne;
    @FXML
    private TextField tGroundTruthImagePath;
    @FXML
    private ProgressBar pbEval;
    @FXML
    private TableView<EvaluationDataModel> tvResults;
    @FXML
    private StackImageView ivPixelCmp;
    @FXML
    private StackImageView ivObjectCmp;
    @FXML
    private TextArea taDebug;

    @FXML
    private HBox zoomContainer;
    @FXML
    private VBox containerStackScroller;


    private Supplier<Evaluator.ShapeType> shapeTypeSupplier;

    private Function<Integer, BinaryImage> toEvalFunction;
    private ImageWrapper stackSource;


    public Evaluation() {
        super();
        widthWrapping = new SimpleDoubleProperty(580);

    }

    public double getWidthWrapping() {
        return widthWrapping.get();
    }

    public DoubleProperty widthWrappingProperty() {
        return widthWrapping;
    }

    public void setWidthWrapping(double widthWrapping) {
        this.widthWrapping.set(widthWrapping);
    }

    @Override
    protected void afterSetMainController(AbstractControl parent) {

        chkThickenLines.selectedProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Eval.thickenLines));
        sMatchMinPixels.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(Eval.matchMinPixels));

    }

    @FXML
    private void onSearch(ActionEvent actionEvent) {
        handleDirectoryChooser("choose ground truth(image file)", tGroundTruthImagePath, actionEvent, false);
    }

    @FXML
    private void initialize() {
        widthWrapping.bind(root.widthProperty().subtract(10));

        colNr.setCellValueFactory((c) -> c.getValue().nrProperty().asObject());
        colWhiteMatches.setCellValueFactory(new PropertyValueFactory<>("whiteMatches"));
        colFpMatches.setCellValueFactory(new PropertyValueFactory<>("fpMatches"));
        colFnMatches.setCellValueFactory(new PropertyValueFactory<>("fnMatches"));
        colObjectsEval.setCellValueFactory(new PropertyValueFactory<>("objectsEval"));
        colObjectsTruth.setCellValueFactory(new PropertyValueFactory<>("objectsTruth"));
        colObjectsFound.setCellValueFactory(new PropertyValueFactory<>("objectsFound"));
        colObjectsMissed.setCellValueFactory(new PropertyValueFactory<>("objectsMissed"));
        colObjectsFp.setCellValueFactory(new PropertyValueFactory<>("objectsFP"));
        colMultiMatchesOneToN.setCellValueFactory(new PropertyValueFactory<>("multiMatchesOneToN"));
        colMultiMatchesNToOne.setCellValueFactory(new PropertyValueFactory<>("multiMatchesNToOne"));


        colWhiteEval.setCellValueFactory(new PropertyValueFactory<>("whiteEval"));
        colWhiteTruth.setCellValueFactory(new PropertyValueFactory<>("whiteTruth"));

        colHitRate.setCellValueFactory(new PropertyValueFactory<>("hitRate"));
        colMissRate.setCellValueFactory(new PropertyValueFactory<>("missRate"));
        colFnRate.setCellValueFactory(new PropertyValueFactory<>("fnRate"));
        colFpRate.setCellValueFactory(new PropertyValueFactory<>("fpRate"));

        //17 cols
        colNr.prefWidthProperty().bind(root.widthProperty().divide(17));
        colPixelParent.prefWidthProperty().bind(root.widthProperty().divide(9 / 17));
        colObjectParent.prefWidthProperty().bind(root.widthProperty().divide(7 / 17));

        ivPixelCmp.prefWidthProperty().bind(root.widthProperty().divide(2));
        ivObjectCmp.prefWidthProperty().bind(root.widthProperty().divide(2));
        ivPixelCmp.setHideScroller(true);
        ivObjectCmp.setHideScroller(true);
        ivPixelCmp.synchronize(ivObjectCmp);
        ivPixelCmp.setShowControls(false);
        ivObjectCmp.setShowControls(false);
        zoomContainer.getChildren().add(ivPixelCmp.getHBoxZoom());
        containerStackScroller.getChildren().add(ivPixelCmp.getVBoxScroller());
        containerStackScroller.managedProperty().bind(containerStackScroller.visibleProperty());


        colWhiteMatches.prefWidthProperty().bind(root.widthProperty().divide(14));
        colFpMatches.prefWidthProperty().bind(root.widthProperty().divide(14));
        colFnMatches.prefWidthProperty().bind(root.widthProperty().divide(14));
        colObjectsEval.prefWidthProperty().bind(root.widthProperty().divide(14));
        colObjectsTruth.prefWidthProperty().bind(root.widthProperty().divide(14));
        colObjectsFound.prefWidthProperty().bind(root.widthProperty().divide(14));
        colObjectsMissed.prefWidthProperty().bind(root.widthProperty().divide(14));
        colObjectsFp.prefWidthProperty().bind(root.widthProperty().divide(14));
        colMultiMatchesOneToN.prefWidthProperty().bind(root.widthProperty().divide(14));
        colMultiMatchesNToOne.prefWidthProperty().bind(root.widthProperty().divide(14));
        colHitRate.prefWidthProperty().bind(root.widthProperty().divide(14));
        colMissRate.prefWidthProperty().bind(root.widthProperty().divide(14));
        colFnRate.prefWidthProperty().bind(root.widthProperty().divide(14));
        colFpRate.prefWidthProperty().bind(root.widthProperty().divide(14));


        taDebug.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            taDebug.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });

    }

    private void message(String message) {
        Platform.runLater(() -> taDebug.appendText("\n" + message));
    }

    private void handleStartStack(ImageWrapper mask) {
        if (stackSource == null || toEvalFunction == null) return;

        Task<Void> t = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(10, 100);
                message("Start Stack Evaluation");
                if (stackSource.getSize() != mask.getSize()) {
                    message("Eval-Stack and Truth-Stack have different Stack-Size");
                    return null;
                }
                Platform.runLater(() -> {
                    ivPixelCmp.setMin(1);
                    ivPixelCmp.setMax(stackSource.getSize());
                    containerStackScroller.setVisible(true);
                    tvResults.getItems().clear();
                });


                List<EvaluationDataModel> results = IntStream.range(0, mask.getSize()).parallel().mapToObj(i -> {
                    Evaluator eval = new Evaluator();
                    BinaryImage toEval = toEvalFunction.apply(i);
                    BinaryImage binMask = new BinaryImage(mask.getEntryList().get(i).getProcessor().convertToByteProcessor());
                    EvaluationData result = null;
                    try {
                        result = eval.evaluate(toEval, binMask, shapeTypeSupplier.get(), getMainController().getModel().getProjectData().getSettings(), i);
                    } catch (Exception e) {
                        message("Element(" + i + ") has encountered an error (maybe no objects found)");
                        e.printStackTrace();
                        result = new EvaluationData();
                    }
                    return new EvaluationDataModel(result, i);
                }).collect(Collectors.toList());
                Platform.runLater(() -> tvResults.getItems().addAll(results));
                updateProgress(100, 100);

                ivPixelCmp.currentProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                        EvaluationData data = tvResults.getItems().get(newValue.intValue() - 1).getSource();
                        ivPixelCmp.setImage(SwingFXUtils.toFXImage(data.getDiffImagePixels().getBufferedImage(), null));
                        ivObjectCmp.setImage(SwingFXUtils.toFXImage(data.getDiffImageObjects().getBufferedImage(), null));
                    }
                });
                Platform.runLater(() -> {
                    ivPixelCmp.setCurrent(1);
                    ivPixelCmp.setImage(SwingFXUtils.toFXImage(tvResults.getItems().get(0).getSource().getDiffImagePixels().getBufferedImage(), null));
                    ivObjectCmp.setImage(SwingFXUtils.toFXImage(tvResults.getItems().get(0).getSource().getDiffImageObjects().getBufferedImage(), null));
                });


                message("Finished Stack Evaluation");
                return null;
            }
        };
        pbEval.progressProperty().bind(t.progressProperty());
        CompletableFuture.runAsync(t);
    }

    private void handleStartSingle(ImageWrapper mask) {
        Task<Void> t = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                BinaryImage toEval = toEvalFunction.apply(getMainController().getModel().getStackModel().getCurrentImage() - 1);
                if (toEval == null) {
                    message("toEval does not exist");
                    return null;
                }
                try {
                    BinaryImage binMask = new BinaryImage(mask.getImage().getProcessor().convertToByteProcessor());
                    updateProgress(20, 100);
                    Evaluator evaluator = new Evaluator();

                    new ImagePlus("TEst", toEval.getByteProcessor().duplicate());
                    /*
                    ImageWindow window=new ImageWindow(new ImagePlus("toEval",toEval.getByteProcessor().duplicate()));
                    window.setVisible(true);
                    ImageWindow window2=new ImageWindow(new ImagePlus("groundTruth",binMask.getByteProcessor().duplicate()));
                    window2.setVisible(true);
                    */

                    EvaluationData result = evaluator.evaluate(toEval, binMask, shapeTypeSupplier.get(), getMainController().getModel().getProjectData().getSettings(), 1);
                    updateProgress(80, 100);

                    EvaluationDataModel edm = new EvaluationDataModel(result, 0);

                    tvResults.getItems().clear();
                    tvResults.setUserData(result);
                    tvResults.getItems().add(edm);

                    ivPixelCmp.setImage(SwingFXUtils.toFXImage(result.getDiffImagePixels().getBufferedImage(), null));
                    ivObjectCmp.setImage(SwingFXUtils.toFXImage(result.getDiffImageObjects().getBufferedImage(), null));
                    updateProgress(100, 100);
                    message("finished evaluation");
                } catch (Exception e) {
                    message("finished evaluation with exception:\n" + e.getMessage());
                    e.printStackTrace();
                    updateProgress(100, 100);
                }
                return null;
            }
        };
        pbEval.progressProperty().bind(t.progressProperty());
        CompletableFuture.runAsync(t);
    }

    @FXML
    private void onStart(ActionEvent actionEvent) {
        if (tGroundTruthImagePath.getText().isEmpty()) {
            message("GroundTruth not set");
            return;
        }
        if (toEvalFunction == null) {
            message("Eval Function not Set");
            return;
        }
        ImageWrapper mask = new ImageWrapper(new File(tGroundTruthImagePath.getText()), getMainController().getModel().getProjectData().getSettings());

        if (mask.getSize() == 1) {
            //single eval
            handleStartSingle(mask);
        } else {
            //stack eval
            handleStartStack(mask);
        }
    }

    public void setToEval(ImageWrapper source, Function<Integer, BinaryImage> toEvalFunction, Supplier<Evaluator.ShapeType> shapeTypeSupplier) {
        this.toEvalFunction = toEvalFunction;
        this.stackSource = source;
        this.shapeTypeSupplier = shapeTypeSupplier;
    }

    public void settGroundTruthImagePath(String groundTruthImagePath) {
        tGroundTruthImagePath.setText(groundTruthImagePath);
    }


    @FXML
    private void onExportResults(ActionEvent actionEvent) {
        File directory = UIUtils.handleDirectoryChooser("Choose Export Directory(export files have static names)", null, actionEvent, true);
        if (directory != null && tvResults.getItems() != null && tvResults.getItems().size() > 0) {
            Task<Void> t = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(10, 100);
                    tvResults.getItems().parallelStream().forEach(evalDataModel -> {
                        EvaluationData evalData = evalDataModel.getSource();

                        ImageExporter.exportImage(evalData.getDiffImagePixels(), new File(directory.getAbsolutePath() + File.separator + "cmp_Eval_Truth_Pixels_" + evalDataModel.getNr() + ".png"));
                        ImageExporter.exportImage(evalData.getDiffImageObjects(), new File(directory.getAbsolutePath() + File.separator + "cmp_Eval_Truth_Objects_" + evalDataModel.getNr() + ".png"));
                        try {
                            IOUtils.writeFile(new File(directory.getAbsolutePath() + File.separator + "comparison_" + evalDataModel.getNr() + ".csv"), evalData.getCsv());

                        } catch (IOException e) {
                            message(e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    updateProgress(100, 100);
                    message("Finished Export.");
                    return null;
                }
            };
            pbEval.progressProperty().bind(t.progressProperty());
            CompletableFuture.runAsync(t);

        }
    }

    public static class EvaluationDataModel {

        private DoubleProperty hitRate;
        private DoubleProperty missRate;
        private DoubleProperty fpRate;
        private DoubleProperty fnRate;


        private IntegerProperty whiteEval;
        private IntegerProperty whiteTruth;

        private IntegerProperty fnMatches;
        private IntegerProperty fpMatches;
        private IntegerProperty whiteMatches;
        private IntegerProperty objectsEval;
        private IntegerProperty objectsTruth;
        private IntegerProperty objectsFound;
        private IntegerProperty objectsMissed;
        private IntegerProperty objectsFP;
        private IntegerProperty multiMatchesOneToN;
        private IntegerProperty multiMatchesNToOne;


        private EvaluationData source;
        private IntegerProperty nr;

        public EvaluationDataModel() {
            hitRate = new SimpleDoubleProperty();
            missRate = new SimpleDoubleProperty();
            fpRate = new SimpleDoubleProperty();
            fnRate = new SimpleDoubleProperty();
            fnMatches = new SimpleIntegerProperty();
            fpMatches = new SimpleIntegerProperty();
            whiteMatches = new SimpleIntegerProperty();
            objectsEval = new SimpleIntegerProperty();
            objectsTruth = new SimpleIntegerProperty();
            objectsFound = new SimpleIntegerProperty();
            objectsMissed = new SimpleIntegerProperty();
            objectsFP = new SimpleIntegerProperty();
            multiMatchesOneToN = new SimpleIntegerProperty();
            multiMatchesNToOne = new SimpleIntegerProperty();
            nr = new SimpleIntegerProperty();

            whiteEval = new SimpleIntegerProperty();
            whiteTruth = new SimpleIntegerProperty();
        }

        public EvaluationDataModel(double hitRate, double missRate, double fpRate, double fnRate,
                                   int fnMatches, int fpMatches, int whiteMatches, int objectsEval, int objectsTruth,
                                   int objectsFound, int objectsMissed, int objectsFP, int multiMatchesOneToN, int multiMatchesNToOne, int whiteEval, int whiteTruth) {
            this();
            setHitRate(hitRate);
            setMissRate(missRate);
            setFpRate(fpRate);
            setFnRate(fnRate);
            setFnMatches(fnMatches);
            setFpMatches(fpMatches);
            setWhiteMatches(whiteMatches);
            setObjectsEval(objectsEval);
            setObjectsTruth(objectsTruth);
            setObjectsFound(objectsFound);
            setObjectsMissed(objectsMissed);
            setObjectsFP(objectsFP);
            setMultiMatchesOneToN(multiMatchesOneToN);
            setMultiMatchesNToOne(multiMatchesNToOne);
            setWhiteEval(whiteEval);
            setWhiteTruth(whiteTruth);
        }

        public EvaluationDataModel(EvaluationData data, int idx) {
            this(data.getHitRate(),
                    data.getMissRate(),
                    data.getFpRate(),
                    data.getFnRate(),
                    data.getFnMatches(),
                    data.getFpMatches(),
                    data.getWhiteMatches(),
                    data.getObjectsEval(),
                    data.getObjectsTruth(),
                    data.getObjectsFound(),
                    data.getObjectsMissed(),
                    data.getObjectsFP(),
                    data.getMultiMatchesOneToN(),
                    data.getMultiMatchesNToOne(),
                    data.getWhiteEval(),
                    data.getWhiteTruth());
            this.source = data;
            setNr(idx);
        }

        public EvaluationData getSource() {
            return source;
        }

        public IntegerProperty nrProperty() {
            return nr;
        }

        public void setNr(int nr) {
            this.nr.set(nr);
        }

        public int getNr() {
            return nr.get();
        }

        public int getWhiteEval() {
            return whiteEval.get();
        }

        public IntegerProperty whiteEvalProperty() {
            return whiteEval;
        }

        public void setWhiteEval(int whiteEval) {
            this.whiteEval.set(whiteEval);
        }

        public int getWhiteTruth() {
            return whiteTruth.get();
        }

        public IntegerProperty whiteTruthProperty() {
            return whiteTruth;
        }

        public void setWhiteTruth(int whiteTruth) {
            this.whiteTruth.set(whiteTruth);
        }

        public double getHitRate() {
            return hitRate.get();
        }

        public DoubleProperty hitRateProperty() {
            return hitRate;
        }

        public void setHitRate(double hitRate) {
            this.hitRate.set(hitRate);
        }

        public double getMissRate() {
            return missRate.get();
        }

        public DoubleProperty missRateProperty() {
            return missRate;
        }

        public void setMissRate(double missRate) {
            this.missRate.set(missRate);
        }

        public double getFpRate() {
            return fpRate.get();
        }

        public DoubleProperty fpRateProperty() {
            return fpRate;
        }

        public void setFpRate(double fpRate) {
            this.fpRate.set(fpRate);
        }

        public double getFnRate() {
            return fnRate.get();
        }

        public DoubleProperty fnRateProperty() {
            return fnRate;
        }

        public void setFnRate(double fnRate) {
            this.fnRate.set(fnRate);
        }

        public int getFnMatches() {
            return fnMatches.get();
        }

        public IntegerProperty fnMatchesProperty() {
            return fnMatches;
        }

        public void setFnMatches(int fnMatches) {
            this.fnMatches.set(fnMatches);
        }

        public int getFpMatches() {
            return fpMatches.get();
        }

        public IntegerProperty fpMatchesProperty() {
            return fpMatches;
        }

        public void setFpMatches(int fpMatches) {
            this.fpMatches.set(fpMatches);
        }

        public int getWhiteMatches() {
            return whiteMatches.get();
        }

        public IntegerProperty whiteMatchesProperty() {
            return whiteMatches;
        }

        public void setWhiteMatches(int whiteMatches) {
            this.whiteMatches.set(whiteMatches);
        }

        public int getObjectsEval() {
            return objectsEval.get();
        }

        public IntegerProperty objectsEvalProperty() {
            return objectsEval;
        }

        public void setObjectsEval(int objectsEval) {
            this.objectsEval.set(objectsEval);
        }

        public int getObjectsTruth() {
            return objectsTruth.get();
        }

        public IntegerProperty objectsTruthProperty() {
            return objectsTruth;
        }

        public void setObjectsTruth(int objectsTruth) {
            this.objectsTruth.set(objectsTruth);
        }

        public int getObjectsFound() {
            return objectsFound.get();
        }

        public IntegerProperty objectsFoundProperty() {
            return objectsFound;
        }

        public void setObjectsFound(int objectsFound) {
            this.objectsFound.set(objectsFound);
        }

        public int getObjectsMissed() {
            return objectsMissed.get();
        }

        public IntegerProperty objectsMissedProperty() {
            return objectsMissed;
        }

        public void setObjectsMissed(int objectsMissed) {
            this.objectsMissed.set(objectsMissed);
        }

        public int getObjectsFP() {
            return objectsFP.get();
        }

        public IntegerProperty objectsFPProperty() {
            return objectsFP;
        }

        public void setObjectsFP(int objectsFP) {
            this.objectsFP.set(objectsFP);
        }

        public int getMultiMatchesOneToN() {
            return multiMatchesOneToN.get();
        }

        public IntegerProperty multiMatchesOneToNProperty() {
            return multiMatchesOneToN;
        }

        public void setMultiMatchesOneToN(int multiMatchesOneToN) {
            this.multiMatchesOneToN.set(multiMatchesOneToN);
        }

        public int getMultiMatchesNToOne() {
            return multiMatchesNToOne.get();
        }

        public IntegerProperty multiMatchesNToOneProperty() {
            return multiMatchesNToOne;
        }

        public void setMultiMatchesNToOne(int multiMatchesNToOne) {
            this.multiMatchesNToOne.set(multiMatchesNToOne);
        }
    }


}
