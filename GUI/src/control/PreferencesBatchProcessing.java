package control;

import core.Const;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import core.settings.Batch;

import java.awt.Color;
import java.util.Map;


public class PreferencesBatchProcessing extends AbstractControl {


    @FXML
    private ComboBox<String> cbColor;

    @FXML
    private CheckBox chkDetermineOrientationFields;

    @FXML
    private CheckBox chkPostProcessOrientationFields;

    @FXML
    private CheckBox chkSaveOrientationFieldImages;

    @FXML
    private CheckBox chkSmallChanges;

    @FXML
    private CheckBox chkFingerprint;

    @FXML
    private CheckBox chkExcursionImages;

    @FXML
    private void initialize() {
        if (cbColor != null) {
            Map<String, Color> colorMap = Const.makeColorMap();
            cbColor.getItems().addAll(colorMap.keySet());
            //cbColor.getSelectionModel().select("Dark Orange");
        }
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {

        if (chkDetermineOrientationFields != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.determineOrientationFields).bindBidirectional(chkDetermineOrientationFields.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().determineOrientationFieldsProperty().bindBidirectional(chkDetermineOrientationFields.selectedProperty());
        }
        if (chkPostProcessOrientationFields != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.postProcessOrientationFields).bindBidirectional(chkPostProcessOrientationFields.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().postProcessOrientationFieldsProperty().bindBidirectional(chkPostProcessOrientationFields.selectedProperty());
        }
        if (chkSaveOrientationFieldImages != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.saveOrientationFieldImages).bindBidirectional(chkSaveOrientationFieldImages.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().saveOrientationFieldImagesProperty().bindBidirectional(chkSaveOrientationFieldImages.selectedProperty());
        }
        if (chkSmallChanges != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.restrictAreaChanges).bindBidirectional(chkSmallChanges.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().smallChangesProperty().bindBidirectional(chkSmallChanges.selectedProperty());
        }
        if (chkFingerprint != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.calculateFingerprints).bindBidirectional(chkFingerprint.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().calculateFingerprintProperty().bindBidirectional(chkFingerprint.selectedProperty());
        }
        if (chkExcursionImages != null) {
            getMainController().getModel().getProjectData().getSettings().getPropertyAsBoolean(Batch.saveExcursionImages).bindBidirectional(chkExcursionImages.selectedProperty());
            //getMainController().getModel().getPreferencesBatchProcessing().saveExcursionImagesProperty().bindBidirectional(chkExcursionImages.selectedProperty());
        }
        if (cbColor != null) {
            cbColor.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    getMainController().getModel().getProjectData().getSettings().setProperty(Batch.color, Const.makeColorMap().get(newValue).getRGB());
                    //getMainController().getModel().getPreferencesBatchProcessing().setColor(Const.makeColorMap().get(newValue));
                }
            });
            cbColor.getSelectionModel().select("Dark Orange");
        }


    }


}
