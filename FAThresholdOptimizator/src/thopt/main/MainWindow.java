package thopt.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainWindow extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        System.out.println("MainWindow::start() --- hash=" + this.hashCode());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/thopt/view/ThreshOptimizer.fxml"));
        try {
            Parent root = fxmlLoader.load();
            stage.setTitle("Threshold Optimizer");
            stage.setScene(new Scene(root, 1280, 720));
            //stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
            stage.show();
            stage.setOnCloseRequest((e) -> Platform.exit());

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }


    }


    public static void main(String[] args) {

        MainWindow.launch(args);
    }
}
