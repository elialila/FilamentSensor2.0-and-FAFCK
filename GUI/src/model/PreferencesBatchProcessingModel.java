package model;

import core.Const;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.awt.*;

public class PreferencesBatchProcessingModel {


    private ObjectProperty<Color> color;


    private BooleanProperty determineOrientationFields;
    private BooleanProperty postProcessOrientationFields;
    private BooleanProperty saveOrientationFieldImages;
    private BooleanProperty smallChanges;//delta area
    private BooleanProperty calculateFingerprint;
    private BooleanProperty saveExcursionImages;

    public PreferencesBatchProcessingModel() {
        determineOrientationFields = new SimpleBooleanProperty();
        postProcessOrientationFields = new SimpleBooleanProperty();
        saveOrientationFieldImages = new SimpleBooleanProperty();
        smallChanges = new SimpleBooleanProperty();
        calculateFingerprint = new SimpleBooleanProperty();
        saveExcursionImages = new SimpleBooleanProperty();
        color = new SimpleObjectProperty<>();
        setColor(Const.makeColorMap().get("Dark Orange"));
        setDetermineOrientationFields(true);
        setPostProcessOrientationFields(true);


    }

    public Color getColor() {
        return color.get();
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public boolean isDetermineOrientationFields() {
        return determineOrientationFields.get();
    }

    public BooleanProperty determineOrientationFieldsProperty() {
        return determineOrientationFields;
    }

    public void setDetermineOrientationFields(boolean determineOrientationFields) {
        this.determineOrientationFields.set(determineOrientationFields);
    }

    public boolean isPostProcessOrientationFields() {
        return postProcessOrientationFields.get();
    }

    public BooleanProperty postProcessOrientationFieldsProperty() {
        return postProcessOrientationFields;
    }

    public void setPostProcessOrientationFields(boolean postProcessOrientationFields) {
        this.postProcessOrientationFields.set(postProcessOrientationFields);
    }

    public boolean isSaveOrientationFieldImages() {
        return saveOrientationFieldImages.get();
    }

    public BooleanProperty saveOrientationFieldImagesProperty() {
        return saveOrientationFieldImages;
    }

    public void setSaveOrientationFieldImages(boolean saveOrientationFieldImages) {
        this.saveOrientationFieldImages.set(saveOrientationFieldImages);
    }

    public boolean isSmallChanges() {
        return smallChanges.get();
    }

    public BooleanProperty smallChangesProperty() {
        return smallChanges;
    }

    public void setSmallChanges(boolean smallChanges) {
        this.smallChanges.set(smallChanges);
    }

    public boolean isCalculateFingerprint() {
        return calculateFingerprint.get();
    }

    public BooleanProperty calculateFingerprintProperty() {
        return calculateFingerprint;
    }

    public void setCalculateFingerprint(boolean calculateFingerprint) {
        this.calculateFingerprint.set(calculateFingerprint);
    }

    public boolean isSaveExcursionImages() {
        return saveExcursionImages.get();
    }

    public BooleanProperty saveExcursionImagesProperty() {
        return saveExcursionImages;
    }

    public void setSaveExcursionImages(boolean saveExcursionImages) {
        this.saveExcursionImages.set(saveExcursionImages);
    }


    public boolean[] getFlags() {
        return new boolean[]{isDetermineOrientationFields(), isPostProcessOrientationFields(),
                isSaveOrientationFieldImages(), isSmallChanges(),
                isCalculateFingerprint(), isSaveExcursionImages()
        };
    }


}
