package core.calculation;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2014 Benjamin Eltzner
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
import core.filaments.AbstractFilament;
import core.settings.Settings;
import core.image.IBinaryImage;
import core.settings.Ori;
import core.FilamentSensor;
import util.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static core.Misc.*;

public class OrientationFields {
    private Block[][] m_blocks;
    private List<Point> m_unoriented_blocks;
    private List<AbstractFilament> m_filament_list;
    private int m_width, m_height, m_n_blocks;
    private double m_minimal_area;
    private Point m_top_left_block;

    private final int STEP, BLOCK_SIGMA, BLOCK_RANGE;
    private static final int BOUNDARY = 15,
            MAX_ORIENTATIONS = 10,
            MAX_RELAX_STEPS = 1000;
    private static final double DUMP_DRAG = 0.3,
            RELAX_SIGMA = 5.0,
            SMEAR_SIGMA = 6.0,
            MIN_COMPAT_TOLERANCE = 15.0,
            COMPAT_TOLERANCE_INCREMENT = 5.0,
            MAX_COMPAT_TOLERANCE = 25.0;
    private double compat_tolerance = 15.0;
    private double[] von_Mises = vonMisesParameters(compat_tolerance);
    private final double[][] BLOCK_MASK;


    public OrientationFields(int area, IBinaryImage mask, int[][] orientation_field, List<AbstractFilament> filaments) {
        this(area, (mask != null) ? mask.toBoolean() : null, orientation_field, filaments);
    }


    public OrientationFields(int area, boolean[][] mask, int[][] orientation_field,
                             List<AbstractFilament> filaments) {
        if (mask == null || orientation_field == null) {
            STEP = -1;
            BLOCK_SIGMA = -1;
            BLOCK_RANGE = -1;
            BLOCK_MASK = null;
            return;
        }
        m_minimal_area = 0;
        m_filament_list = filaments;
        int step_candidate = 3,
                counter = Integer.MAX_VALUE,
                width_blocks = 0,
                height_blocks = 0,
                x_boundary = 0,
                y_boundary = 0;
        m_width = mask.length;
        m_height = mask[0].length;
        while (counter > 500) {
            counter = 0;
            step_candidate += 2;
            width_blocks = (m_width - 2 * BOUNDARY) / step_candidate;
            height_blocks = (m_height - 2 * BOUNDARY) / step_candidate;
            x_boundary = (m_width - step_candidate * (width_blocks - 1) - 1) / 2;
            y_boundary = (m_height - step_candidate * (height_blocks - 1) - 1) / 2;
            for (int x = 0; x < width_blocks; x++) {
                for (int y = 0; y < height_blocks; y++) {
                    int x_position = x_boundary + x * step_candidate,
                            y_position = y_boundary + y * step_candidate;
                    if (mask[x_position][y_position]) {
                        counter++;
                    }
                }
            }
        }
        STEP = step_candidate;
        BLOCK_SIGMA = Math.max(15, 6 * STEP / 5);
        BLOCK_RANGE = 5 * BLOCK_SIGMA / 2;
        BLOCK_MASK = Calc.gaussianMask(BLOCK_RANGE, BLOCK_SIGMA);

        m_top_left_block = new Point(x_boundary, y_boundary);
        Block[][] blocks = new Block[width_blocks][height_blocks];
        m_n_blocks = 0;
        m_unoriented_blocks = new ArrayList<>();

        for (int x = 0; x < width_blocks; x++) {
            for (int y = 0; y < height_blocks; y++) {
                int x_position = x_boundary + x * STEP,
                        y_position = y_boundary + y * STEP;
                if (mask[x_position][y_position]) {
                    blocks[x][y] = new Block(x_position, y_position);
                    int orientation_count = blocks[x][y].simpleMax(orientation_field);
                    if (orientation_count == 0) {
                        blocks[x][y] = null;
                    } else {
                        m_unoriented_blocks.add(new Point(x_position, y_position));
                        m_n_blocks++;
                    }
                }
            }
        }
        m_blocks = blocks;
    }

    //write OrientationField Label into Filaments
    public Pair<Int3D, Map<Integer, List<AbstractFilament>>> label(Settings parameters) {
        if (m_blocks == null || m_blocks.length == 0) {
            return null;
        }
        FilamentSensor.debugMessage("Determine orientation fields...");
        m_minimal_area = parameters.getValue(Ori.min_area) / 1000.;
        int min_filaments = parameters.getValue(Ori.min_filaments);

        double main_mass = 0, main_size = 0;
        List<SimpleField> sorted_fields = null;
        while (main_mass < 2 / 3. && main_size < 0.85 && compat_tolerance < MAX_COMPAT_TOLERANCE) {
            sorted_fields = labelRun(min_filaments);
            compat_tolerance += COMPAT_TOLERANCE_INCREMENT;
            von_Mises = vonMisesParameters(compat_tolerance);
            main_mass = 0;
            for (int i = 1; i < sorted_fields.size(); i++) {
                main_mass += sorted_fields.get(i).getMass();
            }
            main_mass = (sorted_fields.size() > 1 ? sorted_fields.get(1).getMass() / main_mass : 1);
            main_size = (sorted_fields.size() > 1 ? sorted_fields.get(1).getArea() / (double) m_n_blocks : 1);
        }

        int[][][] out = new int[sorted_fields.size()][][];
        Map<Integer, List<AbstractFilament>> sorted_filaments = new HashMap<>();
        for (int i = 1; i < sorted_fields.size(); i++) {
            out[i] = sorted_fields.get(i).getRaw();
            sorted_filaments.put(i, sorted_fields.get(i).m_filaments);
            final int n = i;
            sorted_fields.get(i).m_filaments.forEach(filament -> filament.setOrientationField(n));
            for (int[] p : out[i]) {
                m_unoriented_blocks.remove(new Point(p[0], p[1]));
            }
            // makeImage(out[i]);
        }
        int i = 0;
        out[0] = new int[m_unoriented_blocks.size()][];
        for (Point p : m_unoriented_blocks) {
            out[0][i++] = new int[]{p.x, p.y, -1};
        }
        sorted_filaments.put(0, sorted_fields.get(0).m_filaments);
        FilamentSensor.debugMessage((out.length - 1) + " orientation fields found.");

        return new Pair<>(new Int3D(out), sorted_filaments);
    }

