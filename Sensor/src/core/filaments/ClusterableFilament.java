package core.filaments;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2016 Benjamin Eltzner
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

import core.Const;
import util.NotImplementedException;

import java.awt.*;
import java.util.List;

public class ClusterableFilament extends CurvedFilament implements Clusterable {
    private int time;
    private double medianOrientation, spreadOrientation;

    public ClusterableFilament() {
        super();
    }

    public ClusterableFilament(SingleFilament filament, int time) {
        super(filament);
        orientationRange();
        this.time = time;
    }

    public SingleFilament toSingleFilament() {
        return (SingleFilament) this;
    }

    @Override
    public long distanceSquare(Clusterable that) {
        if (that == null || !that.getClass().equals(this.getClass())) {
            return Long.MAX_VALUE;
        }
        ClusterableFilament other = (ClusterableFilament) that;
        // Quick escape for large time difference or equal time.
        if (Math.abs(time - other.time) > 10 || time == other.time) {
            return Long.MAX_VALUE;
        }
        // Quick escape for large orientation difference.
        double diff = Math.abs(medianOrientation - other.medianOrientation);
        diff = (diff > 90.0 ? 180.0 - diff : diff);
        if (diff > 45 + spreadOrientation + other.spreadOrientation) {
            return Long.MAX_VALUE;
        }
        // TODO: Quick escape for large point distances.
        // TODO: Calculate line distance.
        return 0;
    }

    @Override
    public int label() throws NotImplementedException {
        return 0;
    }

    private void orientationRange() {
        double previous_ori = 0.0, max_ori = Double.NEGATIVE_INFINITY,
                min_ori = Double.POSITIVE_INFINITY;
        List<Point> points = getPoints();

        for (int i = 0; i < points.size() - 1; i++) {
            Point a = points.get(i), b = points.get(i + 1);
            double distance = a.distance(b), dx = (b.x - a.x) / distance,
                    dy = -(b.y - a.y) / distance, ori = Math.atan2(dy, dx) / Const.RAD;
            ori = (360.0 + ori) % 180.0;
            if (i > 0) {
                if (ori > previous_ori + 45) {
                    ori -= 180.0;
                }
                if (ori < previous_ori - 45) {
                    ori += 180.0;
                }
            }

            if (ori > max_ori) {
                max_ori = ori;
            }
            if (ori < min_ori) {
                min_ori = ori;
            }
            previous_ori = ori;
        }
        medianOrientation = 0.5 * (max_ori + min_ori);
        spreadOrientation = (max_ori - medianOrientation);
        medianOrientation = (medianOrientation + 180.0) % 180.0;
    }

    @Override
    public Clusterable clone() {
        //currently not used
        return null;
    }


    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getMedianOrientation() {
        return medianOrientation;
    }

    public void setMedianOrientation(double medianOrientation) {
        this.medianOrientation = medianOrientation;
    }

    public double getSpreadOrientation() {
        return spreadOrientation;
    }

    public void setSpreadOrientation(double spreadOrientation) {
        this.spreadOrientation = spreadOrientation;
    }
}
