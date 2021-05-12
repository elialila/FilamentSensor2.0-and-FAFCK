package control;

import ij.IJ;
import ij.ImagePlus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import core.ProjectData;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ImageOverview extends AbstractControl {

    @FXML
    private GridPane imageGrid;


    private Image loadResized(File file) throws IOException {
        ImagePlus image = IJ.openImage(file.getAbsolutePath());
        BufferedImage tmpImage = image.getProcessor().resize(100).getBufferedImage();
        Image tmp = SwingFXUtils.toFXImage(tmpImage, null);
        image.close();
        return tmp;
    }


    public void setImageData(ProjectData projectData) {
        final ImageOverview self = this;

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<File> images = projectData.getImageFiles();
                int row = 0, column = 0;
                int counter = 0;
                updateProgress(0, images.size());
                for (File image : images) {
                    if (column == 3) {
                        column = 0;
                        row++;
                    }
                    Image tmp = loadResized(image);
                    //Image tmp = new Image(image.toURI().toString(), 100, 100, false, false);
                    ImageView tmpView = new ImageView(tmp);
                    tmpView.setFitWidth(100);
                    tmpView.setFitHeight(100);

                    Button tmpButton = new Button(image.getName(), tmpView);
                    tmpButton.setOnAction((event) -> MainControl.<ImageDetailStack>addDynamicContent(getMainController(), self, "/view/ImageDetailStack.fxml", (t) -> {
                        try {
                            t.setImages(Arrays.asList(image));
                        } catch (Exception e) {
                            e.printStackTrace();
                            getMainController().addDebugMessage(e);
                        }
                    }));
                    final int tmpCol = column, tmpRow = row;
                    Platform.runLater(() -> imageGrid.add(tmpButton, tmpCol, tmpRow));
                    column++;
                    counter++;
                    updateProgress(counter, images.size());
                }
                getMainController().addDebugMessage("Loading Done.");
                return null;
            }
        };
        getMainController().runAsync(task);
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {

    }
}
