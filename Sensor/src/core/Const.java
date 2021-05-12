package core;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2013-2014 Benjamin Eltzner
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

import core.calculation.WrappedSiZer;
import enums.ImageTypes;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Const {
    private Const() {
    } // No instances

    // Scale for angles, length and width of filaments and filament chains
    public static final int M = 1000000;
    public static final double MF = M;
    public static final int THUMBNAIL_SIZE = 200;
    public static final double RAD = Math.PI / 180.0;
    public static final int CONTRAST_FACTOR = 14000;
    public static final int SCROLL = 16;
    public static final double EPS = 1e-14;    // Epsilon for double comparison.
    public static final String ls = System.lineSeparator();


    public static double truncateDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return value;
        return Math.floor(value * ((double) M)) / ((double) M);
    }

    public static final double[][][] CIRCLE =
            {
                    {{0}},

                    {{1}},

                    {{79, 457, 79},
                            {457, 1000, 457},
                            {79, 457, 79}},

                    {{545, 972, 545},
                            {972, 1000, 972},
                            {545, 972, 545}},

                    {{0, 214, 479, 214, 0},
                            {214, 985, 1000, 985, 214},
                            {479, 1000, 1000, 1000, 479},
                            {214, 985, 1000, 985, 214},
                            {0, 214, 479, 214, 0}},

                    {{137, 769, 983, 769, 137},
                            {769, 1000, 1000, 1000, 769},
                            {983, 1000, 1000, 1000, 983},
                            {769, 1000, 1000, 1000, 769},
                            {137, 769, 983, 769, 137}},

                    {{0, 8, 312, 486, 312, 8, 0},
                            {8, 693, 1000, 1000, 1000, 693, 8},
                            {312, 1000, 1000, 1000, 1000, 1000, 312},
                            {486, 1000, 1000, 1000, 1000, 1000, 486},
                            {312, 1000, 1000, 1000, 1000, 1000, 312},
                            {8, 693, 1000, 1000, 1000, 693, 8},
                            {0, 8, 312, 486, 312, 8, 0}},

                    {{0, 352, 841, 988, 841, 352, 0},
                            {352, 999, 1000, 1000, 1000, 999, 352},
                            {841, 1000, 1000, 1000, 1000, 1000, 841},
                            {988, 1000, 1000, 1000, 1000, 1000, 988},
                            {841, 1000, 1000, 1000, 1000, 1000, 841},
                            {352, 999, 1000, 1000, 1000, 999, 352},
                            {0, 352, 841, 988, 841, 352, 0}},

                    {{0, 0, 48, 361, 490, 361, 48, 0, 0},
                            {0, 208, 900, 1000, 1000, 1000, 900, 208, 0},
                            {48, 900, 1000, 1000, 1000, 1000, 1000, 900, 48},
                            {361, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 361},
                            {490, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 490},
                            {361, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 361},
                            {48, 900, 1000, 1000, 1000, 1000, 1000, 900, 48},
                            {0, 208, 900, 1000, 1000, 1000, 900, 208, 0},
                            {0, 0, 48, 361, 490, 361, 48, 0, 0}},

                    {{0, 41, 518, 877, 991, 877, 518, 41, 0},
                            {41, 790, 1000, 1000, 1000, 1000, 1000, 790, 41},
                            {518, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 518},
                            {877, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 877},
                            {991, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 991},
                            {877, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 877},
                            {518, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 518},
                            {41, 790, 1000, 1000, 1000, 1000, 1000, 790, 41},
                            {0, 41, 518, 877, 991, 877, 518, 41, 0}},

                    {{0, 0, 0, 98, 390, 492, 390, 98, 0, 0, 0},
                            {0, 3, 484, 974, 1000, 1000, 1000, 974, 484, 3, 0},
                            {0, 484, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 484, 0},
                            {98, 974, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 974, 98},
                            {390, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 390},
                            {492, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 492},
                            {390, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 390},
                            {98, 974, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 974, 98},
                            {0, 484, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 484, 0},
                            {0, 3, 484, 974, 1000, 1000, 1000, 974, 484, 3, 0},
                            {0, 0, 0, 98, 390, 492, 390, 98, 0, 0, 0}}
            };


    public static Map<String, Integer> makeWizerMap() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("Lines as samples", WrappedSiZer.LINES);
        map.put("Experimental samples", WrappedSiZer.EXPERIMENTAL);
        map.put("Pixels as samples", WrappedSiZer.PIXELS);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Color> makeColorMap() {
        HashMap<String, Color> map = new HashMap<String, Color>();
        map.put("Orange", Color.orange);
        map.put("Dark Orange", new Color(255, 140, 0));
        map.put("Cyan", Color.cyan);
        map.put("Blue", Color.blue);
        map.put("White", Color.white);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, ImageTypes> makeImageMap() {
        Map<String, ImageTypes> map = new HashMap<>();
        map.put("Original", ImageTypes.ORIGINAL);

        map.put("Preprocessed", ImageTypes.PREPROCESSED);
        //map.put("eLoG orientations", DataImage.ELOG_COLOR);
        //map.put("eLoG greylevels", DataImage.ELOG_GREY);
        map.put("Binarized", ImageTypes.BINARY);
        map.put("Line Sensor orientations", ImageTypes.ORIENTATIONMAP);
        map.put("Interior and Boundary", ImageTypes.REDGREEN);
        map.put("Fingerprint", ImageTypes.FINGERPRINT);
        map.put("Empty", ImageTypes.EMPTY);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Integer> makeNeighborHoodMap() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("4 neighbor", 0);
        map.put("8 neighbor", 1);
        return map;
    }

}
