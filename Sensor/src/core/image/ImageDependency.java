package core.image;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @todo add some progress measure
 * <p>
 * Class describes the inter image dependency and enables dependency updates
 * (if B depends on A and A is changed B will change too)
 */
public class ImageDependency {
    private ImageWrapper dependent;// is dependent to the owner of the ImageDependency
    private BiConsumer<ImageWrapper, ImageWrapper> initProcedure;//the initProcedure should provide a way to init the ImageWrapper which is dependent to the owner of this DependencyObject
    //for example just copy the image
    private Function<ImageWrapper, ImageWrapper> initFunction;//returns the ImageWrapper used for initialisation
    private Consumer<ImageWrapper> transformProcedure;//this function includes the processing for the ImageWrapper "dependent"
    private boolean doProcessing;//if true: run processing on dependency changes, if false: do not run

    public ImageDependency() {
    }

    public ImageDependency(ImageWrapper dependent, BiConsumer<ImageWrapper, ImageWrapper> initProcedure, Function<ImageWrapper, ImageWrapper> initFunction, Consumer<ImageWrapper> transformProcedure, boolean doProcessing) {
        this.dependent = dependent;
        this.initProcedure = initProcedure;
        this.initFunction = initFunction;
        this.transformProcedure = transformProcedure;
        this.doProcessing = doProcessing;
    }

    /**
     * This method should test before each update chain dependency if the next object is the first object.
     * If so: no update should be done, otherwise a circular dependency would result which means an endless loop.
     *
     * @param ownerSourceOfUpdates the original source of the update chain
     */
    private boolean testForCircleDependency(ImageWrapper ownerSourceOfUpdates) {
        return (ownerSourceOfUpdates == dependent);
    }


    /**
     * Propagates changes to dependant ImageWrapper's (async method)
     * if listeners on ImageWrapper are used for JavaFx Ui elements do not forget to wrap with Platform.runLater();
     *
     * @param owner                the source of the changes
     * @param ownerSourceOfUpdates the original source of the update chain
     * @param chainUpdate          true: call update on dependent objects; false: do not call update
     * @return
     */
    public CompletableFuture<Void> updateDependency(ImageWrapper owner, ImageWrapper ownerSourceOfUpdates, boolean chainUpdate) {
        Objects.requireNonNull(owner, "ImageDependency::updateDependency() --- Owner-Object is null");
        Objects.requireNonNull(dependent, "ImageDependency::updateDependency() --- Dependent-Object is null");
        Objects.requireNonNull(initFunction, "ImageDependency::updateDependency() --- initFunction-Object is null");
        Objects.requireNonNull(initProcedure, "ImageDependency::updateDependency() --- initProcedure-Object is null");
        if (doProcessing)
            Objects.requireNonNull(transformProcedure, "ImageDependency::updateDependency() --- transformProcedure-Object is null");
        return CompletableFuture.runAsync(() -> {
            synchronized (this) {
                initProcedure.accept(initFunction.apply(owner), dependent);
                if (doProcessing) transformProcedure.accept(dependent);
            }
        }).thenAccept((v) -> dependent.notifyListeners()).thenCompose((v) -> {
            if (!testForCircleDependency(ownerSourceOfUpdates) && chainUpdate)
                return dependent.updateDependencies(ownerSourceOfUpdates, true);
            else return CompletableFuture.completedFuture(null);
        }).exceptionally((ex) -> {
            ex.printStackTrace();
            return null;
        });
        //only execute dependent.updateDependencies if chainUpdate==true; if it is also executed on false the parameter has no effect
        //and the whole chain is always processed
    }

}
