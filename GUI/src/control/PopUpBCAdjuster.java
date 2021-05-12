package control;

import fx.custom.BCAdjusterUI;
import fx.custom.StackImageView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import model.ImageDetailStackModel;
import filters.ContrastAdjuster;
import core.image.ImageWrapper;

import java.util.function.Consumer;

public class PopUpBCAdjuster extends AbstractControl {

    @FXML
    private BCAdjusterUI bcAdjuster;//ui element

    @FXML
    private StackImageView svImage;

    private ContrastAdjuster contrastAdjuster;//filament-sensor contrast/brightness adjuster

    private ImageWrapper wrapperCopy;

    private DecoratedChangeListener<? super Number> minListener;
    private DecoratedChangeListener<? super Number> maxListener;
    private DecoratedChangeListener<? super Number> bListener;
    private DecoratedChangeListener<? super Number> cListener;


    @FXML
    private void initialize() {
        svImage.currentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && wrapperCopy != null) {
                wrapperCopy.notifyListeners();
            }
        });

        bcAdjuster.setOnApply((t) -> {
            if (contrastAdjuster != null) {
                contrastAdjuster.apply(svImage.getCurrent() - 1, bcAdjuster.isWholeStack());
                wrapperCopy.updateProcessors();
                wrapperCopy.notifyListeners();
                //set stack in model
                wrapperCopy.cloneImage(getMainController().getModel().getStackModel().getStackOrig());
                getMainController().getModel().getStackModel().getStackOrig().notifyListeners();
                //on this part it would make sense only to clone the image and replace processors
                //all images depending on original should be updated too
            }
        });
        bcAdjuster.setOnAuto((t) -> {
            if (contrastAdjuster != null) {
                contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.auto, (b) -> {
                    wrapperCopy.updateProcessors();
                    wrapperCopy.notifyListeners();
                });
            }
        });
        bcAdjuster.setOnReset((t) -> {
            if (contrastAdjuster != null) {
                contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.reset, null);
                wrapperCopy.updateProcessors();
                minListener.setBlock(true);
                maxListener.setBlock(true);
                cListener.setBlock(true);
                bListener.setBlock(true);
                bcAdjuster.minProperty().set(contrastAdjuster.getMin());
                bcAdjuster.maxProperty().set(contrastAdjuster.getMax());
                bcAdjuster.brightnessProperty().set(contrastAdjuster.getBrightness());
                bcAdjuster.contrastProperty().set(contrastAdjuster.getContrast());
                wrapperCopy.notifyListeners();

                minListener.setBlock(false);
                maxListener.setBlock(false);
                cListener.setBlock(false);
                bListener.setBlock(false);
                bcAdjuster.updateLine((int) contrastAdjuster.getMin(), (int) contrastAdjuster.getMax());
            }
        });


    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        ImageDetailStackModel stackModel = getMainController().getModel().getStackModel();
        Consumer<Void> task = (t) -> {
            wrapperCopy = stackModel.getStackOrig().clone();
            bcAdjuster.setImage(wrapperCopy.getImage());
            initBrightnessAdjuster();
            wrapperCopy.addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue) {
                    svImage.setImage(SwingFXUtils.toFXImage(wrapperCopy.getImage(svImage.getCurrent()), null));
                }
            });
            svImage.setMax(wrapperCopy.getSize());
            svImage.setCurrent(1);
            wrapperCopy.notifyListeners();
        };
        if (stackModel != null) {
            if (stackModel.getReady().isDone()) {
                task.accept(null);
            } else stackModel.getReady().thenAccept((b) -> Platform.runLater(() -> task.accept(null)));
        }

    }


    private void initBrightnessAdjuster() {
        contrastAdjuster = new ContrastAdjuster(wrapperCopy);
        Consumer<ContrastAdjuster.AdjusterAction> callback = (t) -> {
            synchronized (this) {
                minListener.setBlock(true);
                maxListener.setBlock(true);
                bListener.setBlock(true);
                cListener.setBlock(true);
                //System.out.println("PopUpBCAdjuster::initBrightnessAdjuster::callback()  ---min,max,b,c=" + contrastAdjuster.getMin() + "," + contrastAdjuster.getMax() + "," + contrastAdjuster.getBrightness() + "," + contrastAdjuster.getContrast());

                if (!t.equals(ContrastAdjuster.AdjusterAction.min)) {
                    bcAdjuster.minProperty().set(contrastAdjuster.getMin());
                }
                if (!t.equals(ContrastAdjuster.AdjusterAction.max))
                    bcAdjuster.maxProperty().set(contrastAdjuster.getMax());
                if (!t.equals(ContrastAdjuster.AdjusterAction.brightness))
                    bcAdjuster.brightnessProperty().set(contrastAdjuster.getBrightness());
                if (!t.equals(ContrastAdjuster.AdjusterAction.contrast))
                    bcAdjuster.contrastProperty().set(contrastAdjuster.getContrast());

                bcAdjuster.updateLine((int) contrastAdjuster.getMin(), (int) contrastAdjuster.getMax());

                wrapperCopy.updateProcessors();//this updates the image --> processors in the image get changed and this updates the entrylist
                //so the new processors are shown
                wrapperCopy.notifyListeners();//notify the listener (the one showing the new image on stack-view)
            }
        };
        //onChange call update Method's
        minListener = new DecoratedChangeListener<>((observable, oldValue, newValue) -> {
            if (newValue != null && contrastAdjuster != null) {
                synchronized (this) {
                    minListener.setBlock(true);
                    maxListener.setBlock(true);
                    bListener.setBlock(true);
                    cListener.setBlock(true);
                    contrastAdjuster.minSliderProperty().set(newValue.intValue());

                    contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.min, callback);

                }
            }
        });

        maxListener = new DecoratedChangeListener<>((observable, oldValue, newValue) -> {
            if (newValue != null && contrastAdjuster != null) {
                synchronized (this) {
                    minListener.setBlock(true);
                    maxListener.setBlock(true);
                    bListener.setBlock(true);
                    cListener.setBlock(true);
                    contrastAdjuster.maxSliderProperty().set(newValue.intValue());
                    contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.max, callback);

                }
            }
        });

        bListener = new DecoratedChangeListener<>((observable, oldValue, newValue) -> {
            if (newValue != null && contrastAdjuster != null) {
                synchronized (this) {
                    minListener.setBlock(true);
                    maxListener.setBlock(true);
                    bListener.setBlock(true);
                    cListener.setBlock(true);
                    contrastAdjuster.brightnessSliderProperty().set(newValue.intValue());
                    contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.brightness, callback);


                }
            }
        });

        cListener = new DecoratedChangeListener<>((observable, oldValue, newValue) -> {
            if (newValue != null && contrastAdjuster != null) {
                synchronized (this) {
                    minListener.setBlock(true);
                    maxListener.setBlock(true);
                    bListener.setBlock(true);
                    cListener.setBlock(true);
                    contrastAdjuster.contrastSliderProperty().set(newValue.intValue());
                    contrastAdjuster.doUpdate(svImage.getCurrent() - 1, bcAdjuster.isWholeStack(), ContrastAdjuster.AdjusterAction.contrast, callback);

                }
            }
        });

        bcAdjuster.minProperty().addListener(minListener);
        bcAdjuster.maxProperty().addListener(maxListener);
        bcAdjuster.brightnessProperty().addListener(bListener);
        bcAdjuster.contrastProperty().addListener(cListener);

        EventHandler<? super MouseEvent> handler = event -> {
            Platform.runLater(() -> {
                minListener.setBlock(false);
                maxListener.setBlock(false);
                bListener.setBlock(false);
                cListener.setBlock(false);
            });
        };

        bcAdjuster.setMinOnMouseReleased(handler);
        bcAdjuster.setMaxOnMouseReleased(handler);
        bcAdjuster.setBrightnessOnMouseReleased(handler);
        bcAdjuster.setContrastOnMouseReleased(handler);


    }


    //taken from: https://stackoverflow.com/questions/12578957/javafx-how-to-disable-events-fired-not-from-the-user
    //this is used to stop event-spamming from not user-action started slider-value-changes
    private class DecoratedChangeListener<T> implements ChangeListener<T> {
        private boolean block;
        private ChangeListener<T> decoratedListener;

        public DecoratedChangeListener(ChangeListener<T> listener) {
            decoratedListener = listener;
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            if (!block) {
                decoratedListener.changed(observable, oldValue, newValue);
            }
        }

        public void setBlock(boolean block) {
            this.block = block;
        }
    }


}
