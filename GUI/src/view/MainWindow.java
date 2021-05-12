package view;

import control.MainControl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.MainModel;

import java.io.IOException;

public class MainWindow extends Application {

    private boolean debug = false;
    private static MainModel model;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        System.out.println("MainWindow::start() --- hash=" + this.hashCode());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        try {
            Parent root = fxmlLoader.load();
            if (MainWindow.model != null) fxmlLoader.<MainControl>getController().setModel(MainWindow.model);
            primaryStage.setTitle("Filament Sensor");
            primaryStage.setScene(new Scene(root, 1280, 720));
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
            primaryStage.show();
            primaryStage.setOnCloseRequest((e) -> Platform.exit());

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }


    }

    public void open(String[] args) {
        launch(args);
    }

    public void open(String[] args, MainModel model) {
        MainWindow.model = model;
        launch(args);
    }


    public void openDebug(String[] args, MainModel model) {
        debug = true;
        MainWindow.model = model;
        launch(args);


    }
}
