package fx.custom;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * throwing stackoverflow exception when there are too many values (for example:scale 1 to 1000000,step=1)
 * change to avoid this exception
 */
public class SliderSpinner extends HBox {

    @FXML
    private Spinner<Double> spinner;
    @FXML
    private Slider slider;

    private DoubleProperty value;

    private StringProperty type;


    public final static String TypeInt = "Integer";
    public final static String TypeDouble = "Double";


    public SliderSpinner(double min, double max) {
        this();
        slider.setMax(max);
        slider.setMin(min);
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max));
    }


    public SliderSpinner() {
        super();

        type = new SimpleStringProperty();
        value = new SimpleDoubleProperty();
        spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        slider = new Slider();
        slider.setPrefWidth(200);
        HBox.setHgrow(slider, Priority.ALWAYS);


        slider.majorTickUnitProperty().bind(Bindings.divide(maxProperty(), 5));
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);


        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && ((value.getValue() == null) || (value.getValue() != null && !value.getValue().equals(newValue)))) {
                if (TypeInt.equals(getType())) {
                    value.setValue((double) newValue.intValue());
                } else {
                    value.setValue(newValue.doubleValue());
                }
            }
        });

        spinner.valueFactoryProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                newValue.valueProperty().addListener((ob, ov, nv) -> {
                    if (nv != null && (value.getValue() == null || !value.getValue().equals(nv))) {
                        if (TypeInt.equals(getType())) {
                            value.setValue((double) nv.intValue());
                        } else {
                            value.setValue(nv);
                        }
                    }

                });
            }
        });

        value.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && (spinner.getValue() == null || !spinner.getValue().equals(newValue))) {
                spinner.getValueFactory().setValue(value.getValue());
            }
            if (newValue != null && slider.getValue() != newValue.doubleValue())
                slider.setValue(newValue.doubleValue());
        });


        this.getChildren().add(slider);
        this.getChildren().add(spinner);
    }


    @FXML
    private void initialize() {

    }


    /**
     * Initialize Ranges from Slider and Spinner
     *
     * @param min
     * @param max
     * @param step
     */
    public void initRanges(int min, int max, int step) {
        spinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, value.getValue(), step));
        slider.setMin(min);
        slider.setMax(max);
        slider.setMinorTickCount(step);
        slider.setValue(value.getValue());
    }


    public SpinnerValueFactory<Double> getValueFactory() {
        return spinner.getValueFactory();
    }

    public ObjectProperty<? extends SpinnerValueFactory<Double>> valueFactoryProperty() {
        return spinner.valueFactoryProperty();
    }

    public void setValueFactory(SpinnerValueFactory<Double> valueFactory) {
        spinner.setValueFactory(valueFactory);
    }


    public Number getValue() {
        return value.get();
    }

    public DoubleProperty readOnlyValueProperty() {
        return value;
    }

    public DoubleProperty valueProperty() {
        return slider.valueProperty();
    }

    public void setValue(Double value) {
        slider.valueProperty().set(value);
        //this.value.set(value);
    }


    public double getMin() {
        return slider.minProperty().get();
    }

    public DoubleProperty minProperty() {
        return slider.minProperty();
    }

    public void setMin(double min) {
        slider.minProperty().setValue(min);
    }

    public double getMax() {
        return slider.maxProperty().get();
    }

    public DoubleProperty maxProperty() {
        return slider.maxProperty();
    }

    public void setMax(double max) {
        slider.maxProperty().set(max);
    }

    public int getTick() {
        return slider.minorTickCountProperty().get();
    }

    public IntegerProperty tickProperty() {
        return slider.minorTickCountProperty();
    }

    public void setTick(int tick) {
        slider.minorTickCountProperty().set(tick);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }
}
