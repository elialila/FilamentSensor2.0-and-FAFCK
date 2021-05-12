package filters;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
 *               2013-2016 Benjamin Eltzner
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
import core.FilamentSensor;

import java.awt.*;

public class PluginScoreImage {

    public PluginScoreImage() {
    }

    public static int scoreImage(int[][] image, boolean[][] mask, int sigma, int min_range) {
        final double ori_threshold = 1.1 / (Math.sqrt(2 * Math.PI) * sigma);
        final int bri_threshold = 200;
        if (mask == null) {
            return -1;
        }

        final int width = image.length,
                height = image[0].length,
                size = 3 * sigma;
        final double[][] gauss = Calc.gaussianMask(size, sigma);
        final Point[][] lines = latticeInit(size);

        double gauss_total = 0;
        for (int a = 0; a < 2 * size + 1; a++) {
            for (int b = 0; b < 2 * size + 1; b++) {
                gauss_total += gauss[a][b];
            }
        }

        int area = 0;
        double bright_count = 0,
                oriented_count = 0;
        for (int x = size; x < width - size; x++) {
            for (int y = size; y < height - size; y++) {
                if (mask[x][y]) {
                    // Gaussian weighted mean
                    double gauss_mean = 0;
                    for (int dx = -size; dx < size + 1; dx++) {
                        for (int dy = -size; dy < size + 1; dy++) {
                            gauss_mean += image[x + dx][y + dy] * gauss[size + dx][size + dy];
                        }
                    }
                    gauss_mean /= gauss_total;

                    // Maximal Gaussian weighted rod mean.
                    double max_ori_mean = 0;
                    for (int ori = 0; ori < 4 * size; ori++) {
                        double sum = image[x][y] * gauss[size][size];
                        //double gauss_sum = gauss[size][size];
                        double mean = 0.0;

                        for (int dist = 1; dist < size + 1; dist++) {
                            int dx = lines[ori][dist].x;
                            int dy = lines[ori][dist].y;

                            sum += (image[x + dx][y + dy] * gauss[size + dx][size + dy] +
                                    image[x - dx][y - dy] * gauss[size - dx][size - dy]);
                            //gauss_sum += (gauss[size+dx][size+dy] + gauss[size-dx][size-dy]);
                        }
                        mean = sum / gauss_total;

                        if (mean > max_ori_mean) {
                            max_ori_mean = mean;
                        }
                    }

                    // Calculate measures
                    area++;
                    if (max_ori_mean / gauss_mean > ori_threshold) {
                        oriented_count++;
                    }
                    if (gauss_mean > bri_threshold) {
                        bright_count++;
                    }
                }
            }
        }
        oriented_count /= (float) area;
        bright_count /= (float) area;
        return (int) Math.round(0.001 * area - 7500 * bright_count + 1500 * oriented_count);
    }

    private static Point[][] latticeInit(int radius) {
        Point lines[][] = new Point[4 * radius][radius + 1];

        FilamentSensor.debugMessage("Creating " + 4 * radius + " lines.");

        for (int direction = 0; direction < radius; direction++) {
            double slope = (double) direction / (double) radius;

            for (int x = 0; x <= radius; x++) {
                lines[direction][x] = new Point(x, (int) Math.round(slope * x));
            }
        }

        for (int direction = radius; direction < 3 * radius; direction++) {
            double epols = (2 * (double) radius - direction) / radius;
            for (int y = 0; y <= radius; y++) {
                lines[direction][y] = new Point((int) Math.round(epols * y), y);
            }
        }

        for (int direction = 3 * radius; direction < 4 * radius; direction++) {
            double slope = (direction - 4 * (double) radius) / radius;
            for (int x = 0; x <= radius; x++) {
                lines[direction][x] = new Point(x, (int) Math.round(slope * x));
            }
        }

        return lines;
    }
}
