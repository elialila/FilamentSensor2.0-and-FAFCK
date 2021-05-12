package core.filaments;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
 *               2013-2014 Benjamin Eltzner
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

import core.Calc;
import core.Const;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import util.NotImplementedException;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filament extends SingleFilament {

    // center is scaled by a factor of 10.
    // length, angle and width are longs that are scaled by 1,000,000
    // to avoid the problems of floating point arithmetics.

    // 0 =< angle < 180,000,000


    public Filament() {
        super();
    }

    public Filament(Point a, Point b, long width, boolean keep) {
        super(keep);
        //a==head, b==tail
        getPoints().add(a);
        getPoints().add(b);

        setCenter(calcCenter(a, b));
        setLength(Calc.distanceM(a, b));
        setOrientation(Calc.orientation(a, b));
        setWidth(width);

    }

    @Override
    public ClusterableFilament makeClusterable(int time) {
        return new ClusterableFilament(this, time);
    }

    // Getters


    @Override
    public List<Filament> splitIntoLinearPieces() {
        List<Filament> out = new ArrayList<>();
        out.add(this);
        return out;
    }

    @Override
    public List<Filament> splitIntoLinearPieces(int max_size) {
        List<Filament> out = new ArrayList<>();
        int n_pieces = (int) Math.ceil(getLength() / (1e6 * max_size));
        Point start = new Point(getHead());
        double x_shift = (getTail().x - getHead().x) / (1. * n_pieces),
                y_shift = (getTail().y - getHead().y) / (1. * n_pieces);
        for (int i = 1; i < n_pieces + 1; i++) {
            Point tmp = new Point((int) Math.round(getHead().x + i * x_shift),
                    (int) Math.round(getHead().y + i * y_shift));
            out.add(new Filament(start, tmp, getWidth(), isKeep()));
            start = new Point(tmp);
        }
        out.forEach(f -> f.setParent(this));
        return out;
    }

    @Override
    public void combine(AbstractFilament filament) {
        //@todo implement
        //calc if head or tail is expanded
        throw new NotImplementedException();

    }

    public Point getHead() {
        return getPoints().get(0);
    }

    public Point getTail() {
        return getPoints().get(1);
    }




    public double getMass() {
        return getLength() * getWidth() / (Const.MF * Const.MF);
    }



    @Override
    public void invert() {
        Collections.reverse(points);
    }


    @Override
    public Element toXML(Document document, int chain_number, int filament_number) {
        Element element = document.createElement("filament" + chain_number + "_" + filament_number);

        element.setAttribute("start_x", "" + getHead().x);
        element.setAttribute("start_y", "" + getHead().y);
        element.setAttribute("end_x", "" + getTail().x);
        element.setAttribute("end_y", "" + getTail().y);
        element.setAttribute("center_x", "" + getCenter().x);
        element.setAttribute("center_y", "" + getCenter().y);
        element.setAttribute("length", "" + getLength());
        element.setAttribute("angle", "" + getOrientation());
        element.setAttribute("width", "" + getWidth());
        element.setAttribute("keep", isKeep() ? "yes" : "no");

        return element;
    }

    // Note that the coordinates of the center are scaled by a factor of 10
    // in comparison to the coordinates of the corner points.
    private static Point calcCenter(Point a, Point b) {
        return new Point((a.x + b.x) * 5, (a.y + b.y) * 5);
    }

    @Override
    public Object clone() {
        Filament dup = new Filament(new Point(getHead().x, getHead().y), new Point(getTail().x, getTail().y), getWidth(), isKeep());
        return dup;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object.getClass().equals(this.getClass()))) {
            return false;
        }

        Filament fil = (Filament) object;
        return (isKeep() == fil.isKeep() && getHead().x == fil.getHead().x && getHead().y == fil.getHead().y && getTail().x == fil.getTail().x
                && getTail().y == fil.getTail().y && getWidth() == fil.getWidth() && getCenter().x == fil.getCenter().x && getCenter().y == fil.getCenter().y
                && getLength() == fil.getLength() && getOrientation() % (180 * Const.M) == fil.getOrientation() % (180 * Const.M));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getHead() == null) ? 0 : getHead().hashCode());
        result = prime * result + (int) (getOrientation() ^ (getOrientation() >>> 32));
        result = prime * result + ((getTail() == null) ? 0 : getTail().hashCode());
        result = prime * result + ((getCenter() == null) ? 0 : getCenter().hashCode());
        result = prime * result + (isKeep() ? 1231 : 1237);
        result = prime * result + (int) (getLength() ^ (getLength() >>> 32));
        result = prime * result + (int) (getWidth() ^ (getWidth() >>> 32));
        return result;
    }

    public double[] distanceSquares(final Filament that) {
        final double[] out = new double[2];
        double tmp = Math.abs(getOrientation() - that.getOrientation()) / Const.MF;
        out[0] = (tmp > 90. ? 180. - tmp : tmp);
        out[0] = out[0] * out[0];
        final Point p1 = getHead();
        final Point q1 = getTail();
        final Point p2 = that.getHead();
        final Point q2 = that.getTail();
        final int z1_x = p1.x - q1.x;
        final int z1_y = p1.y - q1.y;
        final int z2_x = p2.x - q2.x;
        final int z2_y = p2.y - q2.y;
        final int h_x = p1.x - p2.x;
        final int h_y = p1.y - p2.y;
        final double t2 = (z1_x * h_y - z1_y * h_x) / (double) (z1_x * z2_y - z1_y * z2_x);
        if (t2 >= 0 && t2 <= 1) {
            final double t1 = (h_x + z2_x * t2) / (double) z1_x;
            if (t1 >= 0 && t1 <= 1) {
                out[1] = 0;
                return out;
            }
        }
        double dist_square = pointLineDistanceSquare(p1, q1, p2);
        tmp = pointLineDistanceSquare(p1, q1, q2);
        dist_square = (Math.min(dist_square, tmp));
        tmp = pointLineDistanceSquare(p2, q2, p1);
        dist_square = (Math.min(dist_square, tmp));
        tmp = pointLineDistanceSquare(p2, q2, q1);
        out[1] = (Math.min(dist_square, tmp));
        return out;
    }
}
