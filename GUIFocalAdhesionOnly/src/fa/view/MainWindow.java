package fa.view;


import control.Evaluation;
import control.ImageDetailStack;
import control.MainControl;
import core.settings.FocAdh;
import fa.control.FocalAdhesionProcessing;
import fa.model.ExtMainModel;
import ij.Prefs;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.ImageDetailStackModel;
import model.MainModel;
import util.ProcessingUtils;
import core.cell.plugins.CellPluginBenjamin;
import core.settings.Settings;
import evaluation.Evaluator;
import core.image.BinaryImage;
import core.image.ImageWrapper;
import core.settings.Config;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static utils.UIUtils.handleDirectoryChooser;


/**
 * Application Description
 * <p>
 * Abstract description:
 * <p>
 * The Application should get 2 ome-tif stacks as input, focal adhesions and stress fibers
 * The stress fibers stack can be put into the filament-sensor for processing and getting the filaments
 * the focal adhesion stack should be preprocessed (noise removal, line sensor?)
 * <p>
 * after this processing steps the left over white pixels in the focal adhesion stack should be
 * put into relation with starting/end-points of filaments, to verify the filaments
 * <p>
 * the ui should show both stacks and color-code verified and non-verified filaments (for example verified are orange, non-verified are yellow)
 * <p>
 * <p>
 * Additional Infos:
 * <p>
 * Include FilamentSensor as lib
 * <p>
 * read ome-tif files, convert/put the items into an ImageWrapper and Run processings mentioned above
 * <p>
 * <p>
 * Concerns:
 */
public class MainWindow extends Application {
    private static boolean DEBUG = true;


    @FXML
    private Node faProcessing;
    @FXML
    private Node filament;
    @FXML
    private FocalAdhesionProcessing faProcessingController;
    @FXML
    private ImageDetailStack filamentController;


    private MainModel model;
    private MainControl fakeControl;


    @FXML
    private TextField tFocalAdhesionImagePath;
    @FXML
    private TextField tStressFibersImagePath;
    @FXML
    private TextArea taDebug;
    @FXML
    private ProgressBar pbProgress;
    @FXML
    private TabPane tpContainer;

