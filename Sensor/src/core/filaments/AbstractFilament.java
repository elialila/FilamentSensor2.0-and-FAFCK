package core.filaments;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2013 Benjamin Eltzner
 *
 * FilamentSensor is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * or see <http://www.gnu.org/licenses/>.
 */

import focaladhesion.Verifier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.beans.Transient;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * This class enables sorting lists of Filament and FilamentChain objects. This
 * defines an injective mapping of lists of Filaments into lists of
 * FilamentChains and a well-defined mapping of lists of FilamentChains onto
 * lists of Filaments.
 */
public abstract class AbstractFilament implements Cloneable, Serializable, Comparable<AbstractFilament> {

    //indicator if the filament was verified by an other procedure
    protected transient BooleanProperty verified;
    //indicator if the filament should be kept, mainly for ui purpose
    protected transient BooleanProperty keep;
    //indicator if the filament is selected, mainly for ui purpose
    protected transient BooleanProperty selected;
    //is used to relate the results of splitIntoLinearPieces with the original filaments
    protected transient AbstractFilament parent = null;


    protected Verifier verifier;

    protected int number;//just for identification on UI

    protected long length;

    protected long orientation;

    protected Point center;

    protected long width;
    //id/hash of the corresponding orientationField
    //assumes that a filament is only related to one single OrientationField
    //set this values in the label method of orientationFields
    /**
     * Default Value = -1, which means not assigned
     */
    protected int orientationField = -1;

    protected int trackingId = -1;

    protected boolean possibleError;

    public boolean isPossibleError() {
        return possibleError;
    }

    public void setPossibleError(boolean possibleError) {
        this.possibleError = possibleError;
    }

    public AbstractFilament() {
        keep = new SimpleBooleanProperty();
        verified = new SimpleBooleanProperty();
        selected = new SimpleBooleanProperty();
    }

    public int getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(int trackingId) {
        this.trackingId = trackingId;
    }

    @java.beans.Transient
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Verifier getVerifier() {
        return verifier;
    }

    public void setVerifier(Verifier verifier) {
        this.verifier = verifier;
    }

    public int getOrientationField() {
        return orientationField;
    }

    public void setOrientationField(int orientationField) {
        this.orientationField = orientationField;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public void setOrientation(long orientation) {
        this.orientation = orientation;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public long getLength() {
        return length;
    }

    public long getOrientation() {
        return orientation;
    }

    public Point getCenter() {
        return center;
    }

    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public boolean isVerified() {
        return verified.get();
    }

    @java.beans.Transient
    public BooleanProperty verifiedProperty() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified.set(verified);
    }


    @java.beans.Transient
    public BooleanProperty keepProperty() {
        return keep;
    }

    public boolean isKeep() {
        return (keepProperty() == null) || keepProperty().get();
    }

    public void setKeep(boolean keep) {
        if (keepProperty() != null) keepProperty().set(keep);
    }


    public abstract void invert();


    public abstract Element toXML(Document document, int chain_number, int filament_number);

    protected double pointLineDistanceSquare(Point p1, Point q1, Point p) {
        int z_x = p1.x - q1.x, z_y = p1.y - q1.y, h_x = p.x - q1.x, h_y = p.y - q1.y;
        double t = (z_x * h_x + z_y * h_y) / (double) (z_x * z_x + z_y * z_y);
        if (t > 1) {
            return p1.distanceSq(p);
        }
        if (t < 0) {
            return q1.distanceSq(p);
        }
        double d_x = q1.x + t * z_x - p.x, d_y = q1.y + t * z_y - p.y;
        return d_x * d_x + d_y * d_y;
    }

    @Override
    public abstract Object clone();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    public abstract double getMass();

    public abstract List<Point> getPoints();

    @Override
    public int compareTo(AbstractFilament filament) {
        if (getLength() > filament.getLength()) {
            return 1;
        }
        if (getLength() < filament.getLength()) {
            return -1;
        }
        //if it is not greater than filament and not lower than filament, what else can it be .... equal
        //no need for   if (getLength() == filament.getLength())
        if (getWidth() > filament.getWidth()) {
            return 1;
        }
        if (getWidth() < filament.getWidth()) {
            return -1;
        }
        //no need for width equals if (not lower and not larger)
        //if (getWidth() == filament.getWidth())

        if (getCenter().y > filament.getCenter().y) {
            return 1;
        }
        if (getCenter().y < filament.getCenter().y) {
            return -1;
        }
        //again  if (getCenter().y == filament.getCenter().y)
        if (getCenter().x > filament.getCenter().x) {
            return 1;
        }
        if (getCenter().x < filament.getCenter().x) {
            return -1;
        }
        //if (getCenter().x == filament.getCenter().x)
        if (getOrientation() > filament.getOrientation()) {
            return 1;
        }
        if (getOrientation() < filament.getOrientation()) {
            return -1;
        }
        return 0;
    }


    public void drawToImage(boolean[][] image) {
        List<Point> points = getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            Point p_1 = points.get(i), p_2 = points.get(i + 1);
            int d_x = p_2.x - p_1.x, d_y = p_2.y - p_1.y, d = Math.max(Math.abs(d_x), Math.abs(d_y));
            double step_x = d_x / (double) d, step_y = d_y / (double) d, p_x = p_1.x, p_y = p_1.y;
            for (int k = 0; k <= d; k++) {
                image[(int) Math.round(p_x)][(int) Math.round(p_y)] = true;
                p_x += step_x;
                p_y += step_y;
            }
        }
    }


    /**
     * This method takes a list as argument and has no return value. As the
     * elements of the list are changed (permuted), the argument itself is
     * changed by the method and thus the return is implicit.
     * <p>
     * The list is first sorted and then reversed so as to begin with the
     * "largest" element.
     */
    public static void sortList(List<? extends AbstractFilament> list) {
        Collections.sort(list);
        Collections.reverse(list);
    }

    public abstract List<Filament> splitIntoLinearPieces();

    /**
     * @param max_size parameter which decides in how many linear pieces a filament gets segmented
     * @return
     */
    public abstract List<Filament> splitIntoLinearPieces(int max_size);


    public AbstractFilament getParent() {
        return parent;
    }

    public void setParent(AbstractFilament parent) {
        this.parent = parent;
    }

    /**
     * Combines two Filaments into one
     *
     * @param filament which the current(this) will combine with
     */
    public abstract void combine(AbstractFilament filament);

}
