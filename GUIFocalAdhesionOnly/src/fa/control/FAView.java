package fa.control;

import control.AbstractControl;
import core.filaments.AbstractFilament;
import fa.model.FATableDataModel;
import fa.model.FAVerifierTableModel;
import focaladhesion.FocalAdhesion;
import focaladhesion.FocalAdhesionContainer;
import focaladhesion.Verifier;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polyline;

import core.image.Entry;
import core.image.ImageWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FAView extends AbstractControl {
    @FXML
    private TableView<FATableDataModel> tableFocalAdhesion;

    @FXML
    private TableColumn<FATableDataModel, Integer> columnNumber;
    @FXML
    private TableColumn<FATableDataModel, Boolean> columnKeep;
    @FXML
    private TableColumn<FATableDataModel, Double> columnCenterX;
    @FXML
    private TableColumn<FATableDataModel, Double> columnCenterY;
    @FXML
    private TableColumn<FATableDataModel, Double> columnLengthMainAxis;
    @FXML
    private TableColumn<FATableDataModel, Double> columnLengthSideAxis;
    @FXML
    private TableColumn<FATableDataModel, Double> columnAngle;
    @FXML
    private TableColumn<FATableDataModel, Double> columnArea;
    @FXML
    private TableColumn<FATableDataModel, Double> columnAreaEllipse;

    @FXML
    private TableView<FAVerifierTableModel> tableVerifier;
    @FXML
    private TableColumn<FAVerifierTableModel, Integer> columnFilamentNumber;
    @FXML
    private TableColumn<FAVerifierTableModel, String> columnVerifier;


    @FXML
    private void initialize() {
        //init column size for tableVerifier
        columnVerifier.prefWidthProperty().bind(tableVerifier.widthProperty().divide(2));
        columnFilamentNumber.prefWidthProperty().bind(tableVerifier.widthProperty().divide(2));
        //end init column size

        //init column size for tableFA
        columnNumber.prefWidthProperty().bind(tableVerifier.widthProperty().divide(18));
        columnKeep.prefWidthProperty().bind(tableVerifier.widthProperty().divide(18));
        columnAngle.prefWidthProperty().bind(tableVerifier.widthProperty().divide(9));
        columnArea.prefWidthProperty().bind(tableVerifier.widthProperty().divide(10));
        columnCenterX.prefWidthProperty().bind(tableVerifier.widthProperty().divide(10));
        columnCenterY.prefWidthProperty().bind(tableVerifier.widthProperty().divide(10));
        columnLengthMainAxis.prefWidthProperty().bind(tableVerifier.widthProperty().divide(6));
        columnLengthSideAxis.prefWidthProperty().bind(tableVerifier.widthProperty().divide(6));
        columnAreaEllipse.prefWidthProperty().bind(tableVerifier.widthProperty().divide(7));
        //end init column size
        tableFocalAdhesion.setEditable(true);
        columnKeep.setCellValueFactory(cellData -> cellData.getValue().keepProperty());
        columnKeep.setCellFactory(CheckBoxTableCell.forTableColumn(columnKeep));
        columnAngle.setCellValueFactory((cellData) -> cellData.getValue().angleProperty().asObject());
        columnArea.setCellValueFactory((cellData) -> cellData.getValue().areaProperty().asObject());
        columnCenterX.setCellValueFactory((cellData) -> cellData.getValue().centerXProperty().asObject());
        columnCenterY.setCellValueFactory((cellData) -> cellData.getValue().centerYProperty().asObject());
        columnLengthMainAxis.setCellValueFactory((cellData) -> cellData.getValue().lengthMainProperty().asObject());
        columnLengthSideAxis.setCellValueFactory((cellData) -> cellData.getValue().lengthSideProperty().asObject());
        columnNumber.setCellValueFactory((cellData) -> cellData.getValue().idProperty().asObject());
        columnAreaEllipse.setCellValueFactory((cellData) -> cellData.getValue().areaEllipseProperty().asObject());

        tableVerifier.setEditable(true);
        columnFilamentNumber.setCellValueFactory(cellData -> cellData.getValue().filamentIdProperty().asObject());

        columnVerifier.setCellValueFactory(new PropertyValueFactory<>("verifier"));
        columnVerifier.setCellFactory(TextFieldTableCell.forTableColumn());

        columnVerifier.setOnEditCommit(
                (TableColumn.CellEditEvent<FAVerifierTableModel, String> t) -> {
                    //should be in the format "number separated by comma"  the comma should only follow if at least one number is inside
                    if (t.getNewValue() == null || t.getNewValue().isEmpty()) {
                        //remove verification
                        t.getRowValue().getFilament().setVerified(false);
                        t.getRowValue().getFilament().setVerifier(null);
                    } else {
                        String[] items = t.getNewValue().split(",");
                        //every item should contain a number
                        try {
                            t.getRowValue().getFilament().setVerifier(Verifier.focalAdhesion(Arrays.stream(items).map(Integer::parseInt).toArray(Integer[]::new)));
                            t.getRowValue().getFilament().setVerified(true);
                        } catch (NumberFormatException ex) {
                            t.getRowValue().getFilament().setVerifier(null);
                            t.getRowValue().getFilament().setVerified(false);
                            t.getRowValue().setVerifier(t.getOldValue());//revert value to old one
                        }
                    }


                });

        tableFocalAdhesion.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                //change selection back to not selected (for example color)
                oldValue.getData().selectedProperty().setValue(false);
            }
            if (newValue != null) {
                //change new selected item to selected mode
                newValue.getData().selectedProperty().setValue(true);
                System.out.println("FAView::initialize() --- selection=" + newValue);
            }
        });

        tableVerifier.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getFilament().selectedProperty().set(false);
                oldValue.getListFocalAdhesion().forEach(focalAdhesion -> focalAdhesion.selectedProperty().set(false));
            }
            if (newValue != null) {
                newValue.getFilament().selectedProperty().set(true);
                newValue.getListFocalAdhesion().forEach(focalAdhesion -> focalAdhesion.selectedProperty().set(true));
            }
        });

    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {

    }

    public void initTableFA(List<Ellipse> listFocalAdhesion) {
        System.out.println("FAView::initTableFA() --- called");
        tableFocalAdhesion.setItems(FXCollections.emptyObservableList());
        if (getMainController() == null ||
                getMainController().getModel() == null ||
                getMainController().getModel().getStackModel() == null ||
                getMainController().getModel().getStackModel().getStackOrig() == null) return;
        initTableFA(getData().getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1), listFocalAdhesion);
    }

    public void initTableVerifier(List<Ellipse> listFocalAdhesion, List<Polyline> listFilament) {
        System.out.println("FAView::initTableVerifier() --- called");
        tableVerifier.setItems(FXCollections.emptyObservableList());
        if (getMainController() == null ||
                getMainController().getModel() == null ||
                getMainController().getModel().getStackModel() == null ||
                getMainController().getModel().getStackModel().getStackOrig() == null) return;
        initTableVerifier(getData().getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1), listFocalAdhesion, listFilament);

    }

    private ImageWrapper getData() {
        return getMainController().getModel().getStackModel().getStackOrig();
    }

    private void initTableFA(Entry entry, List<Ellipse> listFocalAdhesion) {
        List<FATableDataModel> data = new ArrayList<>();
        if (entry.getCorrelationData() == null) return;
        if (!(entry.getCorrelationData() instanceof FocalAdhesionContainer)) return;
        /*((FocalAdhesionContainer) entry.getCorrelationData()).getFilteredData(entry.getDataFilament(),
                getMainController().getModel().getProjectData().getSettings()
        ).forEach(fA -> {
            Ellipse faEllipse = listFocalAdhesion.stream().filter(ellipse -> ellipse.getUserData() instanceof FocalAdhesion && ellipse.getUserData().equals(fA)).findAny().orElse(null);
            data.add(new FATableDataModel(fA, faEllipse));
        });*/
        data = listFocalAdhesion.stream().filter(ellipse -> ellipse.getUserData() != null && ellipse.getUserData() instanceof FocalAdhesion).map(ellipse -> new FATableDataModel((FocalAdhesion) ellipse.getUserData(), ellipse)).collect(Collectors.toList());
        tableFocalAdhesion.setItems(FXCollections.observableList(data));
    }

    private void initTableVerifier(Entry entry, List<Ellipse> listFocalAdhesion, List<Polyline> listFilament) {
        List<FAVerifierTableModel> verifierData = new ArrayList<>();
        entry.getDataFilament().getFilaments().stream().filter(AbstractFilament::isVerified).forEach(f -> {
            Polyline filament = listFilament.stream().filter(polyline -> polyline.getUserData().equals(f)).findAny().orElse(null);

            List<Ellipse> ellipses = listFocalAdhesion.stream().filter(ellipse -> ellipse.getUserData() instanceof FocalAdhesion &&
                    f.getVerifier() != null && f.getVerifier().getId().contains(((FocalAdhesion) ellipse.getUserData()).getNumber())).collect(Collectors.toList());
            verifierData.add(new FAVerifierTableModel(f, filament, ellipses));
        });
        tableVerifier.setItems(FXCollections.observableList(verifierData));
    }

    @FXML
    private void onRemoveFocalAdhesion(ActionEvent actionEvent) {
        List<FATableDataModel> toRemove = tableFocalAdhesion.getItems().stream().
                filter(i -> !i.isKeep()).collect(Collectors.toList());
        ImageWrapper wrapper = getData();
        Entry entry = wrapper.getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1);
        if (entry.getCorrelationData() != null && entry.getCorrelationData() instanceof FocalAdhesionContainer) {
            tableFocalAdhesion.getItems().removeAll(toRemove);
            ((FocalAdhesionContainer) entry.getCorrelationData()).getData().
                    removeAll(toRemove.stream().map(FATableDataModel::getData).collect(Collectors.toList()));

        }
    }
}
