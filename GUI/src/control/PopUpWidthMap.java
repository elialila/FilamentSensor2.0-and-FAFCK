package control;

import core.Misc;

import fx.custom.SliderSpinner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import core.settings.Trace;
import core.tracers.CurveTracer;
import core.tracers.LineSensor;
import core.tracers.Tracer;
import util.ImageFactory;
import util.Pair;


public class PopUpWidthMap extends AbstractControl {

    @FXML
    private ImageView imageView;
    @FXML
    private SliderSpinner sTolerance;

    private boolean lock = false;

    @FXML
    private void initialize() {
        sTolerance.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                if (!lock) {
                    getMainController().getModel().getProjectData().getSettings().setProperty(Trace.tolerance, newValue.intValue());
                    lock = true;
                    Task<Void> task = new Task<Void>() {
                        @Override
                        protected Void call() {
                            calcWidthMap(newValue.doubleValue());
                            lock = false;
                            return null;
                        }
                    };
                    new Thread(task).start();
                }
            }
        });
    }

    public void initTolerance(double tolerance) {
        sTolerance.setValue(tolerance);
    }


    private void calcWidthMap(double tolerance) {
        Tracer tracer;//initialize tracer
        if (getMainController().getModel().getProjectData().getSettings().getValueAsBoolean(Trace.curve)) {
            tracer = new CurveTracer();
        } else {
            tracer = new LineSensor();
        }
        int max = tracer.calcWidthMap(getMainController().getModel().getStackModel().getStackLineSensor().
                getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1).getProcessor(), tolerance / 100.0);
        Pair<Misc.Int2D, Integer> pair = new Pair<>(new Misc.Int2D(tracer.getWidthMap()), max);
        Platform.runLater(() -> imageView.setImage(SwingFXUtils.toFXImage(ImageFactory.makeWidthMap(pair), null)));
    }

    @Override
    protected void afterSetMainController(AbstractControl parent) {

    }
}
