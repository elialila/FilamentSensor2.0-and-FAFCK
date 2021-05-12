package model;

import javafx.beans.property.*;

import java.awt.*;

public class FilamentsModel {


    private Object tableFilaments;//should be the datamodel for table

    private SimpleStringProperty pictureType;
    private SimpleStringProperty colors;


    private SimpleBooleanProperty includeFilaments;
    private BooleanProperty includeAreaOutline;
    private ObjectProperty<Color> colorAreaOutline;

    public FilamentsModel() {

        pictureType = new SimpleStringProperty();
        colors = new SimpleStringProperty();
        includeFilaments = new SimpleBooleanProperty();
        includeAreaOutline = new SimpleBooleanProperty();
        colorAreaOutline = new SimpleObjectProperty<>();
    }

    public String getPictureType() {
        return pictureType.get();
    }

    public SimpleStringProperty pictureTypeProperty() {
        return pictureType;
    }

    public void setPictureType(String pictureType) {
        this.pictureType.set(pictureType);
    }

    public String getColors() {
        return colors.get();
    }

    public SimpleStringProperty colorsProperty() {
        return colors;
    }

    public void setColors(String colors) {
        this.colors.set(colors);
    }

    public boolean isIncludeFilaments() {
        return includeFilaments.get();
    }

    public SimpleBooleanProperty includeFilamentsProperty() {
        return includeFilaments;
    }

    public void setIncludeFilaments(boolean includeFilaments) {
        this.includeFilaments.set(includeFilaments);
    }


    public boolean isIncludeAreaOutline() {
        return includeAreaOutline.get();
    }

    public BooleanProperty includeAreaOutlineProperty() {
        return includeAreaOutline;
    }

    public void setIncludeAreaOutline(boolean includeAreaOutline) {
        this.includeAreaOutline.set(includeAreaOutline);
    }

    public Color getColorAreaOutline() {
        return colorAreaOutline.get();
    }

    public ObjectProperty<Color> colorAreaOutlineProperty() {
        return colorAreaOutline;
    }

    public void setColorAreaOutline(Color colorAreaOutline) {
        this.colorAreaOutline.set(colorAreaOutline);
    }
}
