package interfaces;

import core.Const;
import core.filaments.AbstractFilament;
import core.settings.Settings;
import fx.custom.StackImageView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import core.cell.DataFilaments;
import core.image.Entry;
import util.Annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface IFilamentUpdater {

    /**
     * Updates the Filament-Image in a view
     */
    void updateFilamentImage();


    void initFilamentsPolyline(Entry wrapperEntry);


    /**
     * 2019-12-10: scaling works automatically (the points of the Polyline's are dependant to scale-property)
     * color and width are also set by bindings.
     * This Method adds all Filament's found in wrapperEntry to svFilaments with the given colors.
     * @param wrapperEntry
     * @param svFilaments
     * @param stroke
     * @param strokeKeepFalse
     * @param strokeVerified
     * @param strokeSelected
     * @param callback
     */
    static void initFilamentsPolyline(Entry wrapperEntry, StackImageView svFilaments, final Paint stroke, final Paint strokeKeepFalse, final Paint strokeVerified, final Paint strokeSelected, @Nullable Consumer<List<Polyline>> callback, Settings dp) {

        Platform.runLater(() ->
                svFilaments.getStackPane().getChildren().
                        removeAll(
                                svFilaments.getStackPane().getChildren().
                                        stream().filter(e -> e instanceof Pane && Objects.equals(e.getUserData(), "filaments")).collect(Collectors.toList())
                        )
        );
        if (wrapperEntry == null ||
                wrapperEntry.getDataFilament() == null ||
                wrapperEntry.getDataFilament().getFilaments() == null ||
                wrapperEntry.getDataFilament().getFilaments().size() == 0) return;


        DataFilaments dataFilaments = wrapperEntry.getDataFilament();
        List<AbstractFilament> filaments = dataFilaments.getFilteredFilaments(dp);

        Pane polylines = new Pane();
        polylines.setUserData("filaments");

        final double origWidth = wrapperEntry.getProcessor().getWidth();
        final double origHeight = wrapperEntry.getProcessor().getHeight();

        DoubleBinding scaleBinding = svFilaments.getScaleBinding();

        for (AbstractFilament fil : filaments) {
            //init
            double[] points = new double[fil.getPoints().size() * 2];
            List<Point> pointList = fil.getPoints();
            for (int i = 0; i < pointList.size(); i++) {
                points[2 * i] = pointList.get(i).getX() * scaleBinding.get();
                points[2 * i + 1] = pointList.get(i).getY() * scaleBinding.get();
            }
            Polyline filament = new Polyline(points);
            //scaleBinding listener, if changes appear -> redo the list
            //apply zoom
            scaleBinding.addListener((observable, oldValue, newValue) -> {
                filament.getPoints().clear();
                AbstractFilament abstractFilament = (AbstractFilament) filament.getUserData();
                abstractFilament.getPoints().forEach(p -> {
                    filament.getPoints().add(p.getX() * newValue.doubleValue());
                    filament.getPoints().add(p.getY() * newValue.doubleValue());
                });
            });


            filament.setUserData(fil);
            filament.strokeProperty().bind(Bindings.createObjectBinding(() ->
                            fil.selectedProperty().get() ? strokeSelected : fil.keepProperty().get() ?
                                    ((fil.verifiedProperty().get()) ? strokeVerified : stroke) : strokeKeepFalse,
                    fil.selectedProperty(), fil.keepProperty(), fil.verifiedProperty())
            );
            filament.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getSource() instanceof Polyline) {//onclick funktioniert
                    Polyline source = (Polyline) mouseEvent.getSource();
                    if (source.getUserData() instanceof AbstractFilament) {
                        ((AbstractFilament) source.getUserData()).setKeep(!((AbstractFilament) source.getUserData()).isKeep());
                        //flip the keep state
                    }
                }
            });
            filament.strokeWidthProperty().bind(scaleBinding.multiply(fil.getWidth() / Const.MF));
            polylines.getChildren().add(filament);
        }
        polylines.maxHeightProperty().bind(svFilaments.getStackPane().heightProperty());
        polylines.maxWidthProperty().bind(svFilaments.getStackPane().widthProperty());
        polylines.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(polylines, Pos.TOP_LEFT);
        Platform.runLater(() -> {
            svFilaments.getStackPane().getChildren().add(polylines);
            if (callback != null)
                callback.accept(polylines.getChildren().stream().map(i -> (Polyline) i).collect(Collectors.toList()));
        });//wrap it with javaFx thread
    }


}
