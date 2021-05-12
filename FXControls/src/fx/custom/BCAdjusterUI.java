package fx.custom;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.*;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class BCAdjusterUI extends VBox {

    @FXML
    private LineChart<Number, Number> bcHistogram;

    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    @FXML
    private Slider sMin;
    @FXML
    private Slider sMax;
    @FXML
    private Slider sBrightness;
    @FXML
    private Slider sContrast;
    @FXML
    private CheckBox chkWholeStack;


    //outsource the dependency to filamentSensor lib via consumers?
    private ObjectProperty<Consumer<LineChart<Number, Number>>> onApply;
    private ObjectProperty<Consumer<LineChart<Number, Number>>> onAuto;
    private ObjectProperty<Consumer<LineChart<Number, Number>>> onReset;


    public BCAdjusterUI() {
        super();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fx/custom/view/BCAdjusterUI.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
            onApply = new SimpleObjectProperty<>();
            onAuto = new SimpleObjectProperty<>();
            onReset = new SimpleObjectProperty<>();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void initialize() {
    }


    private ImageStatistics getImageStatistics(ImagePlus imp) {
        ImageStatistics stats = null;
        if (imp.getStackSize() <= 1 && (imp.getNChannels() == 4 || imp.getNChannels() == 2 || imp.getNChannels() == 1) && imp.getType() == ImagePlus.COLOR_RGB) {
            int w = imp.getWidth();
            int h = imp.getHeight();
            byte[] r = new byte[w * h];
            byte[] g = new byte[w * h];
            byte[] b = new byte[w * h];
            ((ColorProcessor) imp.getProcessor()).getRGB(r, g, b);
            byte[] pixels = null;
            if (imp.getNChannels() == 4)
                pixels = r;
            else if (imp.getNChannels() == 2)
                pixels = g;
            else if (imp.getNChannels() == 1)
                pixels = b;
            ImageProcessor ip = new ByteProcessor(w, h, pixels, null);
            stats = ImageStatistics.getStatistics(ip, 0, imp.getCalibration());
        } else {
            int range = imp.getType() == ImagePlus.GRAY16 ? ImagePlus.getDefault16bitRange() : 0;
            if (range != 0 && imp.getProcessor().getMax() == Math.pow(2, range) - 1 && !(imp.getCalibration().isSigned16Bit())) {
                ImagePlus imp2 = new ImagePlus("Temp", imp.getProcessor());
                stats = new StackStatistics(imp2, 256, 0, Math.pow(2, range));
            } else
                stats = imp.getStatistics();
        }
        return stats;
    }


    public void setImage(ImagePlus image) {

        double min = image.getDisplayRangeMin();
        double max = image.getDisplayRangeMax();
        double step = 1;
        setupSlider(sMin, min, max, step);
        setupSlider(sMax, min, max, step);
        setupSlider(sBrightness, min, max, step);
        setupSlider(sContrast, min, max, step);

        initHistogram(getImageStatistics(image));

        Calibration calibration = image.getCalibration();
        if (calibration.calibrated()) {
            min = calibration.getCValue(min);
            max = calibration.getCValue(max);
        }
        sMin.setValue(min);
        sMax.setValue(max);

        sBrightness.setValue(max / 2);
        sContrast.setValue(max / 2);
        updateLine((int) min, (int) max);
    }


    private <T, U> XYChart.Data<T, U> getDataWithInvisiblePoint(T point, U value) {
        XYChart.Data<T, U> data = new XYChart.Data<>(point, value);
        Rectangle rect = new Rectangle(0, 0);
        rect.setVisible(false);
        data.setNode(rect);
        return data;
    }


    public void updateLine(int min, int max) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Line");
        series.getData().add(getDataWithInvisiblePoint(min, 0));
        series.getData().add(getDataWithInvisiblePoint(max, 100));

        //series.getData().add(getDataWithInvisiblePoint((int)sMin.getValue(),0));
        //series.getData().add(getDataWithInvisiblePoint((int)sMax.getValue(),100));
        if (bcHistogram.getData().size() > 1) bcHistogram.getData().remove(bcHistogram.getData().size() - 1);
        bcHistogram.getData().add(series);
    }

    private void initSliders() {


    }


    private void initHistogram(ImageStatistics stats) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        series.setName("Histogram");
        xAxis.setTickLabelsVisible(false);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(255);
        xAxis.setTickUnit(50);
        yAxis.setAutoRanging(false);
        yAxis.setTickLabelsVisible(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);//(stats.maxCount);
        yAxis.setTickUnit(5);//((double) stats.maxCount / 15);

        long[] hist = stats.getHistogram();
        final long maxAmount = Arrays.stream(hist).max().orElse(0);

        System.out.println(Arrays.toString(hist));
        for (int i = 0; i < hist.length; i++) {
            double histVal = hist[i];
            //normalize in relation to highest count
            histVal *= 100;
            histVal /= maxAmount;
            series.getData().add(getDataWithInvisiblePoint(i, histVal));
        }
        bcHistogram.getData().clear();
        bcHistogram.getData().add(series);
    }


    private void setupSlider(Slider slider, double min, double max, double step) {
        slider.setMin(min);
        slider.setMax(max);
        slider.setMajorTickUnit(max / 5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount((int) step);

    }

    public boolean isWholeStack() {
        return chkWholeStack.isSelected();
    }

    public DoubleProperty minProperty() {
        return sMin.valueProperty();
    }

    public DoubleProperty maxProperty() {
        return sMax.valueProperty();
    }

    public DoubleProperty brightnessProperty() {
        return sBrightness.valueProperty();
    }

    public DoubleProperty contrastProperty() {
        return sContrast.valueProperty();
    }

    public void setOnApply(Consumer<LineChart<Number, Number>> onApply) {
        this.onApply.set(onApply);
    }

    public void setOnAuto(Consumer<LineChart<Number, Number>> onAuto) {
        this.onAuto.set(onAuto);
    }

    public void setOnReset(Consumer<LineChart<Number, Number>> onReset) {
        this.onReset.set(onReset);
    }

    public void setMinOnMouseReleased(EventHandler<? super MouseEvent> handler) {
        sMin.setOnMouseReleased(handler);
    }

    public void setMaxOnMouseReleased(EventHandler<? super MouseEvent> handler) {
        sMax.setOnMouseReleased(handler);
    }

    public void setBrightnessOnMouseReleased(EventHandler<? super MouseEvent> handler) {
        sBrightness.setOnMouseReleased(handler);
    }

    public void setContrastOnMouseReleased(EventHandler<? super MouseEvent> handler) {
        sContrast.setOnMouseReleased(handler);
    }


    @FXML
    private void onApply(ActionEvent event) {
        if (onApply != null && onApply.get() != null) {
            onApply.get().accept(bcHistogram);
        }
    }

    @FXML
    private void onAuto(ActionEvent event) {
        System.out.println("BrightnessAdjuster::onAuto() --- Called");//is called
        if (onAuto != null && onAuto.get() != null) {//--> not called
            System.out.println("BrightnessAdjuster::onAuto() --- Called2");//is called
            onAuto.get().accept(bcHistogram);
        }
    }

    @FXML
    private void onReset(ActionEvent event) {
        if (onReset != null && onReset.get() != null) {
            onReset.get().accept(bcHistogram);
        }
    }


}
