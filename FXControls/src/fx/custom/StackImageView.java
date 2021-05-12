package fx.custom;


import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;


import java.awt.geom.Point2D;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * Class for handling the whole ui display of image stack
 */
public class StackImageView extends TitledPane {
    private static final String DRAWING = "drawing";


    //region Properties
    private IntegerProperty zoom;
    private BooleanProperty showControls;
    private BooleanProperty hideScroller;
    private IntegerProperty max;
    private IntegerProperty min;
    private IntegerProperty current;
    private BooleanProperty enableDrawing;
    //endregion


    private List<Point2D> listPoints;
    private ObjectProperty<BiConsumer<List<Point2D>, Polyline>> onLineCreated;
    private ObjectProperty<BiConsumer<List<Point2D>, Polyline>> onLineUpdated;
    private ObjectProperty<Polyline> currentLine;
    private Map<Integer, List<List<Point2D>>> lineMap;

    private DoubleBinding scaleBinding;


    //region JFX Properties
    @FXML
    private ImageView imageView;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private StackPane stackPane;
    @FXML
    private Slider slider;
    @FXML
    private Text txtFrom;
    @FXML
    private Text txtTo;
    @FXML
    private Spinner<Integer> spinnerZoom;
    @FXML
    private HBox hBoxZoom;
    @FXML
    private VBox paneStackScroller;
//endregion


    public StackImageView() {
        zoom = new SimpleIntegerProperty();
        showControls = new SimpleBooleanProperty(true);
        hideScroller = new SimpleBooleanProperty(false);
        max = new SimpleIntegerProperty();
        min = new SimpleIntegerProperty();
        current = new SimpleIntegerProperty();
        enableDrawing = new SimpleBooleanProperty(false);
        onLineCreated = new SimpleObjectProperty<>();
        onLineUpdated = new SimpleObjectProperty<>();
        currentLine = new SimpleObjectProperty<>();
        lineMap = new HashMap<>();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fx/custom/view/StackImageView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }


