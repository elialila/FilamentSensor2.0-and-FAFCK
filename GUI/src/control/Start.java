package control;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import java.io.File;


public class Start extends AbstractControl {

    @FXML
    private void handleNewProject(ActionEvent event) {
        MainControl.addDynamicContent(getMainController(), this, "/view/Project.fxml", null);
    }

    @FXML
    private void handleLoadProject(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("choose were the project-file should be stored");
        File projectFile = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());
        if (projectFile != null) {
            getMainController().getModel().getProjectModel().setProjectFile(projectFile.getAbsolutePath());
            try {
                getMainController().getModel().initializeLoadProject();
                MainControl.addDynamicContent(getMainController(), this, "/view/Project.fxml", null);

            } catch (Exception e) {
                getMainController().addDebugMessage(e);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void afterSetMainController(AbstractControl parent) {

    }
}
