package control;

import core.FilterQueue;
import core.cell.DataFilaments;
import core.image.BinaryImage;
import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.Eval;
import evaluation.EvaluationData;
import evaluation.Evaluator;
import evaluation.FilamentEvaluator;
import filters.FilterAutomaticThreshold;
import fx.custom.SliderSpinner;
import fx.custom.StackImageView;
import ij.process.AutoThresholder;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.EvaluationDataModel;
import model.ImageDetailStackModel;
import util.IOUtils;
import util.ImageExporter;
import utils.UIUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.UIUtils.handleDirectoryChooser;

public class FilamentEvaluation extends AbstractControl {


    private final DoubleProperty widthWrapping;


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


    @FXML
    private ComboBox<String> cbProjectOne;
    @FXML
    private ComboBox<String> cbProjectTwo;
    @FXML
    private ComboBox<Integer> cbImageOne;
    @FXML
    private ComboBox<Integer> cbImageTwo;
    @FXML
    private Button btnComparison;
    @FXML
    private CheckBox chkAllImages;


    public FilamentEvaluation() {
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
        sMatchMinPixels.valueProperty().bindBidirectional(getMainController().getModel().getProjectData().getSettings().getProperty(Eval.matchMinPixels));

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

    private void handleStackComparison() {
        if (cbProjectOne.getSelectionModel().getSelectedItem() == null ||
                cbProjectTwo.getSelectionModel().getSelectedItem() == null) {
            getMainController().addDebugMessage("Some parameters are not Set (check the combo boxes)");
            return;
        }

        final ImageDetailStackModel modelOne = Objects.requireNonNull(getStackModelFromTab(getTabFromSelection(cbProjectOne)));
        final ImageDetailStackModel modelTwo = Objects.requireNonNull(getStackModelFromTab(getTabFromSelection(cbProjectTwo)));

        final Dimension dimEval=new Dimension(modelOne.getStackOrig().getWidth(),modelOne.getStackOrig().getHeight());
        final Dimension dimTruth=new Dimension(modelTwo.getStackOrig().getWidth(),modelTwo.getStackOrig().getHeight());

        final List<DataFilaments> filamentsListOne = modelOne.getStackOrig().getEntryList().stream().map(Entry::getDataFilament).collect(Collectors.toList());
        final List<DataFilaments> filamentsListTwo = modelTwo.getStackOrig().getEntryList().stream().map(Entry::getDataFilament).collect(Collectors.toList());

        final long time=System.currentTimeMillis();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                Platform.runLater(() -> {
                    ivPixelCmp.setMin(1);
                    ivPixelCmp.setMax(Math.min(modelOne.getSize(), modelTwo.getSize()));
                    containerStackScroller.setVisible(true);
                    tvResults.getItems().clear();
                });
                updateProgress(10, 100);
                message("Start Stack Evaluation");
                List<EvaluationDataModel> results = IntStream.range(0, Math.min(modelOne.getSize(), modelTwo.getSize())).parallel().mapToObj(i -> {
                    FilamentEvaluator filamentEvaluator = new FilamentEvaluator();
                    EvaluationData result = null;
                    try {
                        result = filamentEvaluator.compare(filamentsListOne.get(i).getFilaments(), filamentsListTwo.get(i).getFilaments(), dimEval, dimTruth,getMainController().getModel().getProjectData().getSettings());
                    } catch (Exception e) {
                        message("Element(" + i + ") has encountered an error (maybe no objects found)");
                        e.printStackTrace();
                        result = new EvaluationData();
                    }
                    return new EvaluationDataModel(result, i);
                }).collect(Collectors.toList());

                Platform.runLater(() -> tvResults.getItems().addAll(results));
                updateProgress(100, 100);

                ivPixelCmp.currentProperty().addListener((observableValue, oldValue, newValue) -> {
                    EvaluationData data = tvResults.getItems().get(newValue.intValue() - 1).getSource();
                    ivPixelCmp.setImage(SwingFXUtils.toFXImage(data.getDiffImagePixels().getBufferedImage(), null));
                    ivObjectCmp.setImage(SwingFXUtils.toFXImage(data.getDiffImageObjects().getBufferedImage(), null));
                });
                Platform.runLater(() -> {
                    ivPixelCmp.setCurrent(1);
                    ivPixelCmp.setImage(SwingFXUtils.toFXImage(tvResults.getItems().get(0).getSource().getDiffImagePixels().getBufferedImage(), null));
                    ivObjectCmp.setImage(SwingFXUtils.toFXImage(tvResults.getItems().get(0).getSource().getDiffImageObjects().getBufferedImage(), null));
                });
                message("Finished Stack Evaluation");
                message("Evaluation time taken:"+((System.currentTimeMillis()-time)/1000)+"s");
                return null;
            }
        };
        pbEval.progressProperty().bind(task.progressProperty());
        CompletableFuture.runAsync(task);
    }


    private void handleSingleComparison() {
        //start comparison
        if (cbProjectOne.getSelectionModel().getSelectedItem() == null ||
                cbProjectTwo.getSelectionModel().getSelectedItem() == null ||
                cbImageOne.getSelectionModel().getSelectedItem() == null ||
                cbImageTwo.getSelectionModel().getSelectedItem() == null) {

            getMainController().addDebugMessage("Some parameters are not Set (check the combo boxes)");
            return;

        }
        message("Start Evaluation");
        final long time=System.currentTimeMillis();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                try {
                    FilamentEvaluator filamentEvaluator = new FilamentEvaluator();
                    //get filaments of both images
                    updateProgress(10, 100);
                    ImageDetailStackModel modelOne = Objects.requireNonNull(getStackModelFromTab(getTabFromSelection(cbProjectOne)));
                    ImageDetailStackModel modelTwo = Objects.requireNonNull(getStackModelFromTab(getTabFromSelection(cbProjectTwo)));

                    final Dimension dimEval=new Dimension(modelOne.getStackOrig().getWidth(),modelOne.getStackOrig().getHeight());
                    final Dimension dimTruth=new Dimension(modelTwo.getStackOrig().getWidth(),modelTwo.getStackOrig().getHeight());


                    List<DataFilaments> filamentsListOne = modelOne.getStackOrig().getEntryList().stream().map(Entry::getDataFilament).collect(Collectors.toList());
                    List<DataFilaments> filamentsListTwo = modelTwo.getStackOrig().getEntryList().stream().map(Entry::getDataFilament).collect(Collectors.toList());

                    EvaluationData result = filamentEvaluator.compare(filamentsListOne.get(cbImageOne.getSelectionModel().getSelectedItem()).getFilaments(),
                            filamentsListTwo.get(cbImageTwo.getSelectionModel().getSelectedItem()).getFilaments(), dimEval, dimTruth,getMainController().getModel().getProjectData().getSettings());
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
                message("Evaluation time taken:"+((System.currentTimeMillis()-time)/1000)+"s");
                return null;
            }
        };
        pbEval.progressProperty().bind(task.progressProperty());
        CompletableFuture.runAsync(task);
    }


    @FXML
    private void onStart(ActionEvent actionEvent) {
        Platform.runLater(() -> {//run on fx thread
            if (!chkAllImages.isSelected()) {
                //single eval
                handleSingleComparison();
            } else {
                //stack eval
                handleStackComparison();
            }
        });
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

    //region Helper Methods
    private ImageDetailStackModel getStackModelFromTab(Tab tab) {
        //this node should be the DetailStack or the DetailView(which is also the DetailStack)
        Node node = tab.getContent();
        if (node.getUserData() != null && node.getUserData() instanceof ImageDetailStack) {
            //controller exists, and it is the right one, get image data and fill the ComboBox
            return ((ImageDetailStack) node.getUserData()).getModel();
        }
        return null;
    }

    private Tab getTabFromSelection(ComboBox<String> cbProject) {
        String selectedItem = cbProject.getSelectionModel().getSelectedItem();
        //map the string to the actual content
        Tab tab = getMainController().getTpContent().getTabs().stream().filter(tmpTab -> tmpTab.getText().equals(selectedItem)).findAny().orElse(null);
        return tab;
    }

    //endregion
    //region ComboBoxes
    public void updateProjectComboBox() {
        //for use in other task (non fx threads) wrap in Platform::runLater
        Platform.runLater(() -> {
            List<String> projects = getMainController().getTpContent().getTabs().stream().map(Tab::getText).filter(text -> text.toLowerCase().contains("detail")).collect(Collectors.toList());
            cbProjectOne.getItems().clear();
            cbProjectOne.getItems().addAll(projects);
            cbProjectTwo.getItems().clear();
            cbProjectTwo.getItems().addAll(projects);
        });
    }

    private void fillComboBoxImage(ComboBox<Integer> cbImage, ComboBox<String> cbProject) {
        cbImage.getItems().clear();
        Tab tab = getTabFromSelection(cbProject);
        if (tab != null) {
            ImageDetailStackModel model = getStackModelFromTab(tab);
            if (model != null && model.getStackOrig() != null) {
                int size = model.getStackOrig().getSize();
                IntStream.range(0, size).forEach(i -> cbImage.getItems().add(i));
            }
        }
    }

    @FXML
    private void onProjectOneSelected(ActionEvent event) {
        fillComboBoxImage(cbImageOne, cbProjectOne);
    }

    @FXML
    private void onProjectTwoSelected(ActionEvent event) {
        fillComboBoxImage(cbImageTwo, cbProjectTwo);
    }

    @FXML
    private void onComboBoxImageSelected(ActionEvent event) {
        //check if both are selected, then enable the button
        Integer one = cbImageOne.getSelectionModel().getSelectedItem();
        Integer two = cbImageTwo.getSelectionModel().getSelectedItem();

        if ((one != null && two != null)||chkAllImages.isSelected()) {
            btnComparison.setDisable(false);
        }
    }
    //endregion


}