    private List<SimpleField> labelRun(int min_filaments) {
        if(m_blocks==null||m_blocks.length==0){return new ArrayList<>();}
        Block[][] blocks = fromTemplate();
        List<SimpleField> orientation_fields = new ArrayList<>();

        // Get orientation fields
        while (enoughLeft(blocks)) {
            List<List<Point>> list = getUpdateOrder(initialLabeling(blocks), blocks);
            initializeBlocks(list, blocks);
            relax(list, blocks);
            SimpleField tmp = extractData(blocks);
            if (tmp != null) {
                orientation_fields.add(tmp);
            }
        }

        List<SimpleField> sorted_fields = mergeFields(sortFilaments(orientation_fields,
                m_filament_list, compat_tolerance), compat_tolerance);

        if (sorted_fields != null) {
            for (int i = sorted_fields.size() - 1; i > -1; i--) {
                if (sorted_fields.get(i).m_filaments.size() < min_filaments) {
                    sorted_fields.remove(i);
                }
            }
        }

        return sortFilaments(sorted_fields, m_filament_list, compat_tolerance);
    }

    public static Pair<Int3D, Map<Integer, List<AbstractFilament>>> sortFilaments(int[][][] orientation_fields,
                                                                                  List<AbstractFilament> filaments) {
        if (orientation_fields == null || filaments == null) {
            return null;
        }
        List<SimpleField> fields = new ArrayList<>();
        for (int i = 1; i < orientation_fields.length; i++) {
            Map<Point, Integer> orientation_map = new HashMap<>();
            List<Integer> x_values = new ArrayList<>(),
                    y_values = new ArrayList<>();
            for (int[] p : orientation_fields[i]) {
                orientation_map.put(new Point(p[0], p[1]), p[2]);
                if (!x_values.contains(p[0])) {
                    x_values.add(p[0]);
                }
                if (!y_values.contains(p[1])) {
                    y_values.add(p[1]);
                }
            }
            fields.add(new SimpleField(orientation_fields[i], x_values, y_values, orientation_map, null));
        }
        List<SimpleField> tmp = sortFilaments(fields, filaments, MIN_COMPAT_TOLERANCE);
        tmp.get(0).m_raw_data = orientation_fields[0];
        int[][][] out = new int[tmp.size()][][];
        Map<Integer, List<AbstractFilament>> sorted_filaments = new HashMap<>();
        for (int i = 0; i < tmp.size(); i++) {
            out[i] = tmp.get(i).getRaw();
            sorted_filaments.put(i, tmp.get(i).m_filaments);
        }
        return new Pair<>(new Int3D(out), sorted_filaments);
    }

    private static List<SimpleField> sortFilaments(List<SimpleField> orientation_fields,
                                                   List<AbstractFilament> filaments, double tolerance) {
        if (orientation_fields == null || filaments == null) {
            return null;
        }
        if (!orientation_fields.isEmpty() && orientation_fields.get(0).m_raw_data == null) {
            orientation_fields.remove(0);
        }
        for (SimpleField field : orientation_fields) {
            field.resetFilamentList();
        }
        List<SimpleField> list = new ArrayList<>();
        final int n_fields = orientation_fields.size();
        double[] weights = new double[n_fields + 1];
        list.add(new SimpleField());
        for (int i = 0; i < n_fields; i++) {
            list.add(orientation_fields.get(i));
        }

        for (AbstractFilament filament : filaments) {
            Point center = filament.getCenter();
            double ori = filament.getOrientation() / Const.MF;

            double[] ori_diffs = new double[n_fields];
            double min_diff = Double.MAX_VALUE;
            int min_pos = -1;
            for (int i = 0; i < n_fields; i++) {
                int test_ori = orientation_fields.get(i).getOneOrientation(center);
                ori_diffs[i] = Double.MAX_VALUE;
                if (test_ori < 0) {
                    continue;
                }
                ori_diffs[i] = Math.abs(ori - test_ori);
                ori_diffs[i] = (ori_diffs[i] > 90. ? 180. - ori_diffs[i] : ori_diffs[i]);
                if (ori_diffs[i] < min_diff) {
                    min_diff = ori_diffs[i];
                    min_pos = i;
                }
            }

            boolean valid = false;
            if (min_diff < 0.5 * tolerance) {
                valid = true;
                for (int i = 0; i < n_fields; i++) {
                    if (i == min_pos) {
                        continue;
                    }
                    valid = valid && ori_diffs[i] > tolerance;
                }
            }

            if (valid) {
                FilamentSensor.verboseMessage("Found filament in first run.");
                list.get(min_pos + 1).addFilament(filament);
                weights[min_pos + 1] += filament.getMass();
                continue;
            }

            min_diff = Double.MAX_VALUE;
            min_pos = -1;
            for (int i = 0; i < n_fields; i++) {
                int[] test_oris = orientation_fields.get(i).getFourOrientations(center);
                ori_diffs[i] = Double.MAX_VALUE;
                for (int t : test_oris) {
                    if (t < 0) {
                        continue;
                    }
                    double tmp = Math.abs(ori - t);
                    tmp = (tmp > 90. ? 180. - tmp : tmp);
                    if (tmp < ori_diffs[i]) {
                        ori_diffs[i] = tmp;
                    }
                }

                if (ori_diffs[i] < min_diff) {
                    min_diff = ori_diffs[i];
                    min_pos = i;
                }
            }

            if (min_diff < tolerance) {
                FilamentSensor.verboseMessage("Found filament in second run.");
                list.get(min_pos + 1).addFilament(filament);
                weights[min_pos + 1] += filament.getMass();
                continue;
            }
            list.get(0).addFilament(filament);
        }
        return sortByWeight(list, weights);
    }

    private static List<SimpleField> sortByWeight(List<SimpleField> list, double[] weights) {
        int[] sortedIndices = new int[weights.length];
        for (int i = 1; i < sortedIndices.length; i++) {
            double max_weight = -1.0;
            int max_index = -1;
            for (int j = 1; j < weights.length; j++) {
                if (weights[j] > max_weight) {
                    max_weight = weights[j];
                    max_index = j;
                }
            }
            sortedIndices[i] = max_index;
            weights[max_index] = -1.0;
        }
        List<SimpleField> out = new ArrayList<>();
        for (int i = 0; i < sortedIndices.length; i++) {
            out.add(list.get(sortedIndices[i]));
        }
        return out;
    }

