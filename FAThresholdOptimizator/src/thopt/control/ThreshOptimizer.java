package thopt.control;

import core.settings.Settings;
import filters.FilterManualThreshold;
import ij.Prefs;
import ij.process.AutoThresholder;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import core.FilterQueue;
import core.image.ImageWrapper;
import thopt.core.FAThresholdOptimizer;
import thopt.model.ResultModel;
import util.Annotations;
import util.ImageExporter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ThreshOptimizer {

    @FXML
    private TextField tManualCheckAgainst;
    @FXML
    private TableColumn<ResultModel, Double> colHitChkMan;
    @FXML
    private TableColumn<ResultModel, Double> colFPChkMan;
    @FXML
    private TableColumn<ResultModel, Double> colHitChk;
    @FXML
    private TableColumn<ResultModel, Double> colFPChk;
    @FXML
    private TableColumn<ResultModel, Integer> colCheckAgainst;
    @FXML
    private ComboBox<AutoThresholder.Method> cbMethods;
    @FXML
    private CheckBox chkNoNumberforOne;
    @FXML
    private TableColumn<ResultModel, Integer> colNearest;
    @FXML
    private TableColumn<ResultModel, AutoThresholder.Method> colMethod;
    @FXML
    private TableColumn<ResultModel, String> colFileName;
    @FXML
    private TableColumn<ResultModel, Integer> colThresh;
    @FXML
    private TableColumn<ResultModel, Double> colHitRate;
    @FXML
    private TableColumn<ResultModel, Double> colFPRate;
    @FXML
    private TableView<ResultModel> tableResults;
    @FXML
    private TextField tDirectorySample;
    @FXML
    private TextField tDirectoryTruth;
    @FXML
    private TextField tDirectoryTestSet;
    @FXML
    private TextField tNamingSample;
    @FXML
    private TextField tNamingTruth;

    @FXML
    private void initialize() {
        tNamingSample.setText("%imgNr%F(cell%cellNr%)?.tif");
        tNamingTruth.setText("%imgNr%_Swe_FAmask(%cellNr%)?.tif");


        cbMethods.setItems(FXCollections.observableList(Arrays.asList(AutoThresholder.Method.values())));
        cbMethods.getSelectionModel().select(AutoThresholder.Method.Otsu);

        tDirectoryTruth.setText("D:\\Dokumente\\Arbeit Uni Göttingen\\Daten\\TestThresholdOptmization\\marked masks");
        tDirectorySample.setText("D:\\Dokumente\\Arbeit Uni Göttingen\\Daten\\TestThresholdOptmization");
        tDirectoryTestSet.setText("D:\\Dokumente\\Arbeit Uni Göttingen\\Daten\\TestThresholdOptmization\\TestSet");

        colFileName.setCellValueFactory(d -> d.getValue().fileNameProperty());
        colThresh.setCellValueFactory(d -> d.getValue().threshProperty().asObject());
        colHitRate.setCellValueFactory(d -> d.getValue().hitRateProperty().asObject());
        colFPRate.setCellValueFactory(d -> d.getValue().fPRateProperty().asObject());

        colMethod.setCellValueFactory(d -> d.getValue().methodProperty());
        colNearest.setCellValueFactory(d -> d.getValue().autoThreshProperty().asObject());
        colCheckAgainst.setCellValueFactory(d -> d.getValue().checkAgainstProperty().asObject());

        colFPChk.setCellValueFactory(d -> d.getValue().fPChkProperty().asObject());
        colHitChk.setCellValueFactory(d -> d.getValue().hitChkProperty().asObject());

        colFPChkMan.setCellValueFactory(d -> d.getValue().fPChkManProperty().asObject());
        colHitChkMan.setCellValueFactory(d -> d.getValue().hitChkManProperty().asObject());


        colFileName.prefWidthProperty().bind(tableResults.widthProperty().divide(2));

        int cnt = 20;
        colThresh.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colHitRate.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colFPRate.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colFPChk.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colHitChk.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colMethod.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colNearest.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colCheckAgainst.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colHitChkMan.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));
        colFPChkMan.prefWidthProperty().bind(tableResults.widthProperty().divide(cnt));


    }

    /**
     * @param text               text in the headline of the chooser
     * @param tText              TextField which contains the path
     * @param event              event from button which lead to opening the file chooser
     * @param isDirectoryChooser if true a directory is choosen if false it's a file chooser
     *                           Handles the logic behind file/directory chooser
     */
    private File handleDirectoryChooser(String text, @Annotations.Nullable TextField tText, ActionEvent event, boolean isDirectoryChooser) {
        if (isDirectoryChooser) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(text);
            File directory = directoryChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
            if (directory != null && tText != null) {
                tText.setText(directory.getAbsolutePath());
            }
            return directory;
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(text);
            File file = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());
            if (file != null && tText != null) tText.setText(file.getAbsolutePath());
            return file;
        }
    }


    @FXML
    private void onSearchSample(ActionEvent actionEvent) {
        handleDirectoryChooser("choose sample directory", tDirectorySample, actionEvent, true);
    }

    @FXML
    private void onSearchTruth(ActionEvent actionEvent) {
        handleDirectoryChooser("choose truth directory", tDirectoryTruth, actionEvent, true);
    }

    @FXML
    private void onSearchTestSet(ActionEvent actionEvent) {
        handleDirectoryChooser("choose test set directory", tDirectoryTestSet, actionEvent, true);
    }

    @FXML
    private void onProcess(ActionEvent actionEvent) {
        Prefs.blackBackground = true;

        String sSample = tDirectorySample.getText();
        String sTruth = tDirectoryTruth.getText();

        if (sSample == null || sSample.isEmpty() || sTruth == null || sTruth.isEmpty()) return;

        File samples = new File(sSample);
        File truth = new File(sTruth);

        if (!samples.isDirectory() || !truth.isDirectory()) return;

        String regexSamples = tNamingSample.getText().
                replace("%imgNr%", "([0-9]*)").
                replace("%cellNr%", "([0-9]*)");

        String regexTruth = tNamingTruth.getText().
                replace("%imgNr%", "([0-9]*)").
                replace("%cellNr%", "([0-9]*)");

        Pattern patternSamples = Pattern.compile(regexSamples, Pattern.CASE_INSENSITIVE);


        List<Pair<File, File>> pairs = Arrays.stream(Objects.requireNonNull(samples.listFiles())).map(file -> {
            Matcher matcher = patternSamples.matcher(file.getName());
            boolean matchFound = matcher.find();
            if (!matchFound) return null;
            //System.out.println(regexSamples+"\t"+file.getName()+" :"+matchFound);
            String grp1 = matcher.group(1);
            String grp2 = null;
            if (matcher.groupCount() > 1) {
                grp2 = matcher.group(3);
            }
            String name = tNamingTruth.getText().replace("%imgNr%", grp1);
            if (grp2 != null) {
                int iGrp2 = Integer.parseInt(grp2);
                if (chkNoNumberforOne.isSelected() && iGrp2 == 1) {
                    name = name.replace("%cellNr%", "");

                } else name = name.replace("%cellNr%", grp2);
            } else {
                //remove optional parts
                int idxOpen = name.indexOf("(");
                int idxClose = name.indexOf("?");
                name = name.replace(name.substring(idxOpen, idxClose), "");
            }
            String fName = name.replace("(", "").replace(")", "").replace("?", "");
            //System.out.println("truthName:"+fName);

            if (file.getName().contains("1F.tif")) {
                System.out.println("debug");
            }
            File truthMatch = Arrays.stream(Objects.requireNonNull(truth.listFiles())).
                    filter(fTruth -> fTruth.getName().equals(fName)).findAny().orElse(null);

            if (truthMatch != null) return new Pair<>(file, truthMatch);
            else return null;

        }).filter(Objects::nonNull).collect(Collectors.toList());

        System.out.println(pairs);
        FAThresholdOptimizer optimizer = new FAThresholdOptimizer();
        if (tManualCheckAgainst.getText() == null || tManualCheckAgainst.getText().isEmpty())
            tManualCheckAgainst.setText("-1");
        int val = Integer.parseInt(tManualCheckAgainst.getText());


        List<ResultModel> result = optimizer.run(pairs, cbMethods.getSelectionModel().getSelectedItem(), val);

        tableResults.getItems().clear();
        tableResults.setItems(FXCollections.observableList(result));

        CompletableFuture.runAsync(() -> {
            File dirResult = new File(samples.getAbsolutePath() + File.separator + "results");
            dirResult.mkdir();
            result.parallelStream().forEach(model -> {
                File sample = new File(model.getFileName());
                ImageWrapper wrapper = new ImageWrapper(sample, new Settings());
                if (wrapper.getWorker() != null) {
                    try {
                        wrapper.getWorker().get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                FilterQueue queue = new FilterQueue();
                FilterManualThreshold thresh = new FilterManualThreshold();
                thresh.setThreshold((int) model.getThresh());
                queue.add(thresh);
                queue.run(wrapper, f -> {
                });
                ImageExporter.exportImage(wrapper.getImage(), new File(dirResult + File.separator + sample.getName().replace(".tif", ".png")));


            });


        });

    }
}
