package tracking.shape;

import core.settings.Settings;
import javafx.beans.binding.DoubleBinding;

import java.awt.*;
import java.io.Serializable;

public interface TrackingShape extends Serializable {


    void setIdentifier(int identifier);

    /**
     * Returns a Shape(like Rectangle, Ellipse, Polygon etc.)
     * It has to be a closed Shape for Area calculation etc. to work
     *
     * @return
     */
    Shape getBounds(Settings dp);

    /**
     * Returns the JavaFX Shape for the Object (for better displaying)
     *
     * @param dp
     * @return
     */
    javafx.scene.shape.Shape getFXBounds(Settings dp, DoubleBinding scale);


    /**
     * Builds Csv for Current TrackingShape implementation
     *
     * @param builder
     * @param withHeadline
     * @param version
     * @return
     */
    void buildCsv(StringBuilder builder, boolean withHeadline, int version);


}