    private static List<SimpleField> mergeFields(List<SimpleField> list, double tolerance) {
        int n = list.size();
        for (int i = 1; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                SimpleField field1 = list.get(i), field2 = list.get(j);
                int overlap = 0, comparisons = 0, angle_diffs = 0, step = field2.m_step;
                int[][] oriented_points1 = field1.getRaw();
                Map<Point, Integer> oriented_points2 = field2.m_oriented_points;
                Set<Point> points2 = oriented_points2.keySet();
                for (int[] point1 : oriented_points1) {
                    for (int k = -step; k < step + 1; k += step) {
                        Point p_x = new Point(point1[0] + k, point1[1]);
                        if (points2.contains(p_x)) {
                            int tmp = Math.abs(point1[2] - oriented_points2.get(p_x));
                            angle_diffs += (tmp > 90 ? 180 - tmp : tmp);
                            comparisons++;
                            if (k == 0) {
                                overlap++;
                                continue;
                            }
                        }
                        Point p_y = new Point(point1[0], point1[1] + i);
                        if (points2.contains(p_y)) {
                            int tmp = Math.abs(point1[2] - oriented_points2.get(p_y));
                            angle_diffs += (tmp > 90 ? 180 - tmp : tmp);
                            comparisons++;
                        }
                    }
                }
                if (comparisons > 0 && angle_diffs / (double) comparisons < tolerance &&
                        20 * overlap < oriented_points1.length + oriented_points2.size()) {
                    field1.absorb(field2);
                    list.remove(field2);
                    return mergeFields(list, tolerance);
                }
            }
        }
        return list;
    }

    /**
     * Assuming a function<br>
     * (0) f(x) = C + B * exp(A * cos(2*x))<br>
     * we want<br>
     * (1) C + B * exp(A) = 1<br>
     * (2) C + B * exp(-A) = -1<br>
     * (3) C + B * exp(A * cos(2*phi)) = 0<br>
     * The first two equations yield<br>
     * (4) cosh(A) = -C/B<br>
     * which, substituted in (3) yields<br>
     * (5) 2*phi = acos(log(cosh(A)/A)) =: g(A)<br>
     * As g is a monotonous function, it can be easily numerically
     * inverted giving the inverse function h with<br>
     * (6) A = h(2*phi)<br>
     * From (1) and (4) we get<br>
     * (7) B = 1/sinh(A)<br>
     * (8) C = -1/tanh(A)<br>
     * <p>
     * This means the parameters this function returns are such as
     * to define a function f as in (0) such that
     * f(0) = 1, f(phi)=0, f(pi)=-1.
     */
    private static double[] vonMisesParameters(final double phi) {
        Function<Double, Double> angle = x -> Math.acos(Math.log(Math.cosh(x)) / x) * 180.0 / Math.PI;
        Function<Double, Double> kappa = Calc.invert(angle, 0, 1e15, false);
        double[] parameters = new double[3];
        parameters[0] = kappa.apply(2 * phi);
        parameters[1] = 1.0 / Math.sinh(parameters[0]);
        parameters[2] = -1.0 / Math.tanh(parameters[0]);
        return parameters;
    }

    private Block[][] fromTemplate() {
        final int width = m_blocks.length, height = m_blocks[0].length;
        Block[][] blocks = new Block[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (m_blocks[x][y] != null) {
                    blocks[x][y] = (Block) m_blocks[x][y].clone();
                }
            }
        }
        return blocks;
    }

    private boolean enoughLeft(Block[][] blocks) {
        if (blocks == null) {
            return false;
        }

        final int width = blocks.length,
                height = blocks[0].length;
        int counter = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (blocks[x][y] != null) {
                    counter++;
                    if (counter > m_n_blocks * m_minimal_area) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find the largest contiguous region of uniform orientation. If present,
     * treat regions with only one orientation as preferred. Fix the
     * orientation of these blocks and use them as seeds for the relaxation
     * labeling.
     *
     * @return A list of points representing blocks with fixed orientations.
     */
    private List<Point> initialLabeling(Block[][] blocks) {
        List<Point> all_points = new ArrayList<>(),
                single_mode_points = new ArrayList<>();
        final int width = blocks.length,
                height = blocks[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Block block = blocks[x][y];
                if (block != null) {
                    all_points.add(new Point(x, y));
                    if (block.orientations() == 1) {
                        single_mode_points.add(new Point(x, y));
                    }
                }
            }
        }

        single_mode_points = largestContiguousRegion(single_mode_points);
        int main_ori_single = findMainOrientation(single_mode_points, blocks);
        single_mode_points = findBestRegion(single_mode_points, main_ori_single, blocks);

        if (single_mode_points.size() >= m_minimal_area * m_n_blocks) {
            for (Point p : single_mode_points) {
                blocks[p.x][p.y].fix(main_ori_single);
            }
            return single_mode_points;
        }

        all_points = largestContiguousRegion(all_points);
        int main_ori_all = findMainOrientation(all_points, blocks);
        all_points = findBestRegion(all_points, main_ori_all, blocks);

        if (all_points.size() > single_mode_points.size()) {
            for (Point p : all_points) {
                blocks[p.x][p.y].fix(main_ori_all);
            }
            return all_points;
        } else if (!single_mode_points.isEmpty()) {
            for (Point p : single_mode_points) {
                blocks[p.x][p.y].fix(main_ori_single);
            }
            return single_mode_points;
        } else {
            /* 
             * Escape for the corner case that two neighboring blocks remain
             * whose orientations are exactly 2 * SMEAR_SIGMA apart.
             */
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    blocks[x][y] = null;
                }
            }
        }
        return all_points;
    }

    private static List<List<Point>> getUpdateOrder(List<Point> fixed, Block[][] blocks) {
        if (blocks == null || blocks.length <= 0 || blocks[0].length <= 0) {
            return null;
        }

        List<Point> all = new ArrayList<>();
        final int width = blocks.length,
                height = blocks[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (blocks[x][y] != null) {
                    all.add(new Point(x, y));
                }
            }
        }

        all.removeAll(fixed);
        List<Point> current = fixed;
        List<List<Point>> update_order = new ArrayList<>();
        while (!current.isEmpty()) {
            List<Point> to_append = new ArrayList<>();
            for (Point p : current) {
                Point[] p_neighbors = new Point[]{new Point(p.x - 1, p.y),
                        new Point(p.x + 1, p.y),
                        new Point(p.x, p.y - 1),
                        new Point(p.x, p.y + 1)};
                for (Point q : p_neighbors) {
                    if (all.remove(q)) {
                        to_append.add(q);
                    }
                }
            }
            update_order.add(to_append);
            current = to_append;
        }
        for (Point p : all) {
            blocks[p.x][p.y].freeze();
        }
        return update_order;
    }

    private static void initializeBlocks(List<List<Point>> order, Block[][] blocks) {
        if (blocks == null || blocks.length <= 0 || blocks[0].length <= 0 ||
                order == null || order.isEmpty()) {
            return;
        }

        final int range = (int) Math.ceil(3 * RELAX_SIGMA);
        double[][] gauss = Calc.gaussianMask(range, RELAX_SIGMA);

        final int width = blocks.length,
                height = blocks[0].length;
        List<Point> interior = new ArrayList<>();
        for (List<Point> current : order) {
            interior.addAll(current);
            for (Point p : current) {
                int x = p.x, y = p.y,
                        left = Math.max(-x, -range),
                        right = Math.min(width - x, range + 1),
                        top = Math.max(-y, -range),
                        bottom = Math.min(height - y, range + 1);

                List<Pair<Block, Double>> list = new ArrayList<>();
                List<Block> four_neighborhood = new ArrayList<>();
                double gauss_sum = 0.0;
                for (int dx = left; dx < right; dx++) {
                    for (int dy = top; dy < bottom; dy++) {
                        Block block = blocks[x + dx][y + dy];
                        if (block != null && (dx != 0 || dy != 0))// && interior.contains(block))
                        {
                            double tmp = gauss[range + dx][range + dy];
                            list.add(new Pair<>(block, tmp));
                            gauss_sum += tmp;
                            if ((Math.abs(dx) == 1 && dy == 0) || dx == 0 && Math.abs(dy) == 1) {
                                four_neighborhood.add(block);
                            }
                        }
                    }
                }
                for (Pair<Block, Double> pair : list) {
                    pair.setValue(pair.getValue() / gauss_sum);
                }
                blocks[x][y].populateNeighborhood(list, four_neighborhood);
            }
        }
    }

    private void relax(List<List<Point>> update_order, Block[][] blocks) {
        boolean changed = true;
        for (int i = 1; i < MAX_RELAX_STEPS && changed; i++) {
            changed = false;
            for (List<Point> sublist : update_order) {
                for (Point p : sublist) {
                    changed = blocks[p.x][p.y].update() || changed;
                }
            }
            for (List<Point> sublist : update_order) {
                for (Point p : sublist) {
                    if (!blocks[p.x][p.y].m_fixed) {
                        blocks[p.x][p.y].checkFreezeCondition();
                    }
                }
            }
        }
        for (List<Point> sublist : update_order) {
            for (Point p : sublist) {
                blocks[p.x][p.y].update();
            }
        }
    }

    private SimpleField extractData(Block[][] blocks) {
        final int width = blocks.length,
                height = blocks[0].length;

        // Make preliminary binary mask of largest contiguous region
        List<Point> oriented_points = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Block block = blocks[x][y];
                if (block != null && block.hasOrientation()) {
                    oriented_points.add(new Point(x, y));
                }
            }
        }
        oriented_points = largestContiguousRegion(oriented_points);
        if (oriented_points.size() < m_n_blocks * m_minimal_area) {
            for (Point p : oriented_points) {
                int p_x = p.x, p_y = p.y;
                Block block = blocks[p_x][p_y];
                if (block != null && block.hasOrientation()) {
                    block.getOrientation();
                    if (block.orientations() <= 0) {
                        blocks[p_x][p_y] = null;
                    }
                }
            }
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (blocks[x][y] != null) {
                        blocks[x][y].reset();
                    }
                }
            }
            return null;
        }

        boolean[][] preliminary_mask = new boolean[width][height];
        for (Point p : oriented_points) {
            preliminary_mask[p.x][p.y] = true;
        }

        // Get labeling results
        List<Int1D> points_with_orientations = new ArrayList<>();
        List<Int1D> grid_points = new ArrayList<>();
        List<Integer> x_values = new ArrayList<>(),
                y_values = new ArrayList<>();
        Map<Point, Integer> orientation_map = new HashMap<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Block block = blocks[x][y];
                if (block != null) {
                    if (preliminary_mask[x][y]) {
                        Point p = block.m_position;
                        int ori = block.getOrientation();
                        points_with_orientations.add(new Int1D(new int[]{p.x, p.y, ori}));
                        grid_points.add(new Int1D(new int[]{x, y, ori}));
                        if (!x_values.contains(p.x)) {
                            x_values.add(p.x);
                        }
                        if (!y_values.contains(p.y)) {
                            y_values.add(p.y);
                        }
                        orientation_map.put(p, ori);
                        if (block.orientations() <= 0) {
                            blocks[x][y] = null;
                        }
                    } else {
                        block.reset();
                    }
                }
            }
        }
        int[][] output = new int[points_with_orientations.size()][];
        int[][] grid_output = new int[points_with_orientations.size()][];
        for (int i = 0; i < points_with_orientations.size(); i++) {
            output[i] = points_with_orientations.get(i).the;
            grid_output[i] = grid_points.get(i).the;
        }
        return new SimpleField(output, x_values, y_values, orientation_map, grid_output);
    }

    public int[][] makeImage(int[][] block_orientations) {
        int[][] image_seed = new int[m_width][m_height];
        final int width = m_blocks.length,
                height = m_blocks[0].length;
        int x_offset = m_top_left_block.x,
                y_offset = m_top_left_block.y,
                x_min = x_offset,
                y_min = y_offset;

        while (x_min >= 0) {
            x_min -= STEP;
        }
        x_min += STEP;

        while (y_min >= 0) {
            y_min -= STEP;
        }
        y_min += STEP;

        for (int x = x_min; x < m_width; x += STEP) {
            for (int y = y_min; y < m_height; y += STEP) {
                image_seed[x][y] = -1;
            }
        }

        // Get labeling results
        x_min = y_min = Integer.MAX_VALUE;
        int x_max = 0, y_max = 0;
        for (int[] p : block_orientations) {
            int p_x = p[0], p_y = p[1];
            if (p_x < x_min) {
                x_min = p_x;
            }
            if (p_x > x_max) {
                x_max = p_x;
            }

            if (p_y < y_min) {
                y_min = p_y;
            }
            if (p_y > y_max) {
                y_max = p_y;
            }
            // Revert to the angles of the orientation field for color correspondence.
            image_seed[p_x][p_y] = (180 - p[2]) % 180;
        }

        // Make binary mask
        boolean[][] binary_mask = new boolean[m_width][m_height];
        int radius = STEP;
        int x_shift = x_offset - radius, y_shift = y_offset - radius;
        boolean[][] circle_mask = Calc.circleMask(2 * radius);
        int counter = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int x_pos = x * STEP + x_shift, y_pos = y * STEP + y_shift;
                if (image_seed[x_pos][y_pos] > -1) {
                    counter++;

                    for (int a = 0; a < 2 * radius + 1; a++) {
                        for (int b = 0; b < 2 * radius + 1; b++) {
                            binary_mask[x_pos + a][y_pos + b] = circle_mask[a][b] ||
                                    binary_mask[x_pos + a][y_pos + b];
                        }
                    }
                }
            }
        }
        binary_mask = Calc.close(binary_mask, circle_mask);
        binary_mask = Calc.erode(binary_mask, Calc.circleMask(radius));

        if (counter < m_n_blocks * m_minimal_area) {
            FilamentSensor.debugError("How did I get here?");
            return null;
        }

        // Make smooth orientation field.
        int[][] image = new int[m_width][m_height];
        int x_term = STEP - BLOCK_RANGE - x_offset,
                y_term = STEP - BLOCK_RANGE - y_offset;
        x_min = Math.max(0, x_min - BLOCK_RANGE);
        x_max = Math.min(m_width, x_max + BLOCK_RANGE);
        y_min = Math.max(0, y_min - BLOCK_RANGE);
        y_max = Math.min(m_height, y_max + BLOCK_RANGE);
        for (int x = 0; x < m_width; x++) {
            for (int y = 0; y < m_height; y++) {
                image[x][y] = -1;
            }
            if (x == x_min - 1) {
                x = x_max - 1;
            }
        }

        for (int y = 0; y < m_height; y++) {
            for (int x = 0; x < m_width; x++) {
                image[x][y] = -1;
            }
            if (y == y_min - 1) {
                y = y_max - 1;
            }
        }

        double gauss = 0.0, sum = 0.0;
        boolean above135 = false, below45 = false;
        for (int x = x_min; x < x_max; x++) {
            for (int y = y_min; y < y_max; y++) {
                if (!binary_mask[x][y]) {
                    image[x][y] = -1;
                    continue;
                }

                int left = Math.max(0, ((x + x_term) / STEP) * STEP) + x_offset - x,
                        right = Math.min(BLOCK_RANGE, m_width - x),
                        top = Math.max(0, ((y + y_term) / STEP) * STEP) + y_offset - y,
                        bottom = Math.min(BLOCK_RANGE, m_height - y);
                gauss = sum = 0.0;
                for (int dx = left; dx < right; dx += STEP) {
                    for (int dy = top; dy < bottom; dy += STEP) {
                        if (image_seed[x + dx][y + dy] > -1) {
                            above135 = above135 || (image_seed[x + dx][y + dy] > 135);
                            below45 = below45 || (image_seed[x + dx][y + dy] < 45);
                            gauss += image_seed[x + dx][y + dy] * BLOCK_MASK[BLOCK_RANGE + dx][BLOCK_RANGE + dy];
                            sum += BLOCK_MASK[BLOCK_RANGE + dx][BLOCK_RANGE + dy];
                        }
                    }
                }

                if (sum <= 0) {
                    image[x][y] = -1;
                } else if (above135 && below45) {
                    gauss = sum = 0.0;
                    for (int dx = left; dx < right; dx += STEP) {
                        for (int dy = top; dy < bottom; dy += STEP) {
                            if (image_seed[x + dx][y + dy] > -1) {
                                int tmp = (image_seed[x + dx][y + dy] + 90) % 180;
                                gauss += tmp * BLOCK_MASK[BLOCK_RANGE + dx][BLOCK_RANGE + dy];
                                sum += BLOCK_MASK[BLOCK_RANGE + dx][BLOCK_RANGE + dy];
                            }
                        }
                    }
                    image[x][y] = ((int) Math.round(gauss / sum) + 90) % 180;
                } else {
                    image[x][y] = (int) Math.round(gauss / sum);
                }
            }
        }

        return image;
    }

    int[] fieldShape(int[][] block_orientations, int[][] orientation_field, boolean[][] shape) {
        final int width = orientation_field.length,
                height = orientation_field[0].length;
        int[][] image_seed = new int[width][height];
        int x_offset = m_top_left_block.x,
                y_offset = m_top_left_block.y,
                x_min = x_offset,
                y_min = y_offset;

        while (x_min >= STEP) {
            x_min -= STEP;
        }

        while (y_min >= STEP) {
            y_min -= STEP;
        }

        for (int x = x_min; x < m_width; x += STEP) {
            for (int y = y_min; y < m_height; y += STEP) {
                image_seed[x][y] = -1;
            }
        }

        // Get labeling results
        x_min = Integer.MAX_VALUE;
        y_min = Integer.MAX_VALUE;
        int x_max = 0, y_max = 0;
        for (int[] p : block_orientations) {
            int p_x = p[0], p_y = p[1];
            if (p_x < x_min) {
                x_min = p_x;
            }
            if (p_x > x_max) {
                x_max = p_x;
            }

            if (p_y < y_min) {
                y_min = p_y;
            }
            if (p_y > y_max) {
                y_max = p_y;
            }
            image_seed[p_x][p_y] = (180 - p[2]) % 180;
        }

        int[][] partial_orientation_field = new int[width][height];

        for (int x = 0; x < m_width; x++) {
            for (int y = 0; y < m_height; y++) {
                partial_orientation_field[x][y] = -1;
            }
        }
        int x_lo = (x_min > STEP ? x_min - STEP : x_min),
                x_hi = (x_max < width - STEP - 1 ? x_max + STEP : x_max) + 1,
                y_lo = (y_min > STEP ? y_min - STEP : y_min),
                y_hi = (y_max < height - STEP - 1 ? y_max + STEP : y_max) + 1;
        int x_block_lo = x_lo, x_block_hi = x_min + 1;

        for (int x = x_lo; x < x_hi; x++) {
            if (x >= x_block_hi && x_block_hi < width - STEP - 1) {
                x_block_hi += STEP;
            }
            if (x > x_block_lo + STEP) {
                x_block_lo += STEP;
            }

            int y_block_lo = y_lo, y_block_hi = y_min + 1;

            for (int y = y_lo; y < y_hi; y++) {
                if (orientation_field[x][y] < 0 || !shape[x][y]) {
                    continue;
                }

                if (y >= y_block_hi && y_block_hi < height - STEP - 1) {
                    y_block_hi += STEP;
                }
                if (y > y_block_lo + STEP) {
                    y_block_lo += STEP;
                }

                int ori = orientation_field[x][y];
                for (int x_block = x_block_lo; x_block < x_block_hi; x_block += STEP) {
                    for (int y_block = y_block_lo; y_block < y_block_hi; y_block += STEP) {
                        if (image_seed[x_block][y_block] < 0) {
                            continue;
                        }
                        int diff = Math.abs(ori - image_seed[x_block][y_block]);
                        if (diff <= compat_tolerance || 180 - diff <= compat_tolerance) {
                            partial_orientation_field[x][y] = ori;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<Point> largestContiguousRegion(List<Point> list) {
        List<List<Point>> contiguous_regions = new ArrayList<>();
        while (!list.isEmpty()) {
            List<Point> region = new ArrayList<>();
            region.add(list.remove(0));
            int old_length = 0;
            while (region.size() > old_length) {
                old_length = region.size();
                List<Point> to_add = new ArrayList<>();
                for (Point p : list) {
                    for (Point q : region) {
                        if ((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y) <= 1) {
                            if (!to_add.contains(p)) {
                                to_add.add(p);
                            }
                        }
                    }
                }
                list.removeAll(to_add);
                region.addAll(to_add);
            }
            contiguous_regions.add(region);
        }

        List<Point> largest_contiguous_region = new ArrayList<>();
        int max_size = 0;
        for (List<Point> region : contiguous_regions) {
            if (region.size() > max_size) {
                max_size = region.size();
                largest_contiguous_region = region;
            }
        }
        return largest_contiguous_region;
    }

    /**
     * Find main mode in largest contiguous region of blocks
     * and fix all blocks with orientation in main mode.
     */
    private int findMainOrientation(List<Point> all_points, Block[][] blocks) {
        final int smear_range = (int) Math.round(4 * SMEAR_SIGMA);
        double[] histogram = new double[180 + 2 * smear_range];

        for (Point p : all_points) {
            for (int ori : blocks[p.x][p.y].m_orientations)
                histogram[ori + smear_range]++;
        }
        for (int i = 0; i < smear_range; i++) {
            histogram[i] = histogram[i + 180];
            histogram[histogram.length - 1 - i] = histogram[histogram.length - 181 - i];
        }

        double[] gaussian = Calc.gaussian1d(smear_range, SMEAR_SIGMA);
        double[] smeared_histogram = new double[180];
        double max = 0.0;
        int max_orientation = -1;
        for (int i = 0; i < 180; i++) {
            for (int j = 0; j < 2 * smear_range + 1; j++) {
                smeared_histogram[i] += histogram[i + j] * gaussian[j];
            }
            if (smeared_histogram[i] > max) {
                max = smeared_histogram[i];
                max_orientation = i;
            }
        }
        return max_orientation;
    }

    private List<Point> findBestRegion(List<Point> all_points, int max_orientation, Block[][] blocks) {
        List<Point> points_to_fix = new ArrayList<>();
        for (int i = 0; i <= SMEAR_SIGMA && points_to_fix.size() < m_minimal_area * m_n_blocks; i++) {
            for (Point p : all_points) {
                if (blocks[p.x][p.y].contains(max_orientation, i) && !points_to_fix.contains(p)) {
                    points_to_fix.add(p);
                }
            }
            points_to_fix = largestContiguousRegion(points_to_fix);
        }

        return points_to_fix;
    }

    BufferedImage illustrateOrientations(Block[][] blocks) {
        if (blocks == null || blocks.length <= 0 || blocks[0].length <= 0) {
            return null;
        }

        final int width = blocks.length,
                height = blocks[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (blocks[i][j] == null) {
                    raster.setSample(i, j, 0, 0);
                } else {
                    raster.setSample(i, j, 0, 25 * blocks[i][j].orientations());
                }
            }
        }
        return image;
    }

    private static class SimpleField {
        private int[][] m_raw_data;
        private Map<Point, Integer> m_oriented_points;
        private List<Integer> m_x_values, m_y_values;
        private List<AbstractFilament> m_filaments;
        private int m_step;

        private SimpleField(int[][] raw_data, List<Integer> x, List<Integer> y,
                            Map<Point, Integer> p, int[][] grid_points) {
            m_raw_data = raw_data;
            Collections.sort(x);
            Collections.sort(y);
            m_x_values = x;
            m_y_values = y;
            m_oriented_points = p;
            m_step = -1;
            m_filaments = new ArrayList<>();
            if (x.size() > 1) {
                m_step = x.get(1) - x.get(0);
            } else if (y.size() > 1) {
                m_step = y.get(1) - y.get(0);
            }
        }

        private SimpleField() {
            m_filaments = new ArrayList<>();
        }

        private void absorb(SimpleField other) {
            Map<Point, Integer> other_points = other.m_oriented_points;
            for (Point key : other_points.keySet()) {
                if (m_oriented_points.containsKey(key)) {
                    int phi1 = m_oriented_points.get(key), phi2 = other_points.get(key),
                            tmp = (phi1 + phi2) / 2;
                    tmp = (Math.abs(phi1 - phi2) > 90 ? (tmp + 90) % 180 : tmp);
                    m_oriented_points.put(key, tmp);
                }
                m_oriented_points.put(key, other_points.get(key));
            }
            int[][] new_raw = new int[m_oriented_points.size()][];
            int i = 0;
            for (Point p : m_oriented_points.keySet()) {
                new_raw[i++] = new int[]{p.x, p.y, m_oriented_points.get(p)};
                if (!m_x_values.contains(p.x)) {
                    m_x_values.add(p.x);
                }
                if (!m_y_values.contains(p.y)) {
                    m_y_values.add(p.y);
                }
            }
            m_raw_data = new_raw;
            m_filaments.addAll(other.m_filaments);
        }

        private void addFilament(AbstractFilament filament) {
            m_filaments.add(filament);
        }

        private int[][] getRaw() {
            return m_raw_data;
        }

        private int getOneOrientation(Point p) {
            Point q = nearestPoint(p);
            if (q == null) {
                return -1;
            }
            Integer tmp = m_oriented_points.get(q);
            return (tmp == null ? -1 : tmp);
        }

        private int[] getFourOrientations(Point p) {
            Point q = nearestPoint(p);
            if (q == null) {
                return new int[]{-1, -1, -1, -1};
            }
            int x_step = (q.x * 10 > p.x ? -m_step : m_step),
                    y_step = (q.y * 10 > p.y ? -m_step : m_step);
            int[] out = new int[4];
            int i = 0;
            for (int x : new int[]{q.x, q.x + x_step}) {
                for (int y : new int[]{q.y, q.y + y_step}) {
                    Integer tmp = m_oriented_points.get(new Point(x, y));
                    out[i++] = (tmp == null ? -1 : tmp);
                }
            }
            return out;
        }

        private Point nearestPoint(Point p) {
            int x_dist = Integer.MAX_VALUE,
                    y_dist = Integer.MAX_VALUE,
                    x_best = -1, y_best = -1;

            for (int x : m_x_values) {
                int tmp = Math.abs(p.x - x * 10);
                if (tmp < x_dist) {
                    x_dist = tmp;
                    x_best = x;
                }
            }
            for (int y : m_y_values) {
                int tmp = Math.abs(p.y - y * 10);
                if (tmp < y_dist) {
                    y_dist = tmp;
                    y_best = y;
                }
            }
            if (x_dist > m_step * 10 || y_dist > m_step * 10) {
                return null;
            }

            return new Point(x_best, y_best);
        }

        private void resetFilamentList() {
            m_filaments = new ArrayList<>();
        }

        private double getMass() {
            double mass = 0.0;
            for (AbstractFilament filament : m_filaments) {
                mass += filament.getMass();
            }
            return mass;
        }

        private int getArea() {
            return m_raw_data.length;
        }
    }

    private class Block {
        private Point m_position;
        private int[] m_orientations;
        private double[] m_label_probabilities;
        private List<Pair<Block, Double2D>> m_neighbors;
        private List<Block> m_four_neighborhood;
        private boolean m_fixed, m_may_freeze;

        private Block(Point position) {
            m_position = position;
            m_fixed = false;
            m_may_freeze = false;
        }

        private Block(int x, int y) {
            m_position = new Point(x, y);
        }

        /**
         * Exit status signals number of significant maxima.
         */
        private int simpleMax(int[][] orientation_field) {
            final int width = orientation_field.length,
                    height = orientation_field[0].length;

            // Make histogram and count points.
            final int x = m_position.x,
                    y = m_position.y,
                    left = -Math.min(x, BLOCK_RANGE), right = Math.min(width - x, BLOCK_RANGE + 1),
                    top = -Math.min(y, BLOCK_RANGE), bottom = Math.min(height - y, BLOCK_RANGE + 1);
            final int smear_range = (int) Math.round(4 * SMEAR_SIGMA);
            double[] histogram = new double[180 + 2 * smear_range];
            for (int dx = left; dx < right; dx++) {
                for (int dy = top; dy < bottom; dy++) {
                    int ori = orientation_field[x + dx][y + dy];
                    if (ori > -1) {
                        double weight = BLOCK_MASK[BLOCK_RANGE + dx][BLOCK_RANGE + dy];
                        histogram[smear_range + ori] += weight;
                    }
                }
            }
            for (int i = 0; i < smear_range; i++) {
                histogram[i] = histogram[i + 180];
                histogram[histogram.length - 1 - i] = histogram[histogram.length - 181 - i];
            }

            double[] gaussian = Calc.gaussian1d(smear_range, SMEAR_SIGMA);
            double[] smeared_histogram = new double[182];
            //double max = 0.0;
            double mean = 0.0;
            for (int i = 0; i < 180; i++) {
                for (int j = 0; j < 2 * smear_range + 1; j++) {
                    smeared_histogram[i + 1] += histogram[i + j] * gaussian[j];
                }
                mean += smeared_histogram[i + 1];
                /*if (smeared_histogram[i+1] > max)
                {
                    max = smeared_histogram[i+1];
                }*/
            }
            mean /= 180;
            smeared_histogram[0] = smeared_histogram[180];
            smeared_histogram[181] = smeared_histogram[1];

            List<Integer> maxima = new ArrayList<>();
            for (int i = 0; i < 180; i++) {
                //if (smeared_histogram[i+1] > max * MAXIMA_SCALE &&
                if (smeared_histogram[i + 1] > mean &&
                        smeared_histogram[i + 1] > smeared_histogram[i] &&
                        smeared_histogram[i + 1] >= smeared_histogram[i + 2]) {
                    maxima.add(i);
                }
            }

            // Get maxima
            m_orientations = new int[maxima.size()];
            for (int n = 0; n < maxima.size(); n++) {
                m_orientations[n] = (180 - maxima.get(n)) % 180;
            }
            Arrays.sort(m_orientations);

            if (m_orientations.length > MAX_ORIENTATIONS) {
                FilamentSensor.debugMessage(Arrays.toString(m_orientations) + " --> ");
                Map<Double, Integer> map = new HashMap<>();
                double[] masses = new double[m_orientations.length];
                for (int i = 0; i < m_orientations.length; i++) {
                    int ori = m_orientations[i];
                    masses[i] = smeared_histogram[ori + 1];
                    map.put(masses[i], ori);
                }
                Arrays.sort(masses);
                int[] tmp = new int[MAX_ORIENTATIONS];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = map.get(masses[i]);
                }
                m_orientations = tmp;
                FilamentSensor.debugMessage(Arrays.toString(m_orientations));
            }
            return m_orientations.length;
        }

        //Avoid memory pollution!
        private void populateNeighborhood(List<Pair<Block, Double>> neighbors, List<Block> four) {
            if (m_fixed) {
                return;
            }

            m_four_neighborhood = four;
            final int n_this = orientations();

            if (m_label_probabilities == null) {
                m_label_probabilities = new double[n_this + 1];
                for (int i = 0; i < n_this + 1; i++) {
                    m_label_probabilities[i] = 1.0 / (double) (n_this + 1.0);
                }
            }

            m_neighbors = new ArrayList<>();

            for (Pair<Block, Double> pair : neighbors) {
                Block that = pair.getKey();
                int n_that = that.orientations();

                double weight = pair.getValue();
                int[] that_orientations = that.m_orientations;
                double[][] influence = new double[n_this + 1][n_that + 1];

                for (int i = 0; i < n_this; i++) {
                    for (int j = 0; j < n_that; j++) {
                        influence[i][j] = (von_Mises[2] + von_Mises[1] *
                                Math.exp(von_Mises[0] * Math.cos(2 *
                                        (m_orientations[i] - that_orientations[j]) *
                                        Const.RAD))) * weight;
                    }
                    influence[n_this][n_that] = DUMP_DRAG * weight;
                }

                m_neighbors.add(new Pair<>(that, new Double2D(influence)));
            }
        }

        private boolean update() {
            if (m_fixed) {
                return false;
            }

            double probability_sum = 0.0;
            final int n = m_label_probabilities.length;
            double[] new_probabilities = new double[n];
            for (int i = 0; i < n; i++) {
                double factor = 1.0;
                for (Pair<Block, Double2D> pair : m_neighbors) {
                    double[] that_probabilities = pair.getKey().m_label_probabilities;
                    double[][] influence = pair.getValue().the;
                    for (int j = 0; j < that_probabilities.length; j++) {
                        factor += influence[i][j] * that_probabilities[j];
                    }
                }
                new_probabilities[i] = m_label_probabilities[i] * factor;
                probability_sum += new_probabilities[i];
            }

            boolean changed = false;
            double maximal_probability = 0.0;
            int n_max = -1;
            for (int i = 0; i < n; i++) {
                new_probabilities[i] /= probability_sum;
                if (Double.isNaN(new_probabilities[i]) ||
                        Double.isInfinite(new_probabilities[i])) {
                    FilamentSensor.debugMessage("Reset probabilities at " + m_position.toString());
                    new_probabilities[i] = 1.0 / (double) new_probabilities.length;
                }

                if (new_probabilities[i] > maximal_probability) {
                    maximal_probability = new_probabilities[i];
                    n_max = i;
                }
                changed = changed || Math.abs(1 - new_probabilities[i] /
                        m_label_probabilities[i]) > Const.EPS;
            }
            if (m_may_freeze && maximal_probability > 0.999) {
                for (int i = 0; i < new_probabilities.length; i++) {
                    new_probabilities[i] = 0.0;
                }
                new_probabilities[n_max] = 1.0;
                FilamentSensor.verboseMessage("Block at " + m_position.toString() + " with " +
                        orientations() + " orientations is done.");
                m_fixed = true;
            }
            m_label_probabilities = new_probabilities;
            return changed;
        }

        private boolean contains(int orientation, int tolerance) {
            for (int ori : m_orientations) {
                int diff = Math.abs(orientation - ori);
                if (diff <= tolerance || 180 - diff <= tolerance) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasOrientation() {
            double max = 0.0;
            int index = -1;
            final int n = orientations();
            for (int i = 0; i < n + 1; i++) {
                if (m_label_probabilities[i] > max) {
                    max = m_label_probabilities[i];
                    index = i;
                }
            }

            return (index != n);
        }

        private int getOrientation() {
            double max = 0.0;
            int index = -1;
            final int n = orientations();
            for (int i = 0; i < n + 1; i++) {
                if (m_label_probabilities[i] > max) {
                    max = m_label_probabilities[i];
                    index = i;
                }
            }

            reset();

            if (index == n) {
                return -1;
            }

            int out = m_orientations[index];
            if (n == 1) {
                m_orientations = null;
                return out;
            }

            int[] new_orientations = new int[n - 1];
            for (int i = 0; i < n; i++) {
                if (i < index) {
                    new_orientations[i] = m_orientations[i];
                }
                if (i > index) {
                    new_orientations[i - 1] = m_orientations[i];
                }
            }
            m_orientations = new_orientations;
            return out;
        }

        private int orientations() {
            if (m_orientations == null) {
                return 0;
            }
            return m_orientations.length;
        }

        private void fix(int orientation) {
            m_label_probabilities = new double[orientations() + 1];
            int best_fit = -1,
                    min_diff = Integer.MAX_VALUE;
            for (int i = 0; i < m_orientations.length; i++) {
                int diff = Math.abs(orientation - m_orientations[i]);
                diff = (diff > 90 ? 180 - diff : diff);
                if (diff < min_diff) {
                    min_diff = diff;
                    best_fit = i;
                }
            }
            m_label_probabilities[best_fit] = 1.0;
            m_fixed = true;
        }

        private void freeze() {
            int n = orientations();
            m_label_probabilities = new double[n + 1];
            m_label_probabilities[n] = 1.0;
            m_fixed = true;
        }

        private void checkFreezeCondition() {
            for (Block neighbor : m_four_neighborhood) {
                m_may_freeze = m_may_freeze || neighbor.m_fixed;
            }
        }

        private void reset() {
            m_neighbors = null;
            m_label_probabilities = null;
            m_fixed = false;
        }

        @Override
        public String toString() {
            return "Block[position: (" + m_position.x + "," + m_position.y +
                    "), orientations: " + Arrays.toString(m_orientations) +
                    ", probabilities: " + Arrays.toString(m_label_probabilities);
        }

        /**
         * Returns a duplicate block at the same position with the same orientations.
         * All other properties are set to their defaults.
         */
        @Override
        protected Object clone() {
            Block other = new Block((Point) m_position.clone());
            int[] oris = new int[m_orientations.length];
            for (int i = 0; i < m_orientations.length; i++) {
                oris[i] = m_orientations[i];
            }
            other.m_orientations = oris;
            return other;
        }
    }

}
