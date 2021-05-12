package fx.custom;


import ij.process.AutoThresholder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Arrays;

public class Area extends VBox {

    private ObjectProperty<EventHandler<ActionEvent>> onApplySingle;
    private ObjectProperty<EventHandler<ActionEvent>> onApplyAll;

    @FXML
    private CheckBox chkSingleImage;

    @FXML
    private ComboBox<String> cbMethods;
    @FXML
    private SliderSpinner sThreshold;
    @FXML
    private CheckBox chkManualThresholding;

    @FXML
    private Button btnApplySingle;
    @FXML
    private Button btnApplyAll;


    private ObjectProperty<int[]> histogram;
    private ObjectProperty<AutoThresholder.Method> method;
    private AutoThresholder thresholder;

    public Area() {
        super();
        method = new SimpleObjectProperty<>();
        histogram = new SimpleObjectProperty<>();
        thresholder = new AutoThresholder();
        onApplyAll = new SimpleObjectProperty<>();
        onApplySingle = new SimpleObjectProperty<>();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fx/custom/view/Area.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }


    @FXML
    private void initialize() {
        cbMethods.setItems(FXCollections.observableList(Arrays.asList(AutoThresholder.getMethods())));
        cbMethods.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                method.setValue(AutoThresholder.Method.valueOf(newValue));
                //update thresholds
                if (getHistogram() != null) {
                    if (!chkManualThresholding.isSelected())
                        sThreshold.setValue((double) thresholder.getThreshold(getMethod(), getHistogram()));
                }

            }
        });
        methodProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                cbMethods.getSelectionModel().select(newValue.name());
            }
        });

        histogram.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && getMethod() != null) {
                if (!chkManualThresholding.isSelected())
                    sThreshold.setValue((double) thresholder.getThreshold(getMethod(), newValue));
            }
        });

        btnApplySingle.onActionProperty().bind(onApplySingleProperty());
        btnApplyAll.onActionProperty().bind(onApplyAllProperty());

    }


    public AutoThresholder.Method getMethod() {
        return method.get();
    }

    public ObjectProperty<AutoThresholder.Method> methodProperty() {
        return method;
    }

    public void setMethod(AutoThresholder.Method method) {
        this.method.set(method);
    }

    public DoubleProperty getThresholdProperty() {
        return sThreshold.valueProperty();
    }

    public boolean thresholdExists() {
        return sThreshold != null && sThreshold.valueProperty() != null && sThreshold.valueProperty().getValue() != null;
    }

    public double getThreshold() {
        return sThreshold.valueProperty().get();
    }


    @FXML
    private void onApplySingle(ActionEvent event) {
        if (getOnApplySingle() != null) getOnApplySingle().handle(event);

    }

    @FXML
    private void onApplyAll(ActionEvent event) {
        if (getOnApplyAll() != null) getOnApplyAll().handle(event);
    }

    public EventHandler<ActionEvent> getOnApplySingle() {
        return onApplySingle.get();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onApplySingleProperty() {
        return onApplySingle;
    }

    public void setOnApplySingle(EventHandler<ActionEvent> onApplySingle) {
        this.onApplySingle.set(onApplySingle);
    }

    public EventHandler<ActionEvent> getOnApplyAll() {
        return onApplyAll.get();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onApplyAllProperty() {
        return onApplyAll;
    }

    public void setOnApplyAll(EventHandler<ActionEvent> onApplyAll) {
        this.onApplyAll.set(onApplyAll);
    }


    public int[] getHistogram() {
        return histogram.get();
    }

    public ObjectProperty<int[]> histogramProperty() {
        return histogram;
    }

    public void setHistogram(int[] histogram) {
        this.histogram.set(histogram);
    }

    public BooleanProperty isManualThresholdProperty() {
        return chkManualThresholding.selectedProperty();
    }

    public boolean isManualThreshold() {
        return chkManualThresholding.isSelected();
    }

    public void setSingleImageOnly(boolean val) {
        chkSingleImage.setSelected(val);
    }

    public boolean isSingleImageOnly() {
        return chkSingleImage.isSelected();
    }

    public BooleanProperty isSingleImageOnlyProperty() {
        return chkSingleImage.selectedProperty();
    }

}
