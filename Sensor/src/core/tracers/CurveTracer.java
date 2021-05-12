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
 *           2013-2015 Benjamin Eltzner
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
import core.filaments.AbstractFilament;
import ij.process.ImageProcessor;
import core.image.BinaryImage;
import core.FilamentSensor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class CurveTracer extends AbstractTracer {
    // m_directions should be a multiple of 8.
    private static final int m_direction_step = 3;
    private static final int m_directions = 360 / m_direction_step;
    private static final int m_orientations = m_directions / 2;
    //    private static final int m_sense_depth = 20;
//    private static final int m_step_size = 10;
    private static final double m_tolerance = 0.3;

    //former static ...
    private Point[][] m_lattice_sense, m_lattice_step;
    private double[][] m_gauss_mask;


    private int m_max_width;

    public CurveTracer() {
        if (m_circle_masks == null) {
            initCircleMasks(50);
        }
    }

    private void reset() {
        m_filament_list = new ArrayList<>();
        m_orientation_field = null;
        m_lattice_sense = null;
        m_lattice_step = null;
    }

    public int[][] getOrientationField() {
        return m_orientation_field;
    }


    public int[][] getWidthMap() {
        return m_width_map;
    }


    @Override
    public List<AbstractFilament> scanFilaments(ImageProcessor bin, double tolerance, int min_length, int min_angle, int step) {
        boolean[][] binImage = new BinaryImage(bin.getIntArray(), 254).toBoolean();

        reset();
        initSpokeLattices(step, Math.max(2 * step, 20));//low performance impact
        // refresh map and get max_radius
        Map<Integer, List<Point>> width_list = getWidthMap(binImage, tolerance);//low performance impact 100ms > x > 0ms
        final int x_size = bin.getWidth(), y_size = bin.getHeight();
        // TODO: Mask sigma = 0.5 * step?

        m_gauss_mask = Calc.gaussianQuarterMask((3 * m_max_width + 1) / 2, 0.5 * m_max_width);

        initOrientationField(x_size, y_size);

        Map<Integer, List<Gatherer>> scored_lines = new HashMap<>();
        AtomicInteger max_score = new AtomicInteger(0);
        AtomicInteger line_counter = new AtomicInteger(0);
        FilamentSensor.debugMessage("Start tracing, max width: " + m_max_width);

        for (int width = m_max_width; width > 0; width--) {
            FilamentSensor.debugMessage("Tracing filaments, width " + width + "... ");
            List<Point> points = width_list.get(width);//Map.get no impact
            if (points != null) {
                //performance impact due to high number of points
                //higher performance impact 550ms > x > 2ms depending on loops
                scanFilamentsHandlePoints(points, width, scored_lines, max_score, line_counter, min_length, min_angle, binImage);
                FilamentSensor.debugMessage("CurveTracer.scanFilamentsHandlePoints,Number Points:" + points.size());
            }
        }
        markLines(scored_lines, max_score.get(), min_length, min_angle, binImage);//low impact ~45ms on test system

        AtomicInteger counter = new AtomicInteger(1);
        m_filament_list.forEach(f -> f.setNumber(counter.getAndIncrement()));


        return m_filament_list;
    }

    @Override
    public int calcWidthMap(ImageProcessor bin, double tolerance) {
        boolean[][] binImage = new BinaryImage(bin.getIntArray(), 254).toBoolean();
        return calcWidthMap(binImage, tolerance);
    }


    private int[] scanFilamentsInitScores(Point p, int width, Gatherer[] gatherers) {
        int[] scores = new int[m_directions];
        for (int dir = 0; dir < m_directions; dir++) {
            gatherers[dir] = new Gatherer(p.x, p.y, width, dir, m_orientations, m_directions, m_direction_step, m_lattice_sense, m_lattice_step);
            scores[dir] = gatherers[dir].sense(m_width_map);
        }
        return scores;
    }


    private int[] scanFilamentsGetBestPair(int[] scores) {
        // Identify highest scoring pair of directions.
        int[] best_pair = null;
        int highscore = 0;
        // Opposite directions
        for (int i = 0; i < m_orientations; i++) {
            int score = scores[i] + scores[i + m_orientations];
            if (score > highscore) {
                highscore = score;
                best_pair = new int[]{i, i + m_orientations};
            }
        }
        // Direction change in start point.
        for (int i = 0; i < m_directions; i++) {
            int j = (i + m_orientations - 1) % m_directions;
            int score = scores[i] + scores[j];
            if (score > highscore) {
                highscore = score;
                best_pair = new int[]{i, j};
            }
        }
        return best_pair;
    }


    /**
     * Takes the Gatherer's of best_pair, collect points on curve(gather) and join them to one curve
     *
     * @param gatherers
     * @param best_pair
     * @return
     */
    private Gatherer scanFilamentsGetGathererLine(Gatherer[] gatherers, int[] best_pair) {
        Gatherer first = gatherers[best_pair[0]];
        Gatherer second = gatherers[best_pair[1]];
        first.gather(m_width_map);
        second.gather(m_width_map);
        first.join(second);
        return first;
        /*Gatherer[] tmpGatherers = new Gatherer[]{gatherers[best_pair[0]], gatherers[best_pair[1]]};
        for (int i : new int[]{0, 1}) {
            tmpGatherers[i].gather(m_width_map);
        }
        Gatherer line = tmpGatherers[0];
        line.join(tmpGatherers[1]);
        return line;*/
    }

    /**
     * @param points
     * @param width
     * @param scored_lines
     * @param max_score
     * @param line_counter
     * @param min_length
     * @param min_angle
     * @param bin
     */
    private void scanFilamentsHandlePoints(List<Point> points, int width, Map<Integer, List<Gatherer>> scored_lines, AtomicInteger max_score, AtomicInteger line_counter, int min_length, int min_angle, boolean[][] bin) {

        points.stream().forEach(p -> {
            if (m_orientation_field[p.x][p.y] > -1) {
                return;
            }
            // Test lines for all directions.
            Gatherer[] gatherers = new Gatherer[m_directions];
            int[] scores = scanFilamentsInitScores(p, width, gatherers);
            // Identify highest scoring pair of directions.
            int[] best_pair = scanFilamentsGetBestPair(scores);
            // Gather line by following best initial orientations.
            Gatherer line = scanFilamentsGetGathererLine(gatherers, best_pair);
            gatherers = null;

            // Add line to map.
            int score = line.score();
            scored_lines.computeIfAbsent(score, k -> new ArrayList<>());
            if (score > max_score.get()) {
                max_score.set(score);
            }
            scored_lines.get(score).add(line);
            if (line_counter.incrementAndGet() >= 10000) {
                FilamentSensor.debugMessage(line_counter + " lines. Maximal score: " + max_score);
                line_counter.set(cleanUpLines(scored_lines, max_score.get(), min_length, min_angle, bin));
                for (int m = max_score.get(); m >= 0; m--) {
                    if (scored_lines.get(m) != null) {
                        max_score.set(m);
                        m = 0;//loop is canceled without using break;
                    }
                }
                FilamentSensor.debugMessage(line_counter + " lines. Maximal score: " + max_score);
            }

        });


        /*for (Point p : points) {
            if (m_orientation_field[p.x][p.y] > -1) {
                continue;
            }
            // Test lines for all directions.
            Gatherer[] gatherers = new Gatherer[m_directions];
            int[] scores=scanFilamentsInitScores(p,width,gatherers);
            // Identify highest scoring pair of directions.
            int[] best_pair = scanFilamentsGetBestPair(scores);
            // Gather line by following best initial orientations.
            Gatherer line=scanFilamentsGetGathererLine(gatherers,best_pair);
            gatherers=null;

            // Add line to map.
            int score = line.score();
            if (!scored_lines.containsKey(score)) {
                scored_lines.put(score, new ArrayList<>());
            }
            if (score > max_score.get()) {
                max_score.set(score);
            }
            scored_lines.get(score).add(line);
            if (line_counter.incrementAndGet() >= 10000) {
                FilamentSensor.debugMessage(line_counter + " lines. Maximal score: " + max_score);
                line_counter.set(cleanUpLines(scored_lines, max_score.get(), min_length, min_angle, bin));
                for (int m = max_score.get(); m >= 0; m--) {
                    if (scored_lines.get(m) != null) {
                        max_score.set(m);
                        m = 0;//loop is canceled without using break;
                    }
                }
                FilamentSensor.debugMessage(line_counter + " lines. Maximal score: " + max_score);
            }
        }*/


    }


    public int calcWidthMap(boolean[][] bin, double tolerance) {
        return Collections.max(getWidthMap(bin, tolerance).keySet());
    }


    /**
     * For each white point in the binary image the width map entry
     * is the highest diameter at which the ratio of black pixels
     * to all pixels exceeds the tolerance, zero everywhere else.
     */
    private Map<Integer, List<Point>> getWidthMap(boolean[][] bin, double tolerance) {
        m_max_width = 0;
        int x_size = bin.length, y_size = bin[0].length;
        m_width_map = new int[x_size][y_size];
        Map<Integer, List<Point>> width_list = new HashMap<>();

        for (int x = 0; x < x_size; x++) {
            for (int y = 0; y < y_size; y++) {
                if (!bin[x][y]) { // false is white. If black, m_width_map[i][j] remains 0.
                    int diameter = 1, count = 0, misses = 0;
                    double ratio = 0.0;

                    while (ratio <= tolerance) {
                        diameter++;
                        if (diameter > m_max_width) {
                            m_max_width = diameter;
                        }

                        if (diameter >= m_circle_masks.length) {
                            System.out.println("RWCurveTracer --- recursive call");
                            initCircleMasks(m_circle_masks.length * 2);
                            return getWidthMap(bin, tolerance);
                        }

                        boolean[][] mask = m_circle_masks[diameter];
                        int range = mask.length / 2;

                        for (int dx = -range; dx < range + 1; dx++) {
                            if (!(x + dx < 0 || x + dx > x_size - 1)) {
                                for (int dy = -range; dy < range + 1; dy++) {
                                    if (!(!mask[range + dx][range + dy] || y + dy < 0 || y + dy > y_size - 1)) {
                                        count++;
                                        // Count white points as misses.
                                        if (bin[x + dx][y + dy]) {
                                            misses++;
                                        }
                                    }
                                }
                            }
                        }
                        // Ratio of misses.
                        ratio = (double) misses / (double) count;
                    }
                    // Radius at which black pixel ratio exceeded the tolerance.
                    m_width_map[x][y] = diameter - 1;
                    width_list.computeIfAbsent(diameter - 1, k -> new ArrayList<>());
                    width_list.get(diameter - 1).add(new Point(x, y));
                }
            }
        }
        return width_list;
    }

    public static int[][] makeFingerprint(double axis, int[][] orientationField) {
        if (orientationField == null) {
            return null;
        }
        // Make List of pixels with orientation
        List<int[]> points = new ArrayList<>();
        for (int x = 0; x < orientationField.length; x++) {
            for (int y = 0; y < orientationField[0].length; y++) {
                if (orientationField[x][y] >= 0) {
                    points.add(new int[]{x, y, orientationField[x][y]});
                }
            }
        }
        // Make fingerprint image
        int[][] fingerprint = new int[101][91];
        for (int[] p : points) {
            for (int[] q : points) {
                if (q.equals(p)) {
                    break;
                }
                int length = (int) Math.round(100 * Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) +
                        (p[1] - q[1]) * (p[1] - q[1])) / axis);
                if (length > 100) {
                    continue;
                }
                int ori_diff = Math.abs(90 - Math.abs(p[2] - q[2]));
                fingerprint[length][ori_diff]++;
            }
        }
        return fingerprint;
    }

    private void markLines(Map<Integer, List<Gatherer>> scored_lines, int max_score, int min_length, int min_angle, boolean[][] binary_image) {
        // Run through the lines, longest first, and mark them.
        for (int score = max_score; score >= 0; score--) {
            if (scored_lines.containsKey(score)) {

                List<Gatherer> list = scored_lines.get(score);

                for (int j = list.size() - 1; j >= 0; j--) {
                    Gatherer line = list.get(j);

                    int old_length = line.length();
                    if (old_length >= min_length) {
                        drawLine(line, min_angle, binary_image, true);
                    }
                    // Shortened lines may be 3/4 the minimal length.
                    if (line.length() < old_length && line.length() >= 0.75 * min_length) {
                        int new_score = line.score();
                        scored_lines.computeIfAbsent(new_score, k -> new ArrayList<>());
                        scored_lines.get(new_score).add(line);
                    } else {
                        line = null;
                    }
                }
            }
        }
    }

    private int cleanUpLines(Map<Integer, List<Gatherer>> scored_lines, int max_score,
                             int min_length, int min_angle, boolean[][] binary_image) {
        // Run through the longest lines and mark them.
        if (!scored_lines.containsKey(max_score)) {
            return -1;
        }
        List<Gatherer> list = scored_lines.remove(max_score);

        for (int j = list.size() - 1; j >= 0; j--) {
            Gatherer line = list.remove(j);

            int old_length = line.length();
            if (old_length >= min_length) {
                drawLine(line, min_angle, binary_image, true);
            }
            if (line.length() < old_length) {
                // Draw lines that were only shortened by zero-pixels.
                if (line.score() == max_score) {
                    drawLine(line, min_angle, binary_image, true);
                }
                // Really shortened lines may be 3/4 the minimal length.
                else if (line.length() < old_length && line.length() >= 0.75 * min_length) {
                    int new_score = line.score();
                    scored_lines.computeIfAbsent(new_score, k -> new ArrayList<>());
                    scored_lines.get(new_score).add(line);
                }
            }
        }

        // Cleanup lists of shorter lines.
        int line_counter = 0;
        for (int score = max_score - 1; score >= 0; score--) {
            if (!scored_lines.containsKey(score)) {
                continue;
            }
            list = scored_lines.remove(score);

            for (int j = list.size() - 1; j >= 0; j--) {
                Gatherer line = list.remove(j);

                int old_length = line.length();
                if (old_length >= min_length) {
                    drawLine(line, min_angle, binary_image, false);
                }

                // Shortened lines may be 3/4 the minimal length.
                if (line.length() >= 0.75 * min_length) {
                    int new_score = line.score();
                    scored_lines.computeIfAbsent(new_score, k -> new ArrayList<>());
                    scored_lines.get(new_score).add(line);
                    line_counter++;
                }
            }
        }
        return line_counter;
    }

    private void drawLine(Gatherer line, int min_angle, boolean[][] binary_image, boolean draw) {
        List<int[]> points = line.points();
        final boolean xcross = line.xcross();

        // Shorten line from both ends.
        boolean shortened = false;
        while (!points.isEmpty()) {
            int[] p0 = points.get(0);
            if (m_orientation_field[p0[0]][p0[1]] < 0 && p0[2] > 0) {
                break;
            }
            line.remove(0);
            shortened = true;
        }
        while (!points.isEmpty()) {
            int end = points.size() - 1;
            int[] p_end = points.get(end);
            if (m_orientation_field[p_end[0]][p_end[1]] < 0 && p_end[2] > 0) {
                break;
            }
            line.remove(end);
            shortened = true;
        }

        // Return if line was shortened.
        if (shortened) {
            line.rescore();
            return;
        }

        // Check that less than m_tolerance of the line is already covered.
        // FIXME: Restrict fraction of line that may be covered by non-conflicting orientations?
        int already_present = 0;
        for (int[] p : line.points()) {
            int ori = (p[3] * m_direction_step) % 180,
                    pre_ori = m_orientation_field[p[0]][p[1]];
            if (pre_ori != -1) {
                if (angle(ori, pre_ori) < min_angle) {
                    already_present++;
                }
            }
        }
        if (already_present > m_tolerance * line.length()) {
            line.abandon();
            return;
        }

        // Return if we are only on a shortening run.
        if (!draw) {
            line.rescore();
            return;
        }

        // Make binary line mask using point positions, widths and binary image.
        int x_min = Integer.MAX_VALUE, x_max = 0, y_min = Integer.MAX_VALUE, y_max = 0, w_max = 0;
        for (int[] p : line.points()) {
            if (p[0] < x_min) {
                x_min = p[0];
            }
            if (p[0] > x_max) {
                x_max = p[0];
            }
            if (p[1] < y_min) {
                y_min = p[1];
            }
            if (p[1] > y_max) {
                y_max = p[1];
            }
            if (p[2] > w_max) {
                w_max = p[2];
            }
        }
        x_min -= w_max;
        x_max += w_max;
        y_min -= w_max;
        y_max += w_max;

        boolean[][] canvas = new boolean[x_max - x_min + 1][y_max - y_min + 1];
        for (int[] p : line.points()) {
            if (p[2] == 0) {
                continue;
            }
            boolean[][] circle = m_circle_masks[p[2] + 1];
            int shift = circle.length / 2;
            int dx1 = p[0] - shift, dx2 = dx1 - x_min, dy1 = p[1] - shift, dy2 = dy1 - y_min;
            for (int x = 0; x < circle.length; x++) {
                for (int y = 0; y < circle.length; y++) {
                    if (x + dx2 < canvas.length && y + dy2 < canvas[0].length &&
                            x + dx1 < binary_image.length && y + dy1 < binary_image[0].length &&
                            x + dx2 >= 0 && y + dy2 >= 0 && x + dx1 >= 0 && y + dy1 >= 0)
                        canvas[x + dx2][y + dy2] = canvas[x + dx2][y + dy2] ||
                                circle[x][y] && !binary_image[x + dx1][y + dy1];
                }
            }
        }

        // Make line mask with orientations via Gaussian weighted mean over centerline pixels.
        int[][] ori_canvas = new int[x_max - x_min + 1][y_max - y_min + 1];

        int shift = m_gauss_mask.length - 1;
        for (int x = 0; x < canvas.length; x++) {
            for (int y = 0; y < canvas[x].length; y++) {
                if (canvas[x][y]) {
                    int qx = x + x_min, qy = y + y_min, turn = m_orientations / 2;
                    double gauss_sum = 0, sum = 0;

                    if (xcross) {
                        for (int[] p : line.points()) {
                            int dx = Math.abs(qx - p[0]), dy = Math.abs(qy - p[1]);
                            if (dx > shift || dy > shift) {
                                continue;
                            }
                            gauss_sum += m_gauss_mask[dx][dy] * ((p[3] + turn) % m_orientations);
                            sum += m_gauss_mask[dx][dy];
                        }
                        ori_canvas[x][y] = ((int) Math.round((gauss_sum / sum + m_orientations - turn) *
                                m_direction_step) % 180) + 1;
                    } else {
                        for (int[] p : line.points()) {
                            int dx = Math.abs(qx - p[0]), dy = Math.abs(qy - p[1]);
                            if (dx > shift || dy > shift) {
                                continue;
                            }
                            gauss_sum += m_gauss_mask[dx][dy] * p[3];
                            sum += m_gauss_mask[dx][dy];
                        }
                        ori_canvas[x][y] = ((int) Math.round(gauss_sum * m_direction_step /
                                sum) % 180) + 1;
                    }
                }
            }
        }

        // Draw line mask to orientation field.
        for (int x = 0; x < ori_canvas.length; x++) {
            for (int y = 0; y < ori_canvas[x].length; y++) {
                if (ori_canvas[x][y] > 0) {
                    m_orientation_field[x + x_min][y + y_min] = ori_canvas[x][y] - 1;
                }
                ori_canvas[x][y] -= 1;
            }
        }
        m_filament_list.add(line.asFilament());
    }

    private static int angle(int a, int b) {
        int diff = a - b;
        diff = (diff < 0 ? -diff : diff);
        diff = (diff > 90 ? 180 - diff : diff);
        return diff;
    }

    private void initCircleMasks(int size) {
        m_circle_masks = new boolean[size + 1][][];
        for (int i = 1; i <= size; i++) {
            m_circle_masks[i] = Calc.circleMask(i);
        }
    }

    // This method defines a "square" lattice.
    private void initSpokeLattices(int step_size, int sense_depth) {
        Point[][] pre_lattice = new Point[m_directions][sense_depth];

        final int eighth = m_directions / 8;

        for (int j = 0; j < sense_depth; j++) {
            pre_lattice[0][j] = new Point(j + 1, 0);
            pre_lattice[2 * eighth][j] = new Point(0, j + 1);
            pre_lattice[4 * eighth][j] = new Point(-j - 1, 0);
            pre_lattice[6 * eighth][j] = new Point(0, -j - 1);

            pre_lattice[1 * eighth][j] = new Point(j + 1, j + 1);
            pre_lattice[3 * eighth][j] = new Point(-j - 1, j + 1);
            pre_lattice[5 * eighth][j] = new Point(-j - 1, -j - 1);
            pre_lattice[7 * eighth][j] = new Point(j + 1, -j - 1);
        }

        for (int i = 1; i < eighth; i++) {
            double slope = Math.tan(Math.PI * i / ((double) m_orientations));
            for (int j = 0; j < sense_depth; j++) {
                int x = j + 1, y = (int) (Math.round(x * slope));
                pre_lattice[i][j] = new Point(x, y);
                pre_lattice[(2 * eighth) - i][j] = new Point(y, x);
                pre_lattice[(2 * eighth) + i][j] = new Point(-y, x);
                pre_lattice[(4 * eighth) - i][j] = new Point(-x, y);
                pre_lattice[(4 * eighth) + i][j] = new Point(-x, -y);
                pre_lattice[(6 * eighth) - i][j] = new Point(-y, -x);
                pre_lattice[(6 * eighth) + i][j] = new Point(y, -x);
                pre_lattice[(8 * eighth) - i][j] = new Point(x, -y);
            }
        }

        Point[][][] lattices = new Point[2][m_directions][];
        for (int i = 0; i < 2; i++) {
            int depth = (i == 0 ? sense_depth : step_size);
            for (int j = 0; j < m_directions; j++) {
                List<Point> ray = new ArrayList<>();
                double old_distance = 0, distance = 0;
                for (int k = 0; k < sense_depth; k++) {
                    Point p = pre_lattice[j][k];
                    distance = Math.sqrt(p.x * p.x + p.y * p.y);
                    if (Math.abs(distance - depth) > Math.abs(old_distance - depth)) {
                        break;
                    }
                    old_distance = distance;

                    ray.add(p);
                }
                lattices[i][j] = ray.toArray(new Point[0]);
            }
        }
        m_lattice_sense = lattices[0];
        m_lattice_step = lattices[1];
    }

}