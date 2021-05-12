package control;

import core.Const;
import core.calculation.OFCalculator;
import core.filaments.AbstractFilament;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import interfaces.IFilamentUpdater;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Polyline;
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import model.FilamentTableDataModel;
import model.FilamentsModel;
import model.ImageDetailStackModel;
import enums.ImageTypes;
import evaluation.Evaluator;
import core.image.BinaryImage;
import core.image.Entry;
import core.image.ImageWrapper;
import core.settings.Trace;
import core.tracers.CurveTracer;
import core.tracers.LineSensor;
import util.ImageFactory;
import util.IOUtils;
import util.ImageExporter;
import util.MixedUtils;
import util.PathScanner;
import util.io.FilamentCsvExport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Filaments extends AbstractControl {

    public static final javafx.scene.paint.Color defaultFilament = javafx.scene.paint.Color.GREEN;
    public static final javafx.scene.paint.Color defaultKeepFalse = javafx.scene.paint.Color.RED;
    public static final javafx.scene.paint.Color defaultVerified = javafx.scene.paint.Color.PURPLE;
    public static final javafx.scene.paint.Color defaultSelected = javafx.scene.paint.Color.YELLOW;


    public static final ObservableList<String> exportTypes = FXCollections.observableArrayList(".png", ".tif", ".ome.tif", ".jpg", ".gif");

    @FXML
    private TableView<FilamentTableDataModel> tableFilaments;

    @FXML
    private ComboBox<String> cbPictureType;
    @FXML
    private ComboBox<String> cbColors;

    @FXML
    private ComboBox<String> cbOutputType;

    @FXML
    private CheckBox chkIncludeFilaments;

    private FilamentsModel model;
    private AbstractControl parent;

    @FXML
    private TableColumn<FilamentTableDataModel, Boolean> columnKeep;
    @FXML
    private TableColumn columnCenterX;
    @FXML
    private TableColumn columnCenterY;
    @FXML
    private TableColumn columnLength;
    @FXML
    private TableColumn columnAngle;
    @FXML
    private TableColumn columnWidth;
    @FXML
    private TableColumn<FilamentTableDataModel, Integer> columnNumber;

    @FXML
    private CheckBox chkIncludeAreaOutline;

    @FXML
    private CheckBox chkExportAll;

    @FXML
    private ComboBox<Color> cbAreaOutlineColor;


    public Filaments() {
        model = new FilamentsModel();
    }


    @FXML
    private void initialize() {
        cbOutputType.setItems(exportTypes);
        cbOutputType.getSelectionModel().selectFirst();

        cbPictureType.getItems().setAll(Arrays.stream(ImageTypes.values()).map(Enum::name).filter(n -> !n.equals(ImageTypes.DIFF.name()) &&
                !n.equals(ImageTypes.HOUGH.name()) && !n.equals(ImageTypes.ELOG_COLOR.name()) &&
                !n.equals(ImageTypes.ELOG_GREY.name())).collect(Collectors.toList()));


        cbColors.getItems().setAll(Const.makeColorMap().keySet());

        cbPictureType.valueProperty().bindBidirectional(model.pictureTypeProperty());
        cbColors.valueProperty().bindBidirectional(model.colorsProperty());

        chkIncludeFilaments.selectedProperty().bindBidirectional(model.includeFilamentsProperty());

        cbPictureType.getSelectionModel().select(ImageTypes.ORIGINAL.name());
        cbColors.getSelectionModel().select("Dark Orange");


        tableFilaments.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //rgb(135,206,250)
        Color color = new Color(135, 206, 250);
        cbAreaOutlineColor.getItems().add(Color.white);
        cbAreaOutlineColor.getItems().add(color);
        model.colorAreaOutlineProperty().bind(cbAreaOutlineColor.valueProperty());
        cbAreaOutlineColor.getSelectionModel().select(color);

        model.includeAreaOutlineProperty().bind(chkIncludeAreaOutline.selectedProperty());


        tableFilaments.setEditable(true);
        columnKeep.setCellValueFactory(cellData -> cellData.getValue().keepProperty());
        columnKeep.setCellFactory(CheckBoxTableCell.forTableColumn(columnKeep));
        columnNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        columnCenterX.setCellValueFactory(new PropertyValueFactory<FilamentTableDataModel, Double>("centerX"));
        columnCenterY.setCellValueFactory(new PropertyValueFactory<FilamentTableDataModel, Double>("centerY"));
        columnAngle.setCellValueFactory(new PropertyValueFactory<FilamentTableDataModel, Double>("angle"));
        columnWidth.setCellValueFactory(new PropertyValueFactory<FilamentTableDataModel, Double>("width"));
        columnLength.setCellValueFactory(new PropertyValueFactory<FilamentTableDataModel, Double>("length"));
        tableFilaments.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.getFilament().selectedProperty().setValue(false);
            }
            if (newValue != null) {
                newValue.getFilament().selectedProperty().setValue(true);
            }
        });


    }

    @FXML
    private void onDiscardKeepAll(ActionEvent event) {
        getMainController().getModel().getStackModel().getFilaments().keepDiscardAllFilaments();
        if (getParent() instanceof IFilamentUpdater) {
            ((IFilamentUpdater) getParent()).updateFilamentImage();
        }
    }

    @FXML
    private void onRemoveDiscarded(ActionEvent event) {
        getMainController().getModel().getStackModel().getFilaments().cleanTracedFilaments(false);
        if (getParent() instanceof IFilamentUpdater) {
            ((IFilamentUpdater) getParent()).initFilamentsPolyline(getMainController().getModel().getStackModel().getStackFilaments().getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1));
        } else
            initTable(null);
    }


    private Pair<String, BufferedImage> getChosenItemType(ImageDetailStackModel stackModel, int imageNumber) {
        BufferedImage image = null;

        ImageTypes type = ImageTypes.valueOf(getModel().getPictureType());
        switch (type) {
            case ORIGINAL:
                image = stackModel.getStackOrig().getImage(imageNumber, (getModel().isIncludeFilaments()) ? Const.makeColorMap().get(getModel().getColors()) : null);
                break;
            case PREPROCESSED:
                image = stackModel.getStackPreprocessed().getImage(imageNumber, (getModel().isIncludeFilaments()) ? Const.makeColorMap().get(getModel().getColors()) : null);
                break;
            case BINARY:
                image = stackModel.getStackLineSensor().getImage(imageNumber, (getModel().isIncludeFilaments()) ? Const.makeColorMap().get(getModel().getColors()) : null);
                break;
            case ORIENTATIONMAP://entryList starts with 0, getImage starts with 1
                image = ImageFactory.getOrientationMap(stackModel.getStackFilaments().getEntryList().get(imageNumber - 1).getOrientationFieldContainer().getOrientationField());
                break;
            case FINGERPRINT:
                image = ImageFactory.getFingerprint(stackModel.getStackOrig().getEntryList().get(imageNumber - 1),
                        //add correct tracer class
                        getMainController().getModel().getProjectData().getSettings().getValueAsBoolean(Trace.curve) ? CurveTracer.class : LineSensor.class
                );
                break;
            case REDGREEN:
                Entry entry = stackModel.getStackOrig().getEntryList().get(imageNumber - 1);
                image = ImageFactory.makeTwoLevelRedGreenImage(entry.getProcessor(),
                        entry.getInteriorContainer().getInterior(),
                        entry.getShape().getSelectedArea().getBinaryImage()
                );
                if (getModel().isIncludeFilaments())
                    ImageExporter.addFilaments(image, entry.getDataFilament(), Const.makeColorMap().get(getModel().getColors()));
                break;
            case BINARYAREAONLY:
                image = ImageFactory.getAreaImage(stackModel.getStackOrig().getEntryList().get(imageNumber - 1));
                break;

            default:
                getMainController().addDebugMessage("Image-Type is currently not supported");
                break;
        }

        if (image != null) {
            if (getModel().isIncludeAreaOutline()) {
                image = ImageExporter.addArea(image, stackModel.getStackOrig().getEntryList().get(imageNumber - 1).getShape());
            }
        }
        String tmp = new File(stackModel.getStackOrig().getEntryList().get(imageNumber - 1).getPath()).getName();
        for (String s : exportTypes) {
            tmp = tmp.replace(s, "");
        }
        return new Pair<>(tmp + "_" + type.name().toLowerCase(), image);
    }

    private ImagePlus getChosenItemStack(ImageDetailStackModel stackModel) {
        //create a stack with the selected image type and return it
        ImageWrapper stack = null;
        Color color = (getModel().isIncludeFilaments()) ? Const.makeColorMap().get(getModel().getColors()) : null;

        ImageTypes type = ImageTypes.valueOf(getModel().getPictureType());
        switch (type) {
            case ORIGINAL:
                stack = stackModel.getStackOrig().clone();
                break;
            case PREPROCESSED:
                stack = stackModel.getStackPreprocessed().clone();
                break;
            case BINARY:
                stack = stackModel.getStackLineSensor().clone();
                break;
            case ORIENTATIONMAP://entryList starts with 0, getImage starts with 1
                //create a stack
                stack = ImageFactory.getOrientationMap(stackModel.getStackFilaments());
                break;
            case FINGERPRINT:
                //create a stack
                stack = ImageFactory.getFingerprint(stackModel.getStackOrig(),
                        getMainController().getModel().getProjectData().getSettings()
                                .getValueAsBoolean(Trace.curve) ? CurveTracer.class : LineSensor.class);
                break;
            case REDGREEN:
                stack = ImageFactory.makeTwoLevelRedGreenImage(stackModel.getStackOrig());
                break;
            case BINARYAREAONLY:
                stack = ImageFactory.getAreaImage(stackModel.getStackOrig());
                break;
            default:
                getMainController().addDebugMessage("Image-Type is currently not supported");
                break;
        }

        if (stack != null) {
            if (color != null) {
                //include filaments in stack
                ImageExporter.addFilaments(stack, color, color, true);
            }

            if (getModel().isIncludeAreaOutline()) {
                //include area in stack
                ImageExporter.addArea(stack);
            }
            stack.getEntryList().clear();//clear entry list, it is not needed
        }


        if (stack != null) return stack.getImage();

        return null;
    }


    private void onSaveImageImageDetailStack(ActionEvent event, ImageDetailStackModel stackModel) {

        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the image should be stored");
        File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());


        if (".ome.tif".equals(cbOutputType.getSelectionModel().getSelectedItem())) {
            ImagePlus tmp = getChosenItemStack(stackModel);
            if (tmp != null) {
                FileSaver fileSaver = new FileSaver(tmp);
                String name = tmp.getTitle().replace("DUP_", "");//imageJ adds this string to title
                for (String ext : PathScanner.supportedImageExtensions) {
                    name = name.replace(ext, "");
                }
                File tmpFile = new File(imageFile.getAbsolutePath() + File.separator + name + "_" + getModel().getPictureType().toLowerCase() + "_Stack" + ".ome.tif");
                boolean result = fileSaver.saveAsTiffStack(tmpFile.getAbsolutePath());
                if (result) getMainController().addDebugMessage("File stored");
                else getMainController().addDebugMessage("File not stored!");
                tmp.close();
            }
        } else {

            List<Integer> idxs;
            if (chkExportAll.isSelected()) {
                idxs = IntStream.range(1, stackModel.getSize() + 1).boxed().collect(Collectors.toList());
            } else {
                idxs = Collections.singletonList(stackModel.getCurrentImage());
            }
            getMainController().runAsync(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    AtomicBoolean result = new AtomicBoolean(true);
                    idxs.forEach(idx -> {
                        try {
                            Pair<String, BufferedImage> image = getChosenItemType(stackModel, idx);
                            if (image.getValue() != null) {
                                if (cbOutputType.getSelectionModel().getSelectedItem().contains("tif")) {
                                    //its type tif, ImageIO can't do it?
                                    //tiff just won't work with ImageIO -> use a different method to store it
                                    //tried with tif, tiff, TIFF
                                    //this works now
                                    ImagePlus tmp = new ImagePlus(image.getKey(), new ColorProcessor(image.getValue()));
                                    FileSaver fileSaver = new FileSaver(tmp);
                                    fileSaver.saveAsTiff(imageFile.getAbsolutePath() + File.separator + image.getKey() +
                                            cbOutputType.getSelectionModel().getSelectedItem());
                                } else {
                                    result.set(result.get() && ImageIO.write(image.getValue(),
                                            cbOutputType.getSelectionModel().getSelectedItem().replace(".", "").toUpperCase(),
                                            new File(imageFile.getAbsolutePath() + File.separator + image.getKey() +
                                                    cbOutputType.getSelectionModel().getSelectedItem())
                                    ));
                                }


                            }
                        } catch (IOException e) {
                            getMainController().addDebugMessage(e);
                            e.printStackTrace();
                        }
                        updateProgress((((double) idx + 1) / idxs.size()), 100);
                    });
                    if (result.get()) getMainController().addDebugMessage("File stored");
                    else getMainController().addDebugMessage("File not stored!");
                    updateProgress(100, 100);
                    return null;
                }
            });

        }

    }


    @FXML
    private void onSaveImage(ActionEvent event) {
        onSaveImageImageDetailStack(event, getMainController().getModel().getStackModel());
    }


    @FXML
    private void onExportFilaments(ActionEvent event) {

        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the image should be stored");
        File imageFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() {
                getMainController().addDebugMessage("Start Exporting CSV");
                updateProgress(0, 100);
                List<Entry> entries = new ArrayList<>();

                if (chkExportAll.isSelected()) {
                    entries = getMainController().getModel().getStackModel().getStackFilaments().getEntryList();
                } else {
                    entries.add(getMainController().getModel().getStackModel().
                            getStackFilaments().getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1));
                }
                entries.forEach(e -> {
                    try {
                        FilamentCsvExport.exportFilamentCSV(imageFile, false, e);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        getMainController().addDebugMessage(e1);
                    }
                });
                updateProgress(100, 100);
                getMainController().addDebugMessage("Finished Exporting CSV");
                return null;
            }
        });
    }


    private void onExportOrientationsImageDetail(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("choose were the Orientation-File/s should be stored");
        File orientationFile = fileChooser.showDialog(((Button) event.getSource()).getScene().getWindow());

        getMainController().runAsync(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);

                //do orientationField post processing
                OFCalculator.orientationFieldPostProcessing(getMainController().getModel().getStackModel().getStackFilaments().getEntryList());
                updateProgress(50, 100);
                MixedUtils.getStream(getMainController().getModel().getStackModel().getStackFilaments().getEntryList(), false).forEach(e -> {
                    try {
                        IOUtils.exportOF(
                                (int) Math.ceil(1d / getMainController().getModel().getStackModel().getStackFilaments().getCurrentScale()),
                                e,
                                IOUtils.getOutFileFromImageFile(new File(e.getPath()), orientationFile, ".csv", null)
                        );
                        //if an image export is wanted here, would be nicer in color though
                        //ImageExporter.exportImage(e.getOrientationFieldContainer().getOrientationField(),
                        // IOUtils.getOutFileFromImageFile(new File(e.getPath()), orientationFile,
                        // ".png", "_orientation").getAbsolutePath(),BufferedImage.TYPE_BYTE_GRAY);


                    } catch (IOException ex) {
                        ex.printStackTrace();
                        getMainController().addDebugMessage(ex);
                    }

                });
                updateProgress(100, 100);
                getMainController().addDebugMessage("Export Orientations CSV Finished");
                return null;
            }
        });
    }


    @FXML
    private void onExportOrientations(ActionEvent event) {
        onExportOrientationsImageDetail(event);
    }

    public AbstractControl getParent() {
        return parent;
    }

    public void setParent(AbstractControl parent) {
        this.parent = parent;
    }


    public FilamentsModel getModel() {
        return model;
    }

    public void setModel(FilamentsModel model) {
        this.model = model;
    }


    private void initTable(Entry entry, List<Polyline> polylines) {
        tableFilaments.setItems(FXCollections.emptyObservableList());
        List<FilamentTableDataModel> data = new ArrayList<>();
        data = polylines.stream().filter(p -> p.getUserData() != null && p.getUserData() instanceof AbstractFilament).map(p -> new FilamentTableDataModel((AbstractFilament) p.getUserData(), p)).collect(Collectors.toList());
        /*entry.getDataFilament().getFilaments().forEach(filament -> {
            Polyline line = polylines.stream().filter(p -> p.getUserData() != null && p.getUserData().equals(filament)).findAny().orElse(null);
            data.add(new FilamentTableDataModel(filament, line));
        });*/
        tableFilaments.setItems(FXCollections.observableList(data));
    }


    public void initTable(List<Polyline> filaments) {
        initTable(getMainController().getModel().getStackModel().getStackFilaments().getEntryList().get(getMainController().getModel().getStackModel().getCurrentImage() - 1), filaments);
    }


    @Override
    protected void afterSetMainController(AbstractControl parent) {
        setParent(parent);
        getModel().colorsProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getMainController().getModel().getStackModel().setFilamentColor(Const.makeColorMap().get(newValue));
            }
        });
        cbColors.getSelectionModel().select("White");
        cbColors.getSelectionModel().select("Dark Orange");

    }

    @FXML
    private void onEvaluateFilaments(ActionEvent actionEvent) {
        //can get stuck due to wrong background color

        getMainController().<Evaluation>openPopUp("/view/Evaluation.fxml", "Evaluate Filaments", (control) -> {
            ImageWrapper evalMask = ImageExporter.getFilamentWrapperAsMask(getMainController().getModel().getStackModel().getStackFilaments());
            control.setToEval(evalMask,
                    (i) -> new BinaryImage(evalMask.getEntryList().get(i).getProcessor().convertToByteProcessor())
                    , () -> Evaluator.ShapeType.lineLike);
        });
    }

    @FXML
    private void onOpenSingleFilamentTracking(ActionEvent actionEvent) {
        getMainController().openPopUp("/view/PopUpSingleFilamentTracking.fxml",
                "Single Filament Tracking", (control) -> {


                });

    }
}