    private void initializeSlider() {
        //when max is bound, min creates an exception -> Slider.max : A bound value cannot be set. (YES, its right, there is Slider.MAX in the exception)
        //slider.maxProperty().bind(maxProperty());
        maxProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) slider.setMax(newValue.intValue());
        });

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) slider.valueProperty().setValue(newValue.intValue());
        });
        slider.minProperty().setValue(1);

        txtFrom.textProperty().bind(currentProperty().asString());
        txtTo.textProperty().bind(maxProperty().asString());

        slider.setOnScroll((event) -> {//enable scrolling on stack slider
            if (event.getDeltaY() > 0 && slider.getValue() < slider.getMax()) {
                slider.setValue(slider.getValue() + 1);
            } else if (event.getDeltaY() < 0 && slider.getValue() > 0) {
                slider.setValue(slider.getValue() - 1);
            }
        });
        current.bindBidirectional(slider.valueProperty());
    }


    public boolean updateDrawing(List<Point2D> points, int image) {
        if (lineMap.get(image) == null) return false;
        boolean result = lineMap.get(image).remove(points);
        updateDrawing();
        return result;
    }

    private void updateDrawing() {
        //update drawing data-model
        stackPane.getChildren().removeAll(stackPane.getChildren().stream().filter(p -> (p instanceof Pane) && p.getUserData().equals(DRAWING)).collect(Collectors.toList()));
        if (lineMap.get(getCurrent()) != null) {
            Pane pane = createPolyLinePane();
            lineMap.get(getCurrent()).forEach(list -> {
                Polyline line = createLine(list);
                if (onLineUpdated.get() != null) {
                    onLineUpdated.get().accept(list, line);
                }
                pane.getChildren().add(line);
            });
            getStackPane().getChildren().add(pane);
        }
    }

    private void finalizeLine() {
        onLineCreated.get().accept(listPoints, currentLine.get());
        currentLine.set(null);
        listPoints = null;
    }


    private void initDrawing() {
        enableDrawing.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && !oldValue && newValue != null && newValue) {

                if (onLineCreated.get() == null) return;
                currentProperty().addListener((observable1, oldValue1, newValue1) -> {
                    //current changed, clean up lines, draw all known lines for current image
                    updateDrawing();
                });
                imageView.setOnMouseDragged(event -> {
                    if (listPoints == null) {
                        event.consume();
                        return;
                    }
                    double x = event.getX(), y = event.getY();
                    //if outside of bounds set to the state where it would be on mouse release
                    if (!imageView.contains(new javafx.geometry.Point2D(x, y))) {
                        finalizeLine();
                        event.consume();
                        return;
                    }
                    drawOnImage((int) x, (int) y);
                    event.consume();
                });
                imageView.setOnMousePressed(event -> {
                    lineMap.computeIfAbsent(current.get(), k -> new ArrayList<>());
                    if (listPoints == null) {
                        listPoints = new ArrayList<>();
                        lineMap.get(current.get()).add(listPoints);
                    }
                    event.consume();
                });
                imageView.setOnMouseReleased(event -> {
                    if (listPoints != null) {
                        finalizeLine();
                    }
                    event.consume();
                });
            }
        });
    }


    //current state (2020-02-18 17:20) drawing of one line works with correct scale
    //after that, no more drag events are fired (the pane added for drawing most likely blocks the event)
    //https://stackoverflow.com/questions/36650240/javafx8-stackpane-children-blocking-mouse-events
    //explains the problem, only the top most nodes fire events
    //event blocking solved by setDisable(true)

    //state (2020-02-18 17:47) drawing of multiple lines works with correct scale
    //event blocking from panel above is solved
    //out of bounds is solved (line is only drawn within the ImageView)

    //bind the lines to an image -> if you change the image get rid of the lines and draw the lines that belong to that image

    //relation can not be done with hash of image (image is always converted from bufferedImage to FXImage, not known if it will be the same)
    //relation could be done with the image number (current property of StackImageView)

    //store the whole panes or just the List and recreate the pane?
    //storing list would take less memory, storing the whole pane could provide a bit more speed when switching images


    private Polyline createLine(List<Point2D> listPoints) {

        Polyline line = new Polyline();
        line.setUserData(listPoints);
        scaleBinding.addListener((observable, oldValue, newValue) -> {
            line.getPoints().clear();
            ((List<Point2D>) line.getUserData()).forEach(p -> {
                line.getPoints().add(p.getX() * newValue.doubleValue());
                line.getPoints().add(p.getY() * newValue.doubleValue());
            });
        });
        line.setStroke(javafx.scene.paint.Paint.valueOf("Red"));
        line.strokeWidthProperty().bind(scaleBinding.multiply(1));//set a factor

        if (listPoints.size() > 0) {
            listPoints.forEach(p -> {
                line.getPoints().add(p.getX() * scaleBinding.get());
                line.getPoints().add(p.getY() * scaleBinding.get());
            });
        }
        return line;
    }

    private Pane createPolyLinePane() {
        Pane polylines = new Pane();
        polylines.setUserData(DRAWING);//set user-data for an additional filter opportunity when removing elements
        polylines.setDisable(true);//prevents the pane from blocking mouse events of the layer behind(imageView)

        polylines.maxHeightProperty().bind(getStackPane().heightProperty());
        polylines.maxWidthProperty().bind(getStackPane().widthProperty());
        polylines.setStyle("-fx-background-color:transparent;");//transparent
        StackPane.setAlignment(polylines, Pos.TOP_LEFT);
        return polylines;
    }


    private void drawOnImage(double x, double y) {

        if (currentLine.get() == null) {
            Pane polylines = createPolyLinePane();

            Polyline line = createLine(listPoints);
            currentLine.set(line);

            polylines.getChildren().add(line);
            getStackPane().getChildren().add(polylines);
            //StackPane.setAlignment(line, Pos.TOP_LEFT);
            //getStackPane().getChildren().add(line);
        }
        listPoints.add(new Point2D.Double(x * (1 / scaleBinding.get()), y * (1 / scaleBinding.get())));
        currentLine.get().getPoints().add(x);
        currentLine.get().getPoints().add(y);
    }


    private void initializeZoom() {
        spinnerZoom.setEditable(true);
        spinnerZoom.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) zoom.setValue(newValue);
        });
        zoom.addListener((observable, oldValue, newValue) -> {
            if (spinnerZoom.getValueFactory() != null && newValue != null)
                spinnerZoom.getValueFactory().setValue(newValue.intValue());
        });
        EventHandler<? super ScrollEvent> zoomHandler = (event) -> {
            if (event.getDeltaY() > 0) {
                zoom.set((int) ((double) zoom.get() * 1.1));
            } else if (event.getDeltaY() < 0) {
                zoom.set((int) ((double) zoom.get() / 1.1));
            }
        };
        scrollPane.addEventFilter(ScrollEvent.ANY, zoomHandler);
        imageView.fitWidthProperty().bind(zoom.multiply(this.widthProperty()).divide(100));
        zoom.setValue(100);
    }


    @FXML
    private void initialize() {
        hBoxZoom.managedProperty().bind(hBoxZoom.visibleProperty());
        hBoxZoom.visibleProperty().bind(showControls);
        paneStackScroller.managedProperty().bind(paneStackScroller.visibleProperty());
        paneStackScroller.visibleProperty().bind(showControls.and(hideScroller.not()));

        scaleBinding = Bindings.createDoubleBinding(() -> {
            if (imageView.getImage() == null) return (double) 1;
            double cFitWidth = imageView.getFitWidth();
            double cFitHeight = imageView.getFitHeight();
            return (cFitWidth > 0) ? cFitWidth / imageView.getImage().getWidth() : (cFitHeight > 0) ? cFitHeight / imageView.getImage().getHeight() : 1;
        }, imageView.fitWidthProperty(), imageView.fitHeightProperty(), imageView.imageProperty());

        initializeSlider();
        initializeZoom();
        initDrawing();


    }

    //region getter-setter
    public boolean isHideScroller() {
        return hideScroller.get();
    }

    public BooleanProperty hideScrollerProperty() {
        return hideScroller;
    }

    public void setHideScroller(boolean hideScroller) {
        this.hideScroller.set(hideScroller);
    }

    public int getZoom() {
        return zoom.get();
    }

    public IntegerProperty zoomProperty() {
        return zoom;
    }

    public boolean getShowControls() {
        return showControls.get();
    }

    public BooleanProperty showControlsProperty() {
        return showControls;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public Slider getSlider() {
        return slider;
    }

    public void setZoom(int zoom) {
        this.zoom.set(zoom);
    }

    public void setShowControls(boolean showControls) {
        this.showControls.set(showControls);
    }

    public int getMax() {
        return max.get();
    }

    public IntegerProperty maxProperty() {
        return max;
    }

    public void setMax(int max) {
        this.max.set(max);
    }

    public int getCurrent() {
        return current.get();
    }

    public IntegerProperty currentProperty() {
        return current;
    }

    public void setCurrent(int current) {
        this.current.set(current);
    }

    public void setImage(Image image) {
        imageView.setImage(image);
    }

    public int getMin() {
        return min.get();
    }

    public IntegerProperty minProperty() {
        return min;
    }

    public void setMin(int min) {
        this.min.set(min);
    }

    public StackPane getStackPane() {
        return this.stackPane;
    }

    public boolean isEnableDrawing() {
        return enableDrawing.get();
    }

    public BooleanProperty enableDrawingProperty() {
        return enableDrawing;
    }

    public void setEnableDrawing(boolean enableDrawing) {
        this.enableDrawing.set(enableDrawing);
    }

    public BiConsumer<List<Point2D>, Polyline> getOnLineCreated() {
        return onLineCreated.get();
    }

    public ObjectProperty<BiConsumer<List<Point2D>, Polyline>> onLineCreatedProperty() {
        return onLineCreated;
    }

    public void setOnLineCreated(BiConsumer<List<Point2D>, Polyline> onLineCreated) {
        this.onLineCreated.set(onLineCreated);
    }

    public BiConsumer<List<Point2D>, Polyline> getOnLineUpdated() {
        return onLineUpdated.get();
    }

    public ObjectProperty<BiConsumer<List<Point2D>, Polyline>> onLineUpdatedProperty() {
        return onLineUpdated;
    }

    public void setOnLineUpdated(BiConsumer<List<Point2D>, Polyline> onLineUpdated) {
        this.onLineUpdated.set(onLineUpdated);
    }

    public DoubleBinding getScaleBinding() {
        return scaleBinding;
    }

    public Map<Integer, List<List<Point2D>>> getLineMap() {
        return lineMap;
    }

    //endregion
    //region Synchronize-ZoomBox-ScrollBox
    public void synchronize(StackImageView other) {
        maxProperty().bindBidirectional(other.maxProperty());
        minProperty().bindBidirectional(other.minProperty());
        currentProperty().bindBidirectional(other.currentProperty());
        zoomProperty().bindBidirectional(other.zoomProperty());
        scrollPane.hvalueProperty().bindBidirectional(other.scrollPane.hvalueProperty());
        scrollPane.vvalueProperty().bindBidirectional(other.scrollPane.vvalueProperty());
    }

    /**
     * Create a HBox with all Bindings for Zoom centralisation (in case of multiple synchronized stackviews)
     *
     * @return
     */
    public HBox getHBoxZoom() {
        HBox container = new HBox();

        container.getChildren().add(new Label("Zoom:"));
        Spinner<Integer> tmpSpinner = new Spinner<>();
        tmpSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000));
        tmpSpinner.getValueFactory().setValue(getZoom());
        tmpSpinner.setEditable(true);

        tmpSpinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) zoom.setValue(newValue);
        });
        zoom.addListener((observable, oldValue, newValue) -> {
            if (tmpSpinner.getValueFactory() != null && newValue != null)
                tmpSpinner.getValueFactory().setValue(newValue.intValue());
        });
        container.getChildren().add(tmpSpinner);

        return container;
    }

    /**
     * Create a VBox with all Bindings for StackScroller centralisation
     *
     * @return
     */
    public VBox getVBoxScroller() {

        VBox container = new VBox();

        HBox tmpBox = new HBox();
        tmpBox.getChildren().add(new Label("ImageStack Scroller"));
        Text from = new Text();
        Text sep = new Text("/");
        Text to = new Text();
        from.textProperty().bind(currentProperty().asString());
        to.textProperty().bind(maxProperty().asString());
        tmpBox.getChildren().addAll(from, sep, to);
        container.getChildren().add(tmpBox);


        Slider tmpSlider = new Slider();
        tmpSlider.setMajorTickUnit(1);
        tmpSlider.setMinorTickCount(1);
        tmpSlider.setShowTickLabels(false);
        tmpSlider.setShowTickMarks(true);
        tmpSlider.setSnapToTicks(true);
        tmpSlider.valueProperty().bindBidirectional(slider.valueProperty());

        maxProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("StackImageView::getVBoxScroller() --- max=" + newValue);
            if (newValue != null) tmpSlider.setMax(newValue.intValue());
        });
        minProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("StackImageView::getVBoxScroller() --- min=" + newValue);
            if (newValue != null) tmpSlider.setMin(newValue.intValue());
        });
        tmpSlider.setMin(getMin());
        tmpSlider.setMax(getMax());

        tmpSlider.setOnScroll((event) -> {//enable scrolling on stack slider
            if (event.getDeltaY() > 0 && tmpSlider.getValue() < tmpSlider.getMax()) {
                tmpSlider.setValue(tmpSlider.getValue() + 1);
            } else if (event.getDeltaY() < 0 && tmpSlider.getValue() > 0) {
                tmpSlider.setValue(tmpSlider.getValue() - 1);
            }
        });

        tmpSlider.setValue(getCurrent());

        container.getChildren().add(tmpSlider);

        return container;
    }
    //endregion
}
