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


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class SingleFilament extends AbstractFilament {


    protected List<Point> points;



    public SingleFilament() {
        super();
        points = new ArrayList<>();
    }

    public SingleFilament(boolean keep) {
        this();
        keepProperty().set(keep);
    }


    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public List<Point> getPoints() {
        return points;
    }


    public abstract ClusterableFilament makeClusterable(int time);


    public long getSignedCurvature() {
        return 0;
    }

    public long getAbsoluteCurvature() {
        return 0;
    }









}

    