package utils;

import filters.IFilter;
import fx.custom.SliderSpinner;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import core.FilterQueue;
import filters.supplier.CBSupplier;
import core.settings.Any;
import core.settings.Settings;
import util.Annotations;


import java.lang.reflect.Field;
import java.util.*;


public class FilterUIFactory {
    private static final String TAB_DRAG_KEY = "titledpane";

    //Sources from: https://stackoverflow.com/questions/18929161/how-to-move-items-with-in-vboxchange-order-by-dragging-in-javafx
    private static void initDragAndDrop(TitledPane pane, FilterQueue model, ObjectProperty<TitledPane> draggingTab) {

        pane.setOnDragOver(event -> {
            final Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()
                    && TAB_DRAG_KEY.equals(dragboard.getString())
                    && draggingTab.get() != null) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        pane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Pane parent = (Pane) pane.getParent();
                Object source = event.getGestureSource();
                int sourceIndex = parent.getChildren().indexOf(source);
                int targetIndex = parent.getChildren().indexOf(pane);

                model.rotateFilters((IFilter) ((TitledPane) source).getUserData(), (IFilter) pane.getUserData());


                List<Node> nodes = new ArrayList<>(parent.getChildren());
                if (sourceIndex < targetIndex) {
                    Collections.rotate(
                            nodes.subList(sourceIndex, targetIndex + 1), -1);

                } else {
                    Collections.rotate(
                            nodes.subList(targetIndex, sourceIndex + 1), 1);
                }
                parent.getChildren().clear();
                parent.getChildren().addAll(nodes);


                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        pane.setOnDragDetected(event -> {
            Dragboard dragboard = pane.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(TAB_DRAG_KEY);
            WritableImage snapshot = pane.snapshot(new SnapshotParameters(), null);
            dragboard.setDragView(snapshot);
            dragboard.setContent(clipboardContent);
            draggingTab.set(pane);
            event.consume();
        });


    }


    private static Button getDeleteButton(Pane parent, FilterQueue model, Node node, IFilter filter) {
        Button btnDelete = new Button("Remove Filter");
        btnDelete.getStyleClass().add("dark-blue");
        btnDelete.setOnAction((e) -> {
            parent.getChildren().remove(node);
            model.remove(filter);
            model.setChanged(true);//notify listeners
        });
        return btnDelete;
    }

    public static Button getButtonForFilter(String clsName, VBox boxFilters, ObjectProperty<TitledPane> draggingTab, Settings dp, FilterQueue model) throws ClassNotFoundException {
        Class<IFilter> cls = (Class<IFilter>) Class.forName(clsName);
        if (!cls.isAnnotationPresent(Annotations.FilterUI.class)) {
            throw new RuntimeException("PreprocessingFilterQueue::getButtonForFilter() --- no FilterUI-Annotation present in " + clsName);
        }
        Button btn = new Button(cls.getSimpleName());
        btn.getStyleClass().add("shiny-orange");
        btn.setOnAction(event -> {
            try {
                IFilter filter = cls.newInstance();
                FilterUIFactory.generateFilterUI(filter, boxFilters, model, draggingTab, false, dp);
                model.setChanged(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return btn;
    }


    private static void addSlider(Field field, IFilter filter, VBox content, Settings dp) throws IllegalAccessException {
        if (field.isAnnotationPresent(Annotations.Max.class) &&
                field.isAnnotationPresent(Annotations.Min.class)) {
            double value = ((Property<Number>) field.get(filter)).getValue().doubleValue();
            SliderSpinner ssValue = new SliderSpinner(field.getAnnotation(Annotations.Min.class).value(), field.getAnnotation(Annotations.Max.class).value());
            if (field.isAnnotationPresent(Annotations.NumberFormat.class))
                ssValue.setType(field.getAnnotation(Annotations.NumberFormat.class).value());
            if (field.isAnnotationPresent(Annotations.Step.class))
                ssValue.setTick(field.getAnnotation(Annotations.Step.class).value());
            ((Property<Number>) field.get(filter)).bind(ssValue.valueProperty());
            if (field.isAnnotationPresent(Annotations.Default.class)) {
                Annotations.Default def = field.getAnnotation(Annotations.Default.class);
                String defaultValue = def.value();
                try {
                    Any key = getEnumFromDefault(defaultValue);
                    if (key == null) {
                        try {
                            ssValue.setValue(Double.parseDouble(defaultValue));
                        } catch (NumberFormatException nfe) {
                            ssValue.setValue(value);
                        }
                    } else ssValue.setValue(dp.getValueAsDouble(key));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                ssValue.setValue(value);
            }
            content.getChildren().add(ssValue);
        }
    }

    private static Any getEnumFromDefault(String value) throws ClassNotFoundException {
        String[] parts = value.split("\\.");
        String key = parts[parts.length - 1];
        try {
            String enumName = value.substring(0, value.length() - (key.length() + 1));
            Class<Enum> enumClass = (Class<Enum>) Class.forName(enumName);
            Object t = Enum.valueOf(enumClass, key);
            if (!(t instanceof Any)) {
                throw new RuntimeException("FilterUIFactory - enum does not implement Any-Interface");
            }
            return (Any) t;
        } catch (StringIndexOutOfBoundsException siobe) {
            return null;
        }

    }


    private static void addCheckBox(Field field, IFilter filter, VBox content, Settings dp) throws IllegalAccessException {
        CheckBox chk = new CheckBox();
        ((Property<Boolean>) field.get(filter)).bind(chk.selectedProperty());
        if (field.isAnnotationPresent(Annotations.Default.class)) {
            Annotations.Default def = field.getAnnotation(Annotations.Default.class);
            String value = def.value();
            try {
                Any key = getEnumFromDefault(value);
                chk.setSelected(dp.getValueAsBoolean(key));
            } catch (ClassNotFoundException e) {
                //this part is not an error (not for sure), here it goes if the "default" is just a value
                chk.setSelected(Boolean.parseBoolean(value));
                e.printStackTrace();
            }
        }
        content.getChildren().add(chk);
    }

    private static void addComboBox(Field field, IFilter filter, VBox content, Settings dp) throws IllegalAccessException {
        if (field.isAnnotationPresent(Annotations.Values.class)) {
            ComboBox<String> comboBox = new ComboBox<>();
            Object initValue = ((Property<?>) field.get(filter)).getValue();
            String value = field.getAnnotation(Annotations.Values.class).value();
            try {
                CBSupplier<?> supplier = (CBSupplier<?>) Class.forName(value).newInstance();
                comboBox.getItems().setAll(supplier.get());
                comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue != oldValue) {
                        try {
                            ((Property<Object>) field.get(filter)).setValue(supplier.getValue(newValue));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                });

                if (field.isAnnotationPresent(Annotations.Default.class)) {
                    Annotations.Default def = field.getAnnotation(Annotations.Default.class);
                    String defaultValue = def.value();
                    try {
                        Any key = getEnumFromDefault(defaultValue);
                        comboBox.getSelectionModel().select(supplier.get(dp.getValue(key)));
                    } catch (ClassNotFoundException e) {
                        //this part is not an error (not for sure), here it goes if the "default" is just a value
                        comboBox.getSelectionModel().select(defaultValue);
                        e.printStackTrace();
                    }
                } else {
                    comboBox.getSelectionModel().select(supplier.get(initValue));
                }
            } catch (ClassNotFoundException ex) {
                //not a class string, should be handled like a value string
                comboBox.getItems().setAll(Arrays.asList(value.split(",")));
                comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue != oldValue) {
                        try {
                            field.set(filter, newValue);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                });

                if (field.isAnnotationPresent(Annotations.Default.class)) {
                    Annotations.Default def = field.getAnnotation(Annotations.Default.class);
                    String defaultValue = def.value();
                    comboBox.getSelectionModel().select(defaultValue);
                } else {
                    comboBox.getSelectionModel().select(0);
                }
            } catch (Exception ex) {
                //other exception like ClassCastException, something is wrong with the input
                ex.printStackTrace();
            }

            content.getChildren().add(comboBox);
        }

    }


    private static Node getFieldUI(Field field, IFilter filter, Settings dp) throws Exception {
        Annotations.FilterUIField fieldAnnotation = field.getAnnotation(Annotations.FilterUIField.class);
        String fLabel = fieldAnnotation.label();
        if (fLabel.isEmpty()) fLabel = field.getName();
        Annotations.FilterUIType fType = fieldAnnotation.type();
        field.setAccessible(true);
        if (!(field.get(filter) instanceof Property<?>))
            throw new Exception("FilterUIFactory::field is not sub-class of javafx.beans.property");

        VBox content = new VBox();
        content.getChildren().add(new Label(fLabel + ":"));

        switch (fType) {
            case none:
                return new VBox();
            case slider:
                addSlider(field, filter, content, dp);
                break;
            case checkbox:
                addCheckBox(field, filter, content, dp);
                break;
            case combobox:
                addComboBox(field, filter, content, dp);
                break;
            default:
                break;
        }
        return content;
    }

    public static void generateFilterUI(IFilter filter, Pane parent, FilterQueue model, ObjectProperty<TitledPane> draggingTab,
                                        boolean doNotAddToQueue, Settings dp) throws Exception {
        VBox content = new VBox();
        Class<? extends IFilter> filterClass = filter.getClass();
        if (!filterClass.isAnnotationPresent(Annotations.FilterUI.class))
            throw new Exception("FilterUIFactory::FilterUI Annotation not present:" + filterClass.getName());
        String label = filterClass.getDeclaredAnnotation(Annotations.FilterUI.class).label();
        if (label.isEmpty()) {
            label = filterClass.getSimpleName();
        }
        Arrays.stream(filterClass.getDeclaredFields()).
                filter(field -> field.isAnnotationPresent(Annotations.FilterUIField.class)).
                forEachOrdered(field -> {
                    try {
                        content.getChildren().add(getFieldUI(field, filter, dp));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        //);

        TitledPane titledPane = new TitledPane();
        titledPane.setText(label);
        titledPane.setContent(content);
        content.getChildren().add(getDeleteButton(parent, model, titledPane, filter));
        titledPane.setUserData(filter);
        initDragAndDrop(titledPane, model, draggingTab);
        parent.getChildren().add(titledPane);
        if (!doNotAddToQueue) model.add(filter);


    }


}