    @FXML
    private ComboBox<Evaluator.ShapeType> cbType;
    @FXML
    private TextField tBinarySourceImagePath;
    @FXML
    private TextField tBinaryTruthImagePath;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Focal Adhesion");
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();
    }


    @FXML
    private void handleOnSearchFocalAdhesionImage(ActionEvent event) {
        handleDirectoryChooser("choose the focal adhesion image(single image or ome.tif stack)", tFocalAdhesionImagePath, event, false);

    }

    @FXML
    private void handleOnSearchBinarySourceImage(ActionEvent event) {
        handleDirectoryChooser("choose the source binary image", tBinarySourceImagePath, event, false);

    }


    @FXML
    private void handleOnSearchStressFibersImage(ActionEvent event) {
        handleDirectoryChooser("choose the stress fibers image(single image or ome.tif stack)", tStressFibersImagePath, event, false);
    }

    @FXML
    private void onLoad(ActionEvent event) {
        //resolved?
        /**error when loading stacks more than once (load a stack, do something, load another one ...)
         stack slider gets added every time (load 2 times = 2 stack sliders)
         images will get strange (inverted)
         stacks are not really reset? filaments are still stored, FA-Separations are stored etc.
         on load has to reset everything(or probably better setImages in FocalAdhesionProcessing and filamentController)
         */

        try {
            System.out.println("MainWindow::onLoad():" + model.getProjectModel());
            String file = (tFocalAdhesionImagePath.getText() != null && !tFocalAdhesionImagePath.getText().isEmpty()) ? tFocalAdhesionImagePath.getText() :
                    tStressFibersImagePath.getText();

            //fix for only using one file; this way only filament stuff or only fa stuff can be done without exceptions
            //not completely without exceptions but without fatal exceptions
            if (tFocalAdhesionImagePath.getText() == null || tFocalAdhesionImagePath.getText().isEmpty() ||
                    tStressFibersImagePath.getText() == null || tStressFibersImagePath.getText().isEmpty()) {
                tFocalAdhesionImagePath.setText(file);
                tStressFibersImagePath.setText(file);

            }


            model.getProjectModel().setImageDirectory(new File(file).getParent());
            model.initializeProjectData();

            faProcessingController.setImages((v) -> {
                Platform.runLater(() -> {
                    System.out.println("MainWindow::onLoad() --- faProcessingController callback");
                    filamentController.setImages();
                    //filament.setDisable(false);
                    //faProcessing.setDisable(false);
                    tpContainer.getSelectionModel().select(1);
                    System.out.println("MainWindow::onLoad() --- faProcessingController callback-finished");
                    fakeControl.addDebugMessage("load finished");
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            fakeControl.addDebugMessage(e);
            //show exception somewhere
        }


    }

    private static void initSavedFilterQueues() {
        Settings dp = new Settings();
        //create default filter lists

        File filtersDirectory = Config.getInstance().getFiltersDirectory();
        File outputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + "default.xml");
        try {
            XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
            encoder.writeObject(ProcessingUtils.getDefaultPreprocessingFilterQueue(dp));
            encoder.flush();
            encoder.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        outputFile = new File(filtersDirectory.getAbsolutePath() + File.separator + "simple.xml");
        try {
            XMLEncoder encoder = new XMLEncoder(new FileOutputStream(outputFile));
            encoder.writeObject(ProcessingUtils.getSimpleFilterQueue(dp));
            encoder.flush();
            encoder.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initConfig() {

        File configFile = new File(System.getProperty("user.home") + File.separator + ".filamentsensor" + File.separator + "application-settings.xml");
        configFile.getParentFile().mkdirs();
        if (configFile.exists()) {
            try {
                Config.load(configFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Config.getInstance().init();
            Config.getInstance().setConfigurationFile(configFile.getAbsolutePath());
            try {
                Config.getInstance().store();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        initSavedFilterQueues();

    }


    public static void main(String[] args) {
        Prefs.blackBackground = true;
        initConfig();
        MainWindow.launch(args);
        //debugFASeparation();


    }


    private static void debugTifColorProblem() {
        //the problem is: a tif image is officially a 16bit gray scale but it's red/green
        //so far it is not a problem, the filament-sensor works on it, focal adhesion does work too
        //if the thresholding is adjusted (otsu is working very poor on that image)
        //but when exporting(focal adhesion overlay or other images with marked areas) the highlighted parts are black
        //(like it is in a non RGB image), but the images should be converted to rgb at the export


    }





    @FXML
    private void onLoadFiles(ActionEvent event) {
        //project load button


    }

    @FXML
    private void initialize() {

        cbType.getItems().add(Evaluator.ShapeType.dotLike);
        cbType.getItems().add(Evaluator.ShapeType.lineLike);
        cbType.getSelectionModel().select(0);

        //init the sub views (project, focal adhesion, filament)
        model = new ExtMainModel();
        model.getProjectData().setPlugin(new CellPluginBenjamin());
        model.setStackModel(new ImageDetailStackModel());
        fakeControl = new MainControl();
        fakeControl.setModel(model);
        fakeControl.setDebugTextArea(taDebug);
        fakeControl.setPbProgress(pbProgress);
        //bind path's for files
        tFocalAdhesionImagePath.textProperty().bindBidirectional(model.getProjectModel().fileFocalAdhesionsProperty());
        tStressFibersImagePath.textProperty().bindBidirectional(model.getProjectModel().fileStressFibersProperty());

        //FILAMENT PART
        filamentController.setMainController(fakeControl, null);
        //END FILAMENT PART

        //FA PART
        faProcessingController.setMainController(fakeControl, null);
        //END FA PART


        //for debugging
        //tFocalAdhesionImagePath.textProperty().set("C:\\Users\\Andreas\\IdeaProjects\\FilamentSensorNew\\test\\testFocalAdhesion\\orig\\FocalAdhesion.ome.tif");
        //tStressFibersImagePath.textProperty().set("C:\\Users\\Andreas\\IdeaProjects\\FilamentSensorNew\\test\\testFocalAdhesion\\orig\\StressFibers.ome.tif");
        if (DEBUG) {
            //tFocalAdhesionImagePath.textProperty().set("D:\\Test\\TestFA.tif");
            //tStressFibersImagePath.textProperty().set("D:\\Test\\TestFiber.tif");

            tFocalAdhesionImagePath.textProperty().set("D:\\Dokumente\\Arbeit Uni Göttingen\\Daten\\Zelle4\\Conf_12\\FA\\17_FA_original.png");
            tStressFibersImagePath.textProperty().set("D:\\Dokumente\\Arbeit Uni Göttingen\\Daten\\Zelle4\\Conf_12\\Fiber\\17_original.png");
        }

    }

    @FXML
    private void handleOnEvaluate(ActionEvent actionEvent) {
        fakeControl.<Evaluation>openPopUp("/view/Evaluation.fxml", "Evaluate Focal Adhesion's", (control) -> {
            control.settGroundTruthImagePath(tBinaryTruthImagePath.getText());
            ImageWrapper wrapper = new ImageWrapper(new File(tBinarySourceImagePath.getText()), model.getProjectData().getSettings());
            control.setToEval(wrapper, (i) ->
                            new BinaryImage(wrapper.getEntryList().get(i).getProcessor().convertToByteProcessor())
                    , () -> cbType.getSelectionModel().getSelectedItem());
        });

    }

    @FXML
    private void handleOnSearchBinaryTruthImage(ActionEvent actionEvent) {
        handleDirectoryChooser("choose the ground truth binary image", tBinaryTruthImagePath, actionEvent, false);
    }


    @FXML
    private void onLoadBinaryMask(ActionEvent actionEvent) {

        try {
            System.out.println("MainWindow::onLoad():" + model.getProjectModel());
            model.getProjectModel().setImageDirectory(new File(tFocalAdhesionImagePath.textProperty().get()).getParent());
            model.initializeProjectData();
            model.getProjectData().getSettings().setProperty(FocAdh.doClosing, 0);
            model.getProjectData().getSettings().setProperty(FocAdh.doFillHoles, 0);
            faProcessingController.setImages((v) -> {
                Platform.runLater(() -> {
                    System.out.println("MainWindow::onLoad() --- faProcessingController callback");
                    filamentController.setImages();
                    //filament.setDisable(false);
                    //faProcessing.setDisable(false);
                    tpContainer.getSelectionModel().select(1);
                    System.out.println("MainWindow::onLoad() --- faProcessingController callback-finished");
                    fakeControl.addDebugMessage("load finished");
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            fakeControl.addDebugMessage(e);
            //show exception somewhere
        }

    }
}
