package control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PopUpViewImages extends AbstractControl {

    @FXML
    private ImageView imageView;
    @FXML
    private Button btnStore;

    private List<Image> images;

    private IntegerProperty count;
    private IntegerProperty length;

    @FXML
    private Text txtFrom;
    @FXML
    private Text txtTo;

    public PopUpViewImages() {
        count = new SimpleIntegerProperty(0);
        length = new SimpleIntegerProperty();
    }


    public void setImages(List<Image> images, boolean store) {
        this.images = images;
        length.setValue(images.size());
        btnStore.setVisible(store);
        imageView.setImage(images.get(0));
    }


    @FXML
    private void initialize() {
        btnStore.setVisible(false);
        txtFrom.textProperty().bind(Bindings.add(1, countProperty()).asString());
        txtTo.textProperty().bind(lengthProperty().asString());
    }


    @FXML
    private void onPrevious(ActionEvent event) {
        if (getCount() - 1 < 0) setCount(getLength() - 1);
        else {
            setCount(getCount() - 1);
        }
        imageView.setImage(images.get(getCount()));
    }

    @FXML
    private void onNext(ActionEvent event) {
        setCount((getCount() + 1) % getLength());
        imageView.setImage(images.get(getCount()));
    }

    @FXML
    private void onStore(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose the location and file-name where you want to store the image");
        File imageFile = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());

        BufferedImage bImage = SwingFXUtils.fromFXImage(images.get(getCount()), null);
        try {
            ImageIO.write(bImage, "png", imageFile);
        } catch (IOException e) {
            getMainController().addDebugMessage(e);
        }


    }

    public int getCount() {
        return count.get();
    }

    public IntegerProperty countProperty() {
        return count;
    }

    public int getLength() {
        return length.get();
    }

    public IntegerProperty lengthProperty() {
        return length;
    }

    private void setCount(int count) {
        this.count.set(count);
    }

    @Override
    protected void afterSetMainController(AbstractControl parent) {

    }
}
