package core.tracers;
/*
 * Implementation of the Line Tracer algorithm, modified for filament tracing.
 *
 * Relevant research articles:
 * Carsten Gottschlich, Preda Mihailescu, and Axel Munk,
 * "Robust Orientation Field Estimation and Extrapolation Using Semilocal Line Sensors",
 * IEEE Transactions on Information Forensics and Security, vol. 4, no. 4, pp. 802-811, Dec. 2009.
 * dx.doi.org/10.1109/TIFS.2009.2033219
 *
 * Copyright 2009-2010 Carsten Gottschlich
 *           2011-2013 Julian Rüger
 *           2013-2014 Benjamin Eltzner
 *
 * This class is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This class is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this class; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * or see <http://www.gnu.org/licenses/>.
 */


import core.Calc;
import core.Const;
import core.filaments.AbstractFilament;
import core.filaments.Filament;
import ij.process.ImageProcessor;
import core.image.BinaryImage;
import core.FilamentSensor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LineSensor extends AbstractTracer {
    // m_number_of_directions should be a multiple of 8.
    private static final int m_directions = 360;
    private static final int m_orientations = m_directions / 2;
    private static final int m_maximum_step = 500;
    private static final double m_tolerance = 0.3;


    private Point[][] m_lattice;

    private int m_width, m_height;
    private int m_filament_number;


    public LineSensor() {
        reset();

        if (m_circle_masks == null) {
            initCircleMasks(50);
        }

        m_width_map = null;
    }

    private void reset() {
        m_filament_number = 0;
        m_filament_list = new ArrayList<>();
        m_orientation_field = null;
    }

    public int[][] getOrientationField() {
        return m_orientation_field;
    }


    public int[][] getWidthMap() {
        return m_width_map;
    }


    /**
     * Traces filaments with width-support by wrapping LineSensor.
     */
    @Override
    public List<AbstractFilament> scanFilaments(ImageProcessor bin, double tolerance, int min_length, int min_angle, int step) {
        reset();
        // refresh map and get max_radius
        int max_diameter = calcWidthMap(bin, tolerance);

        for (int width = max_diameter; width > 0; width--) {
            FilamentSensor.debugMessage("Tracing filaments, width " + width + "... ");

            boolean[][] input = new boolean[m_width][m_height];


            // Creates boolean mask where all smaller diameters than the current diameter(width) are true.
            // Sensor interprets true as black and false as white pixels!
            for (int i = 0; i < m_width; i++) {
                for (int j = 0; j < m_height; j++) {
                    input[i][j] = (m_width_map[i][j] < width);
                }
            }

            // The lineSensor is invoked with the input mask as binary_image.
            findOrientations(input, min_length, min_angle, width);

            if (m_filament_number <= 0) {
                FilamentSensor.debugMessage("No filaments for width " + width + " found.");
                continue;
            }

            FilamentSensor.debugMessage(m_filament_number + " filaments found.");
        }
        AtomicInteger counter = new AtomicInteger(1);
        m_filament_list.forEach(f -> f.setNumber(counter.getAndIncrement()));
        return m_filament_list;
    }

    @Override
    public int calcWidthMap(ImageProcessor bin, double tolerance) {
        boolean[][] binImage = new BinaryImage(bin.getIntArray(), 254).toBoolean();
        return calcWidthMap(binImage, tolerance);
    }

    /**
     * For each white point in the binary image the width map entry
     * is the highest diameter at which the ratio of black pixels
     * to all pixels exceeds the tolerance, zero everywhere else.
     */
    public int calcWidthMap(boolean[][] bin, double tolerance) {
        int max_observed_diameter = 0;

        m_width = bin.length;
        m_height = bin[0].length;
        m_width_map = new int[m_width][m_height];

        for (int x = 0; x < m_width; x++) {
            for (int y = 0; y < m_height; y++) {
                if (bin[x][y]) // false is white. If black, m_width_map[i][j] remains 0.
                {
                    continue;
                }
                int diameter = 1, count = 0, misses = 0;
                double ratio = 0.0;

                while (ratio <= tolerance) {
                    diameter++;
                    if (diameter > max_observed_diameter) {
                        max_observed_diameter = diameter;
                    }

                    if (diameter >= m_circle_masks.length) {
                        initCircleMasks(m_circle_masks.length * 2);
                        return calcWidthMap(bin, tolerance);
                    }

                    boolean[][] mask = m_circle_masks[diameter];
                    int range = mask.length / 2;

                    for (int dx = -range; dx < range + 1; dx++) {
                        if (x - dx < 0 || x + dx > m_width - 1) {
                            continue;
                        }

                        for (int dy = -range; dy < range + 1; dy++) {
                            if (!mask[range + dx][range + dy] || y - dy < 0 || y + dy > m_height - 1) {
                                continue;
                            }
                            count++;
                            // Count true points as misses.(white is false)
                            if (bin[x + dx][y + dy]) {
                                misses++;
                            }
                        }
                    }

                    // Ratio of misses.
                    ratio = (double) misses / (double) count;
                }
                // Radius at which black pixel ratio exceeded the tolerance.
                m_width_map[x][y] = diameter - 1;
            }
        }
        return max_observed_diameter;
    }


    public static int[][] makeFingerprint(double axis, int[][] orientationField) {
        FilamentSensor.debugError("LineSensor.makeFingerprint() not implemented!");
        return new int[0][0];
    }

    /**
     * Finds Orientations and Filaments.
     * <p>
     * Initialises LineInfo's for every white pixel.
     * Creates a map of all points lying on lines whose length exceeds minimal_length.
     * The points are sorted in lists by line length.
     * Run through all remaining lines (longest first) and mark them in m_orientation_field and add the resulting filament to filament-list.
     * --> @see markLines(...)
     *
     * @param binary_image
     * @param minimal_length
     * @param minimal_angle_step
     * @param width
     */
    private void findOrientations(boolean[][] binary_image, int minimal_length, int minimal_angle_step, int width) {
        if (m_lattice == null) {
            initSpokeLattice();
        }

        m_width = binary_image.length;
        m_height = binary_image[0].length;

        initOrientationField(m_width, m_height);

        LineInfo[][] line_info = new LineInfo[m_width][m_height];

        for (int i = 0; i < m_width; i++) {//init lineInfo for each false pixel (false == white pixel)
            for (int j = 0; j < m_height; j++) {
                if (!binary_image[i][j]) {
                    line_info[i][j] = new LineInfo(binary_image, new Point(i, j));
                }
            }
        }

        FilamentSensor.debugMessage("senseLines done. ");

        // Make a map of all points lying on lines whose length exceeds
        // minimal_length. The points are sorted in lists by line length.
        HashMap<Integer, List<Point>> map = new HashMap<>();
        int maximal_length = 0;

        for (int i = 0; i < m_width; i++) {
            for (int j = 0; j < m_height; j++) {
                if (line_info[i][j] != null && m_orientation_field[i][j] == -1 &&
                        (int) (line_info[i][j].head.distance(line_info[i][j].tail)) >= minimal_length) {
                    int line_length = line_info[i][j].length;

                    map.computeIfAbsent(line_length, k -> new ArrayList<>());
                    map.get(line_length).add(new Point(i, j));

                    if (line_length > maximal_length) {
                        maximal_length = line_length;
                    }
                }
            }
        }

        // Run through the lines, longest first, and mark them.
        for (int i = maximal_length; i >= 0; i--) {
            if (!map.containsKey(i)) {
                continue;
            }
            List<Point> list = map.get(i);

            for (int j = list.size() - 1; j >= 0; j--) {
                Point point = list.get(j);
                int x = point.x, y = point.y;

                if (m_orientation_field[x][y] == -1) {
                    int old_length = line_info[x][y].length;
                    markLines(line_info[x][y], minimal_angle_step, new Point(x, y), width);
                    //orientation fields and filament-list are updated here (markLines)

                    // Shortened lines may be 3/4 the minimal length.
                    if (line_info[x][y].length < old_length &&
                            (int) (line_info[x][y].head.distance(line_info[x][y].tail)) >= 0.75 * minimal_length) {
                        map.computeIfAbsent(line_info[x][y].length, k -> new ArrayList<>());
                        map.get(line_info[x][y].length).add(new Point(x, y));
                    } else {
                        line_info[x][y] = null;
                    }
                }
            }
        }

        FilamentSensor.debugMessage("markLines done. ");
    }

    /**
     * Marks the lines found by the line sensor in the orientation field.
     * The lines are marked with diameter increased by 2 to prevent line
     * duplication.
     * Adds lines to Filament-List
     * <p>
     * THIS METHOD CAN CHANGE INPUT line_info!
     */
    private void markLines(LineInfo lineInfo, int minimal_angle_step, Point point, int width) {
        int already_present = 0;
        int length = lineInfo.length;
        int orientation = lineInfo.orientation;
        int length_in_maximum_direction = lineInfo.length_in_max_dir;
        int length_in_opposite_direction = lineInfo.length_in_opposite_dir;
        Point head = new Point(lineInfo.head);
        Point tail = new Point(lineInfo.tail);
        boolean[][] circle = m_circle_masks[width + 1];
        int range = circle.length / 2;

        // Find points on the line that are already part of another line.
        // This is determined by the presence of an orientation.
        for (int i = 0; i < length_in_maximum_direction; i++) {
            int tx = point.x + m_lattice[orientation][i].x;
            int ty = point.y + m_lattice[orientation][i].y;

            if (m_orientation_field[tx][ty] != -1) {
                int diff = angle(orientation, m_orientation_field[tx][ty]);
                if (diff < minimal_angle_step) {
                    already_present++;
                }
            }
        }

        for (int i = 0; i < length_in_opposite_direction; i++) {
            int tx = point.x + m_lattice[orientation + m_orientations][i].x;
            int ty = point.y + m_lattice[orientation + m_orientations][i].y;

            if (m_orientation_field[tx][ty] != -1) {
                int diff = angle(orientation, m_orientation_field[tx][ty]);
                if (diff < minimal_angle_step) {
                    already_present++;
                }
            }
        }

        // If pixels of the line candidate are covered by line with
        // conflicting orientation, try to shorten the line.
        if (already_present > 0) {
            int diff = -1;
            while (diff < minimal_angle_step && length_in_maximum_direction > 0) {
                int tx = point.x + m_lattice[orientation][length_in_maximum_direction - 1].x;
                int ty = point.y + m_lattice[orientation][length_in_maximum_direction - 1].y;

                if (m_orientation_field[tx][ty] == -1) {
                    break;
                }
                diff = angle(orientation, m_orientation_field[tx][ty]);
                if (diff < minimal_angle_step) {
                    length_in_maximum_direction--;
                }
            }

            diff = -1;
            while (diff < minimal_angle_step && length_in_opposite_direction > 0) {
                int tx = point.x + m_lattice[orientation + m_orientations][length_in_opposite_direction - 1].x;
                int ty = point.y + m_lattice[orientation + m_orientations][length_in_opposite_direction - 1].y;

                if (m_orientation_field[tx][ty] == -1) {
                    break;
                }
                diff = angle(orientation, m_orientation_field[tx][ty]);
                if (diff < minimal_angle_step) {
                    length_in_opposite_direction--;
                }
            }

            // If the line candidate was shortened, update line data and
            // return to treat this line later.
            int new_length = length_in_opposite_direction + length_in_maximum_direction + 1;
            if (new_length < length) {
                lineInfo.length_in_max_dir = length_in_maximum_direction;
                lineInfo.length_in_opposite_dir = length_in_opposite_direction;
                lineInfo.length = new_length;
                lineInfo.head = shift(point, orientation, length_in_maximum_direction);
                lineInfo.tail = shift(point, orientation + m_orientations,
                        length_in_opposite_direction);

                FilamentSensor.verboseMessage("Line at point (" + point.x + ", " + point.y + ") shortened from " + length + " to " + new_length);
                return;
            }
        }
        // If the line candidate did not need to or could not be
        // shortened and the ratio of it already covered by
        // another line exceeds the tolerance, it is ignored.
        if (already_present < m_tolerance * length) {
            // Mark the line in the orientation field.
            for (int x = 0; x < 2 * range + 1; x++) {
                for (int y = 0; y < 2 * range + 1; y++) {
                    if (circle[x][y]) {
                        int p_x = point.x + x - range, p_y = point.y + y - range;

                        if (p_x >= 0 && p_x < m_width && p_y >= 0 && p_y < m_height) {
                            m_orientation_field[p_x][p_y] = orientation;
                        }

                        for (int i = 0; i < length_in_maximum_direction; i++) {
                            int tx = p_x + m_lattice[orientation][i].x;
                            int ty = p_y + m_lattice[orientation][i].y;
                            if (tx >= 0 && tx < m_width && ty >= 0 && ty < m_height) {
                                m_orientation_field[tx][ty] = orientation;
                            }
                        }

                        for (int i = 0; i < length_in_opposite_direction; i++) {
                            int tx = p_x + m_lattice[orientation + m_orientations][i].x;
                            int ty = p_y + m_lattice[orientation + m_orientations][i].y;
                            if (tx >= 0 && tx < m_width && ty >= 0 && ty < m_height) {
                                m_orientation_field[tx][ty] = orientation;
                            }
                        }
                    }
                }
            }

            // Write out filament data.
            width = (width < 3 ? width : 2 * width / 3);
            m_filament_list.add(new Filament(new Point(head), new Point(tail), Const.M * width, true));
            m_filament_number++;
        }
    }

    private static int angle(int a, int b) {
        int diff = a - b;
        diff = (diff < 0 ? -diff : diff);
        diff = (diff > m_orientations / 2 ? m_orientations - diff : diff);
        return diff;
    }

    private Point shift(Point point, int direction, int length) {
        if (length > 0) {
            int x = point.x + m_lattice[direction][length - 1].x;
            int y = point.y + m_lattice[direction][length - 1].y;
            return new Point(x, y);
        }
        return new Point(point);
    }

    private void initCircleMasks(int size) {
        m_circle_masks = new boolean[size + 1][][];
        for (int i = 1; i <= size; i++) {
            m_circle_masks[i] = Calc.circleMask(i);
        }
    }

    // This method defines a "square" lattice.
    private void initSpokeLattice() {
        m_lattice = new Point[m_directions][m_maximum_step];
        final int eighth = m_directions / 8;

        for (int j = 0; j < m_maximum_step; j++) {
            m_lattice[0][j] = new Point(j + 1, 0);
            m_lattice[1 * eighth][j] = new Point(j + 1, j + 1);
            m_lattice[2 * eighth][j] = new Point(0, j + 1);
            m_lattice[3 * eighth][j] = new Point(-j - 1, j + 1);
            m_lattice[4 * eighth][j] = new Point(-j - 1, 0);
            m_lattice[5 * eighth][j] = new Point(-j - 1, -j - 1);
            m_lattice[6 * eighth][j] = new Point(0, -j - 1);
            m_lattice[7 * eighth][j] = new Point(j + 1, -j - 1);
        }

        for (int i = 1; i < eighth; i++) {

            //double slope = (double) i / eighth;
            double slope = Math.tan(Math.PI * i / ((double) m_orientations));

            for (int j = 0; j < m_maximum_step; j++) {
                int x = j + 1;
                int y = (int) (Math.round(x * slope));

                m_lattice[i][j] = new Point(x, y);

                m_lattice[(2 * eighth) - i][j] = new Point(y, x);
                m_lattice[(2 * eighth) + i][j] = new Point(-y, x);

                m_lattice[(4 * eighth) - i][j] = new Point(-x, y);
                m_lattice[(4 * eighth) + i][j] = new Point(-x, -y);

                m_lattice[(6 * eighth) - i][j] = new Point(-y, -x);
                m_lattice[(6 * eighth) + i][j] = new Point(y, -x);

                m_lattice[(8 * eighth) - i][j] = new Point(x, -y);

            }
        }
    }


    /**
     * This is a simple wrapper class around the line sensing method.
     * It serves as a convenient way to return a mixed list of objects.
     */
    private class LineInfo {
        private int length, orientation, length_in_max_dir, length_in_opposite_dir;
        private Point head, tail;

        // This method identifies the filaments. It relies on
        // m_number_of_orientations = m_number_of_directions/2.
        private LineInfo(boolean[][] binary_image, Point point) {
            m_width = binary_image.length;
            m_height = binary_image[0].length;
            boolean color = false; // false denotes white pixels.
            Point test_point = new Point(-1, -1);
            Point[] line_end_points = new Point[m_directions];

            int[] length_in_direction = new int[m_directions];
            int[] length_in_orientation = new int[m_orientations];

            for (int k = 0; k < m_directions; k++) {
                int m = 0;
                boolean go_on = true;
                line_end_points[k] = new Point(point);

                // This loop finds the last point in direction k, to which there is
                // an uninterrupted line of white pixels from (i,j).
                while (go_on && m < m_lattice[k].length) {
                    test_point.x = point.x + m_lattice[k][m].x;
                    test_point.y = point.y + m_lattice[k][m].y;

                    if (test_point.x >= 0 && test_point.x < m_width && test_point.y >= 0 && test_point.y < m_height
                            && binary_image[test_point.x][test_point.y] == color) {
                        m++;
                        line_end_points[k] = new Point(test_point);
                    } else {
                        go_on = false;
                    }
                }

                length_in_direction[k] = m;

                if (k < m_orientations) {
                    length_in_orientation[k] = m + 1; // Count starting pixel.
                } else {
                    length_in_orientation[k % m_orientations] += m;
                }
            }

            length = 0;
            orientation = 0;

            for (int k = 0; k < m_orientations; k++) {
                if (length_in_orientation[k] > length) {
                    length = length_in_orientation[k];
                    orientation = k;
                }
            }

            head = new Point(line_end_points[orientation]);
            tail = new Point(line_end_points[orientation + m_orientations]);
            length_in_max_dir = length_in_direction[orientation];
            length_in_opposite_dir = length_in_direction[orientation + m_orientations];
        }
    }

}