package utils;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import util.Annotations;

import java.io.File;

public class UIUtils {

    /**
     * @param text               text in the headline of the chooser
     * @param tText              TextField which contains the path
     * @param event              event from button which lead to opening the file chooser
     * @param isDirectoryChooser if true a directory is choosen if false it's a file chooser
     *                           Handles the logic behind file/directory chooser
     */
    public static File handleDirectoryChooser(String text, @Annotations.Nullable TextField tText, ActionEvent event, boolean isDirectoryChooser) {
        if (isDirectoryChooser) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(text);
            File directory = directoryChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
            if (directory != null && tText != null) {
                tText.setText(directory.getAbsolutePath());
            }
            return directory;
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(text);
            File file = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());
            if (file != null && tText != null) tText.setText(file.getAbsolutePath());
            return file;
        }
    }

}
