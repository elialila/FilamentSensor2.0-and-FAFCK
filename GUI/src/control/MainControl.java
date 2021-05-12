package control;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.ImageDetailStackModel;
import model.MainModel;
import core.ProjectData;
import core.FilamentSensor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainControl {


    @FXML
    private Pane content;

    @FXML
    private TextArea taDebug;


    @FXML
    private MenuItem navToStart;
    @FXML
    private MenuItem navToImageOverview;
    @FXML
    private MenuItem navToProject;
    @FXML
    private MenuItem navToImageStack;
    @FXML
    private ProgressBar pbProgress;

    private MainModel model;


    private Map<String, AbstractControl> registeredControls;


    public MainControl() {
        super();
        model = new MainModel();
        registeredControls = new HashMap<>();
    }

    public AbstractControl getRegisteredControl(String name) {
        return registeredControls.get(name);
    }

    public Map<String, AbstractControl> getRegisteredControls() {
        return registeredControls;
    }

    public MainModel getModel() {
        return model;
    }

    public void setModel(MainModel model) {
        this.model = model;
    }


    private ProgressBar getPbProgress() {
        return pbProgress;
    }

    public void setPbProgress(ProgressBar pbProgress) {
        this.pbProgress = pbProgress;
    }

    public Pane getDynamicContentPane() {
        return content;
    }

    public TextArea getDebugTextArea() {
        return taDebug;
    }

    public void setDebugTextArea(TextArea ta) {
        taDebug = ta;
    }

    private ExecutorService executorService;

    private void setExecutorService(ExecutorService service) {
        this.executorService = service;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }


    @FXML
    private void initialize() {
        setExecutorService(Executors.newCachedThreadPool());
        getPbProgress().setProgress(0);

        getDebugTextArea().textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> {
            getDebugTextArea().setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });
        addDynamicContent(this, null, "/view/Project.fxml", null);

        navToImageOverview.disableProperty().bind(getModel().getProjectModel().imageDirectoryProperty().isEmpty());
        //navToProject.disableProperty().bind(getModel().projectDataProperty().isNull());
        navToImageStack.disableProperty().bind(getModel().getProjectModel().imageDirectoryProperty().isEmpty());
        addDebugMessage("You can see notices and error messages in this text area");

        FilamentSensor.ERROR = true;
        FilamentSensor.setErrorStream(this::addDebugMessage);


    }

    public void addDebugMessage(String message) {
        Platform.runLater(() -> getDebugTextArea().appendText("\n" + message));
    }

    public void addDebugMessage(Throwable ex) {
        addDebugMessage(ex.getMessage() + " Exception:" + ex.getClass() + " Cause:" + ex.getCause());
        ex.printStackTrace();
    }


    public CompletableFuture<Void> runAsync(Task<Void> task) {
        getPbProgress().progressProperty().bind(task.progressProperty());
        return CompletableFuture.runAsync(task);
        //new Thread(task).start();
    }

    public CompletableFuture<Void> runAsync(Task<Void> task, ProgressBar progress) {
        progress.progressProperty().bind(task.progressProperty());
        return CompletableFuture.runAsync(task);
    }


    public static <T extends AbstractControl> void addDynamicContent(MainControl mainController, AbstractControl parent, String view, Consumer<T> initializationMethod) {
        Platform.runLater(() -> {
            mainController.getDynamicContentPane().getChildren().clear();
            FXMLLoader loader = new FXMLLoader(mainController.getClass().getResource(view));
            try {
                Node pane = loader.load();
                T controller = loader.getController();
                controller.setMainController(mainController, parent);
                if (initializationMethod != null) initializationMethod.accept(controller);
                VBox.setVgrow(pane, Priority.ALWAYS);
                mainController.getDynamicContentPane().getChildren().add(pane);
                //mainController.getDynamicContentPane().getChildren().add(pane,0,0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }


    @FXML
    private void onNavigateToStart(ActionEvent event) {
        addDynamicContent(this, null, "/view/Start.fxml", null);
    }


    @FXML
    private void onNavigateToImageOverView(ActionEvent event) {
        if (getModel().getProjectModel().getImageDirectory() != null)
            MainControl.<ImageOverview>addDynamicContent(this, null, "/view/ImageOverview.fxml", (t) -> {
                t.setImageData(getModel().getProjectData());
        });
    }

    @FXML
    private void onNavigateToFilterQueue(ActionEvent event) {
        MainControl.addDynamicContent(this, null, "/view/PreprocessingFilterQueue.fxml", null);
    }

    @FXML
    private void onNavigateToImageDetailStack(ActionEvent event) {

        if (getModel().getStackModel() == null) {
            File files = new File(getModel().getProjectModel().getImageDirectory());
            if (!files.exists() || !files.isDirectory()) {
                addDebugMessage("Image Folder is not a directory or doesn't exist");
            } else {
                ImageDetailStackModel idsm = new ImageDetailStackModel();
                getModel().setStackModel(idsm);
                MainControl.addDynamicContent(this, null, "/view/ImageDetailStack.fxml", null);
            }
        } else {
            MainControl.addDynamicContent(this, null, "/view/ImageDetailStack.fxml", null);
        }


    }





    @FXML
    private void onNavigateToProject(ActionEvent event) {
        ProjectData project = getModel().getProjectData();
        if (project != null) {
            MainControl.addDynamicContent(this, null, "/view/Project.fxml", null);
        }
    }

    @FXML
    private void onPreferencesBatchProcessing(ActionEvent event) {
        MainControl.addDynamicContent(this, null, "/view/PreferencesBatchProcessing.fxml", null);
    }

    @FXML
    private void onNotes(ActionEvent event) {
        MainControl.addDynamicContent(this, null, "/view/Notes.fxml", null);
    }


    public <T extends AbstractControl> void openPopUp(String view, String title, Consumer<T> initFunction) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(view));
        try {
            Scene dialogScene = new Scene(loader.load());
            Stage inputStage = new Stage();
            inputStage.getIcons().add(new Image(getClass().getResourceAsStream("/view/icon.png")));
            inputStage.setTitle(title);
            inputStage.initModality(Modality.WINDOW_MODAL);
            inputStage.setScene(dialogScene);
            loader.<T>getController().setMainController(this, null);
            initFunction.accept(loader.getController());
            inputStage.show();
        } catch (IOException e) {
            this.addDebugMessage(e);
            e.printStackTrace();
        }
    }



}
