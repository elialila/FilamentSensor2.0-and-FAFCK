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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CurvedFilament extends SingleFilament {
    // center is scaled by a factor of 10.
    // length, angle, width and curvatures are longs that are scaled
    // by 1,000,000 to avoid the problems of floating point arithmetics.
    // 0 =< angle < 180,000,000
    private long m_mean_signed_curvature;
    private long m_mean_absolute_curvature;

    public CurvedFilament() {
        super();
    }

    public CurvedFilament(List<Point> points, long width, int signed_curvature,
                          int absolute_curvature, boolean keep) {
        super();
        setPoints(points);
        setPoints(straighten());
        Point a = getPoints().get(0), b = getPoints().get(getPoints().size() - 1);
        setLength(0);
        long[] partial_lengths = new long[getPoints().size()];
        for (int i = 0; i < getPoints().size() - 1; i++) {
            setLength(getLength() + Calc.distanceM(getPoints().get(i), getPoints().get(i + 1)));
            partial_lengths[i + 1] = getLength();
        }
        setCenter(calcCenter(partial_lengths, getPoints()));
        setOrientation(Calc.orientation(a, b));
        setWidth(width);
        long[] curvatures = calcCurvatures(getPoints(), getLength());
        m_mean_signed_curvature = curvatures[0];
        m_mean_absolute_curvature = curvatures[1];
        setKeep(keep);
    }

    public CurvedFilament(List<Point> points, long width) {
        super();
        setPoints(points);
        Point a = points.get(0), b = points.get(points.size() - 1);
        setLength(0);
        long[] partial_lengths = new long[points.size()];
        for (int i = 0; i < points.size() - 1; i++) {
            length += Calc.distanceM(points.get(i), points.get(i + 1));
            partial_lengths[i + 1] = getLength();
        }
        setCenter(calcCenter(partial_lengths, points));
        setOrientation(Calc.orientation(a, b));
        setWidth(width);
        long[] curvatures = calcCurvatures(points, getLength());
        m_mean_signed_curvature = curvatures[0];
        m_mean_absolute_curvature = curvatures[1];
        setKeep(true);
    }

    public CurvedFilament(List<Point> points) {
        this(points, Const.M);
    }

    public CurvedFilament(Element element) {
        super();
        setPoints(pointsFromElement(element));
        setCenter(new Point(Integer.parseInt(element.getAttribute("center_x")),
                Integer.parseInt(element.getAttribute("center_y"))));

        setLength(Long.parseLong(element.getAttribute("length")));
        setOrientation(Long.parseLong(element.getAttribute("angle")));
        setWidth(Long.parseLong(element.getAttribute("width")));
        m_mean_signed_curvature = zeroOld(element.getAttribute("signed_curv"));
        m_mean_absolute_curvature = zeroOld(element.getAttribute("abs_curv"));
        setKeep(element.getAttribute("keep").equals("yes"));
    }

    public CurvedFilament(SingleFilament fil) {
        super();
        setPoints(new ArrayList<>());
        if (fil instanceof Filament) {
            Filament filament = (Filament) fil;
            getPoints().add(filament.getHead());
            getPoints().add(filament.getTail());
        } else if (fil instanceof CurvedFilament) {
            CurvedFilament filament = (CurvedFilament) fil;
            for (Point p : filament.getPoints()) {
                getPoints().add(new Point(p));
            }
        }

        setCenter(fil.getCenter());
        setLength(fil.getLength());
        setOrientation(fil.getOrientation());
        setWidth(fil.getWidth());

        m_mean_signed_curvature = fil.getSignedCurvature();
        m_mean_absolute_curvature = fil.getAbsoluteCurvature();
        setKeep(fil.isKeep());
    }

    private CurvedFilament(CurvedFilament that) {
        super();
        setPoints(new ArrayList<>());
        for (Point p : that.getPoints()) {
            getPoints().add(new Point(p));
        }
        setCenter(new Point(that.getCenter()));
        setLength(that.getLength());
        setOrientation(that.getOrientation());
        setWidth(that.getWidth());
        m_mean_signed_curvature = that.m_mean_signed_curvature;
        m_mean_absolute_curvature = that.m_mean_absolute_curvature;
        setKeep(that.isKeep());
    }

    @Override
    public ClusterableFilament makeClusterable(int time) {
        return new ClusterableFilament(this, time);
    }

    private static long zeroOld(String string) {
        return (string == "" ? 0 : Long.parseLong(string));
    }

    private static List<Point> pointsFromElement(Element element) {
        List<Point> points = new ArrayList<>();
        if (element.getAttribute("start_x").isEmpty()) {
            int i = 0;
            while (true) {
                String x = element.getAttribute("point" + i + "_x");
                if (x.isEmpty()) {
                    break;
                }
                points.add(new Point(Integer.parseInt(x),
                        Integer.parseInt(element.getAttribute("point" + i + "_y"))));
                i++;
            }
        } else {
            points.add(new Point(Integer.parseInt(element.getAttribute("start_x")),
                    Integer.parseInt(element.getAttribute("start_y"))));
            points.add(new Point(Integer.parseInt(element.getAttribute("end_x")),
                    Integer.parseInt(element.getAttribute("end_y"))));
        }

        return points;
    }

    // Getters


    @Override
    public List<Filament> splitIntoLinearPieces() {
        straighten();
        List<Filament> out = new ArrayList<>();
        if (getPoints().size() < 2) {
            return out;
        }
        if (getPoints().size() == 2) {
            out.add(new Filament(getPoints().get(0), getPoints().get(1), getWidth(), isKeep()));
            return out;
        }

        out.add(new Filament(getPoints().get(0), getPoints().get(1), getWidth(), isKeep()));
        for (int i = 1; i < getPoints().size() - 1; i++) {
            Point p1 = getPoints().get(i - 1), q1 = getPoints().get(i);

            // Avoid "touching end points".
            double vx = q1.x - p1.x, vy = q1.y - p1.y,
                    v = Math.max(Math.abs(vx), Math.abs(vy));
            int dx = (int) Math.round(vx / v), dy = (int) Math.round(vy / v);

            out.add(new Filament(new Point(q1.x + dx, q1.y + dy), getPoints().get(i + 1),
                    getWidth(), isKeep()));
        }
        out.forEach(f -> f.setParent(this));
        return out;
    }

    public List<Filament> splitIntoLinearPieces(int max_size) {
        straighten();
        int n_pieces = (int) Math.ceil(getLength() / (1e6 * max_size));
        double l_pieces = getLength() / (1e6 * n_pieces), rest_step = l_pieces;
        List<Point> points = new ArrayList<>();
        points.add(new Point(getPoints().get(0)));
        for (int k = 0; k < getPoints().size() - 1; k++) {
            Point p = getPoints().get(k), q = getPoints().get(k + 1);
            double piece_length = p.distance(q), piece_rest = piece_length,
                    x_shift = (q.x - p.x) / piece_length,
                    y_shift = (q.y - p.y) / piece_length;
            int counter = 0;
            while (piece_rest > (counter == 0 ? rest_step : l_pieces)) {
                double d = rest_step + counter * l_pieces;
                points.add(new Point((int) Math.round(p.x + d * x_shift),
                        (int) Math.round(p.y + d * y_shift)));
                counter++;
                piece_rest = piece_length - d;
            }
            rest_step = (counter == 0 ? rest_step : l_pieces) - piece_rest;
            if ((k == getPoints().size() - 2) && (piece_rest > 1e-3)) {
                points.add(new Point(q));
            }
        }

        List<Filament> out = new ArrayList<>();
        for (int i = 0; i < points.size() - 2; i++) {
            Point p1 = points.get(i), q1 = points.get(i + 1);
            out.add(new Filament(p1, q1, getWidth(), isKeep()));
        }

        out.forEach(f -> f.setParent(this));
        return out;
    }

    @Override
    public void combine(AbstractFilament filament) {

        List<Point> nPoints = new ArrayList<>();
        //calc if added in the beginning or end
        //if this.firstPoint > filament.lastPoint add before

        Point a1 = points.get(0), b1 = points.get(points.size() - 1),
                a2 = filament.getPoints().get(0),
                b2 = filament.getPoints().get(filament.getPoints().size() - 1);

        double distA1A2 = a1.distance(a2),
                distA1B2 = a1.distance(b2),
                distA2B1 = a2.distance(b1),
                distB1B2 = b1.distance(b2);
        double min = Math.min(Math.min(distA1A2, distA1B2), Math.min(distA2B1, distB1B2));
        if (min == distA1A2) {
            //rotate one list and combine
            System.out.println("CurvedFilament::combine() --- special case a1a2");
            List<Point> tmp = new ArrayList<>(filament.getPoints());
            Collections.reverse(tmp);
            nPoints.addAll(tmp);
            nPoints.addAll(points);

        } else if (min == distA1B2) {
            //add second points to first points
            nPoints.addAll(points);
            nPoints.addAll(filament.getPoints());
        } else if (min == distA2B1) {
            //add first points to second
            nPoints.addAll(filament.getPoints());
            nPoints.addAll(points);
        } else if (min == distB1B2) {
            //rotate one list
            System.out.println("CurvedFilament::combine() --- special case b1b2");
            List<Point> tmp = new ArrayList<>(filament.getPoints());
            Collections.reverse(tmp);
            nPoints.addAll(points);
            nPoints.addAll(tmp);
        }

        setPoints(nPoints);
        Point a = points.get(0), b = points.get(points.size() - 1);
        setLength(0);
        long[] partial_lengths = new long[points.size()];
        for (int i = 0; i < points.size() - 1; i++) {
            length += Calc.distanceM(points.get(i), points.get(i + 1));
            partial_lengths[i + 1] = getLength();
        }
        setCenter(calcCenter(partial_lengths, points));
        setOrientation(Calc.orientation(a, b));


        /*
        //or just take average
        //taken from FilamentChain::width()
        double numerator = 0;
        double denominator = m_filament_list.size();
        for (int i = 0; i < m_filament_list.size(); i++) {
            denominator += m_filament_list.get(i).getLength();
            numerator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth();
        }
        setWidth(Math.round(numerator / denominator));
        */

        setWidth((getWidth() + filament.getWidth()) / 2);//set width (average width?)

        long[] curvatures = calcCurvatures(points, getLength());
        m_mean_signed_curvature = curvatures[0];
        m_mean_absolute_curvature = curvatures[1];
        setKeep(true);


    }

    /*
    public Point getHead()
    {
        return new Point(m_points.get(0));
    }

    public Point getTail()
    {
        return new Point(m_points.get(m_points.size() - 1));
    }*/



    public double getMass() {
        return length * width / (Const.MF * Const.MF);
    }


    @Override
    public long getSignedCurvature() {
        return m_mean_signed_curvature;
    }

    @Override
    public long getAbsoluteCurvature() {
        return m_mean_absolute_curvature;
    }

    @Override
    public void setKeep(boolean keep) {
        keepProperty().set(keep);
    }

    @Override
    public void invert() {
        Collections.reverse(points);
    }


    @Override
    public Element toXML(Document document, int chain_number, int filament_number) {
        Element element = document.createElement("curved_filament" + chain_number + "_" + filament_number);

        int i = 0;
        for (Point p : getPoints()) {
            element.setAttribute("point" + i + "_x", "" + p.x);
            element.setAttribute("point" + i + "_y", "" + p.y);
            i++;
        }
        element.setAttribute("center_x", "" + getCenter().x);
        element.setAttribute("center_y", "" + getCenter().y);
        element.setAttribute("length", "" + getLength());
        element.setAttribute("angle", "" + getOrientation());
        element.setAttribute("width", "" + getWidth());
        element.setAttribute("signed_curv", "" + m_mean_signed_curvature);
        element.setAttribute("abs_curv", "" + m_mean_absolute_curvature);
        element.setAttribute("keep", isKeep() ? "yes" : "no");

        return element;
    }

    // Note that the coordinates of the center are scaled by a factor of 10
    // in comparison to the coordinates of the corner points.
    private static Point calcCenter(long[] partial_lengths, List<Point> points) {
        if (points.size() == 2) {
            Point a = points.get(0), b = points.get(1);
            return new Point((a.x + b.x) * 5, (a.y + b.y) * 5);
        }
        long full_length = partial_lengths[partial_lengths.length - 1];
        int i;
        for (i = 0; i < partial_lengths.length; i++) {
            if (2 * partial_lengths[i] > full_length) {
                break;
            }
        }
        Point a = points.get(i - 1), b = points.get(i);
        double ratio = (0.5 * full_length - partial_lengths[i - 1]) /
                (partial_lengths[i] - partial_lengths[i - 1]);
        return new Point((int) Math.round(10 * (a.x + ratio * (b.x - a.x))),
                (int) Math.round(10 * (a.y + ratio * (b.y - a.y))));
    }

    private static long[] calcCurvatures(List<Point> points, long length) {
        if (points.size() < 3) {
            return new long[]{0, 0};
        }
        double[][] directions = new double[points.size() - 1][2];
        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i), b = points.get(i + 1);
            double distance = a.distance(b);
            directions[i][0] = (b.x - a.x) / distance;
            directions[i][1] = (b.y - a.y) / distance;
        }
        double abs_sum = 0;
        int n = directions.length - 1;
        for (int i = 0; i < n; i++) {
            double angle = Math.acos(Math.min(1, directions[i][0] * directions[i + 1][0] +
                    directions[i][1] * directions[i + 1][1])) / Const.RAD;

            abs_sum += Math.abs(angle);
        }

        double cosine = (directions[0][0] * directions[n][0] +
                directions[0][1] * directions[n][1]),
                sine = (directions[0][0] * directions[n][1] -
                        directions[0][1] * directions[n][0]),
                angle = Math.atan2(sine, cosine) / Const.RAD,
                factor = Const.MF * Const.MF / (double) length;

        return new long[]{Math.round(factor * angle), Math.round(factor * abs_sum)};
    }

    private List<Point> straighten() {
        if (getPoints().size() < 3) {
            return getPoints();
        }

        List<Integer> split_list = new ArrayList<>();
        split_list.add(0);
        int end = getPoints().size() - 1;
        split_list = bestSplits(0, end, split_list);
        split_list.add(end);
        Collections.sort(split_list);

        List<Point> out = new ArrayList<>();
        for (int i : split_list) {
            out.add(getPoints().get(i));
        }
        return out;
    }

    private List<Integer> bestSplits(int start, int end, List<Integer> list) {
        if (maxDistance(start, end) < 1) {
            return list;
        }

        double max_length = 0;
        boolean which_end = false; // false = start, true = end
        int best_split = start;
        for (int i = start + 1; i < end; i++) {
            double dist1 = maxDistance(start, i), dist2 = maxDistance(i, end);
            if (dist1 < 1 && dist2 < 1) {
                list.add(i);
                return list;
            }
            if (dist1 < 1) {
                double length = getPoints().get(start).distanceSq(getPoints().get(i));
                if (length > max_length) {
                    best_split = i;
                    max_length = length;
                    which_end = false;
                }
            }
            if (dist2 < 1) {
                double length = getPoints().get(i).distanceSq(getPoints().get(end));
                if (length > max_length) {
                    best_split = i;
                    max_length = length;
                    which_end = true;
                }
            }
        }
        list.add(best_split);
        if (which_end) {
            return bestSplits(start, best_split, list);
        }
        return bestSplits(best_split, end, list);
    }

    private double maxDistance(int start, int end) {
        if (end < start + 2) {
            return 0;
        }
        Point p1 = getPoints().get(start), q1 = getPoints().get(end);
        double max_dist = 0;
        for (int i = start + 1; i < end; i++) {
            double dist = pointLineDistanceSquare(p1, q1, getPoints().get(i));
            if (dist > max_dist) {
                max_dist = dist;
            }
        }
        return max_dist;
    }

    @Override
    public Object clone() {
        return new CurvedFilament(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object.getClass().equals(this.getClass()))) {
            return false;
        }

        CurvedFilament fil = (CurvedFilament) object;
        if (fil.getPoints().size() != getPoints().size()) {
            return false;
        }
        for (int i = 0; i < getPoints().size(); i++) {
            if (!fil.getPoints().get(i).equals(getPoints().get(i))) {
                return false;
            }
        }
        return (isKeep() == fil.isKeep() && getWidth() == fil.getWidth() && m_mean_signed_curvature == fil.m_mean_signed_curvature &&
                m_mean_absolute_curvature == fil.m_mean_absolute_curvature && getCenter().x == fil.getCenter().x &&
                getCenter().y == fil.getCenter().y && getLength() == fil.getLength() &&
                getOrientation() % (180 * Const.M) == fil.getOrientation() % (180 * Const.M));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getCenter() == null) ? 0 : getCenter().hashCode());
        result = prime * result + (isKeep() ? 1231 : 1237);
        result = prime * result + (int) (getLength() ^ (getLength() >>> 32));
        result = prime * result + (int) (m_mean_absolute_curvature ^ (m_mean_absolute_curvature >>> 32));
        result = prime * result + (int) (m_mean_signed_curvature ^ (m_mean_signed_curvature >>> 32));
        result = prime * result + (int) (getOrientation() ^ (getOrientation() >>> 32));
        result = prime * result + ((getPoints() == null) ? 0 : getPoints().hashCode());
        result = prime * result + (int) (getWidth() ^ (getWidth() >>> 32));
        return result;
    }

}