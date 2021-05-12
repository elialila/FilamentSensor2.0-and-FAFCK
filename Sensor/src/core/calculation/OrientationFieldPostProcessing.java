package core.calculation;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 * 
 * Copyright (C) 2013-2015 Patricia Burger, Benjamin Eltzner
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
import core.FilamentSensor;

import java.awt.*;
import java.util.*;
import java.util.List;

import static core.Misc.addAllUnique;
import static core.Misc.addUnique;

public class OrientationFieldPostProcessing {

    private static final int MAX_SQUARE_DIST = 1000,
            MAX_SKEW = 45,
            SCORE_OFFSET = 20 * Const.M;
    private int[][][] m_out_set1, m_out_set2;

    public OrientationFieldPostProcessing() {
    }

    public int[][][] get(boolean two) {
        return (two ? m_out_set2 : m_out_set1);
    }

    public boolean sort(int[][][] set1, int[][][] set2) {
        m_out_set1 = null;
        m_out_set2 = null;
        if (set1 == null || set2 == null) {
            return false;
        }

        List<Joint> joints = new ArrayList<>();

        for (int i = 1; i < set1.length; i++) {
            for (int[] block : set1[i]) {
                joints.addAll(getJointCandidates(block, i, set2));
            }
        }

        long[][][] tmp = countMatches(joints, set1.length, set2.length);
        long[][] match_matrix = tmp[0];

        if (match_matrix.length == 0) {
            return false;
        }


        double[][] score_matrix = new double[match_matrix.length][match_matrix[0].length];
        for (int i = 1; i < set1.length; i++) {
            for (int j = 1; j < set2.length; j++) {
                if (match_matrix[i][j] > 0) {
                    score_matrix[i][j] = tmp[1][i][j] / (double) match_matrix[i][j];
                    match_matrix[i][0] += match_matrix[i][j];
                    match_matrix[0][j] += match_matrix[i][j];
                }
            }
        }
        Map<Integer, List<Integer>> matches1 = new HashMap<>(), matches2 = new HashMap<>();
        for (int i = 1; i < set1.length; i++) {
            if (2 * match_matrix[i][0] > set1[i].length) {
                int best_match = 0, best_score = 0;
                for (int j = 1; j < set2.length; j++) {
                    if (match_matrix[i][j] > best_score) {
                        best_score = (int) match_matrix[i][j];
                        best_match = j;
                    }
                }
                addIf(matches1, i, best_match, set2, score_matrix, false);
                addIf(matches2, best_match, i, set1, score_matrix, true);
            }
        }
        for (int j = 1; j < set2.length; j++) {
            if (2 * match_matrix[0][j] > set2[j].length) {
                int best_match = 0, best_score = 0;
                for (int i = 1; i < set1.length; i++) {
                    if (match_matrix[i][j] > best_score) {
                        best_score = (int) match_matrix[i][j];
                        best_match = i;
                    }
                }
                addIf(matches1, best_match, j, set2, score_matrix, false);
                addIf(matches2, j, best_match, set1, score_matrix, true);
            }
        }
        List<List<Integer>> all_sets1 = joinLists(unmap(matches2)),
                all_sets2 = joinLists(unmap(matches1));
        return makeOutput(all_sets1, all_sets2, set1, set2);
    }

    /**
     * Count fields matches across images.
     */
    private static long[][][] countMatches(List<Joint> joints, int n_fields1, int n_fields2) {
        Map<Long, List<Joint>> scored_joints = new HashMap<>();

        for (Joint joint : joints) {
            long score = joint.score();

            if (scored_joints.get(score) == null) {
                scored_joints.put(score, new ArrayList<Joint>());
            }

            scored_joints.get(score).add(joint);
        }

        List<Long> scores = new ArrayList<>(scored_joints.keySet());
        Collections.sort(scores);
        List<int[]> joined1 = new ArrayList<>(),
                joined2 = new ArrayList<>();
        Map<int[], joinInfo> joined_info1 = new HashMap<>(),
                joined_info2 = new HashMap<>();

        long[][][] matches = new long[2][n_fields1][n_fields2];

        for (Long score : scores) {
            List<Joint> to_join = scored_joints.get(score);

            for (Joint joint : to_join) {
                int[] block1 = joint.block1(), block2 = joint.block2();
                if (joined1.contains(block1)) {
                    // Only allow multiple matches, if they score similarly.
                    if (joined_info1.get(block1).m_fields.contains(joint.field2()) ||
                            joint.score() > joined_info1.get(block1).m_min_score + SCORE_OFFSET) {
                        continue;
                    }
                    matches[0][joint.field1()][joint.field2()]++;
                    matches[1][joint.field1()][joint.field2()] += joint.score();
                    addUnique(joined1, block1);
                    addUnique(joined2, block2);
                    joined_info1.get(block1).add(joint.field2());
                    if (joined_info2.containsKey(block2)) {
                        joined_info2.get(block2).add(joint.field1());
                    } else {
                        joined_info2.put(block2, new joinInfo(joint.score(), joint.field1()));
                    }
                    continue;
                }
                if (joined2.contains(block2)) {
                    if (joined_info2.get(block2).m_fields.contains(joint.field1()) ||
                            joint.score() > joined_info2.get(block2).m_min_score + SCORE_OFFSET) {
                        continue;
                    }
                    matches[0][joint.field1()][joint.field2()]++;
                    matches[1][joint.field1()][joint.field2()] += joint.score();
                    addUnique(joined1, block1);
                    addUnique(joined2, block2);
                    if (joined_info1.containsKey(block1)) {
                        joined_info1.get(block1).add(joint.field2());
                    } else {
                        joined_info1.put(block1, new joinInfo(joint.score(), joint.field2()));
                    }
                    joined_info2.get(block2).add(joint.field1());
                    continue;
                }

                matches[0][joint.field1()][joint.field2()]++;
                matches[1][joint.field1()][joint.field2()] += joint.score();
                addUnique(joined1, block1);
                addUnique(joined2, block2);
                joined_info1.put(block1, new joinInfo(joint.score(), joint.field2()));
                joined_info2.put(block2, new joinInfo(joint.score(), joint.field1()));
            }
        }

        return matches;
    }

    /**
     * Returns joint candidates for a reference block. Candidate joints
     * differ in orientation by at most MAX_SKEW and the distance
     * square between joined block's centers is smaller than
     * MAX_SQUARE_DIST.
     */
    private static List<Joint> getJointCandidates(int[] block1, int field1, int[][][] set2) {
        List<Joint> joint_candidates = new ArrayList<Joint>();

        for (int j = 1; j < set2.length; j++) {
            for (int[] block2 : set2[j]) {
                int ori_diff = Math.abs(block1[2] - block2[2]);
                ori_diff = (ori_diff > 90 ? 180 - ori_diff : ori_diff);
                if (ori_diff > MAX_SKEW) {
                    continue;
                }
                int dx = block1[0] - block2[0], dy = block1[1] - block2[1];
                double dist_square = dx * dx + dy * dy;
                if (dist_square > MAX_SQUARE_DIST) {
                    continue;
                }
                joint_candidates.add(new Joint(block1, field1, block2, j, ori_diff, dist_square));
            }
        }

        return joint_candidates;
    }

    private boolean makeOutput(List<List<Integer>> all_sets1, List<List<Integer>> all_sets2,
                               int[][][] set1, int[][][] set2) {
        if (all_sets1.isEmpty() && all_sets2.isEmpty()) {
            m_out_set1 = set1;
            m_out_set2 = set2;
            return false;
        }

        m_out_set1 = joinAllFields(all_sets1, set1);
        m_out_set2 = joinAllFields(all_sets2, set2);
        return (m_out_set1.length != set1.length || m_out_set2.length != set2.length);
    }

    /**
     * This method may return a set that is equal to the input set.
     * Otherwise, the output set contains fewer fields than the input set.
     */
    private static int[][][] joinAllFields(List<List<Integer>> all_sets, int[][][] set) {
        List<int[][]> tmp_set = new ArrayList<>();
        List<Integer> joined = new ArrayList<>();
        for (int i = 0; i < set.length; i++) {
            if (joined.contains(i)) {
                continue;
            }
            List<int[][]> to_join = new ArrayList<>();
            for (List<Integer> list : all_sets) {
                if (list.contains(i)) {
                    joined.addAll(list);
                    for (int a : list) {
                        to_join.add(set[a]);
                    }
                    FilamentSensor.debugMessage("Joining fields " + list);
                    List<int[][]> new_fields = joinFields(to_join);
                    FilamentSensor.debugMessage("Done!");
                    tmp_set.addAll(new_fields);
                    break;
                }
            }
            if (to_join.isEmpty()) {
                tmp_set.add(set[i]);
            }
        }
        int[][][] output_set = new int[tmp_set.size()][][];
        for (int i = 0; i < output_set.length; i++) {
            output_set[i] = tmp_set.get(i);
        }
        return output_set;
    }

    /**
     * Fields are only joined, if orientations are compatible.
     */
    private static List<int[][]> joinFields(List<int[][]> to_join) {
        // Join only two fields at a time.
        boolean go_on = true;
        while (go_on && to_join.size() > 2) {
            go_on = false;
            for (int i = 0; i < to_join.size(); i++) {
                int[][] field2 = to_join.get(i);
                for (int j = 0; j < i; j++) {
                    int[][] field1 = to_join.get(j);
                    List<int[][]> pair = new ArrayList<>();
                    pair.add(field1);
                    pair.add(field2);
                    pair = joinFields(pair);
                    if (pair.size() < 2) {
                        to_join.remove(field1);
                        to_join.remove(field2);
                        to_join.addAll(pair);
                        go_on = true;
                        break;
                    }
                }
                if (go_on) {
                    break;
                }
            }
        }

        // If we are done joining fields and more than two remain, return them.
        if (to_join.size() != 2) {
            return to_join;
        }

        // From this point on, to_join contains exactly two fields.
        Map<Point, List<Integer>> point_list = new HashMap<>();

        boolean overlap = false;
        for (int[][] field : to_join) {
            for (int[] p : field) {
                Point point = new Point(p[0], p[1]);
                if (!point_list.containsKey(point)) {
                    point_list.put(point, new ArrayList<Integer>());
                } else {
                    overlap = true;
                    // Forbid more than MAX_SKEW orientation difference of fields to join.
                    int orientation_diff = Math.abs(point_list.get(point).get(0) - p[2]);
                    if (orientation_diff > MAX_SKEW && orientation_diff < 180 - MAX_SKEW) {
                        FilamentSensor.debugMessage("Not joining: Incompatible orientations.");
                        return to_join;
                    }
                }
                point_list.get(point).add(p[2]);
            }
        }
        if (!overlap) {
            FilamentSensor.debugMessage("Not joining: Disjoint fields.");
            return to_join;
        }

        int[][] new_field = new int[point_list.size()][3];
        int i = 0;


        for (Point p : point_list.keySet()) {
            List<Integer> orientations = point_list.get(p);
            if (orientations.size() == 1) {
                new_field[i++] = new int[]{p.x, p.y, orientations.get(0)};
                continue;
            }
            int ori0 = orientations.get(0), sum = 0;
            boolean try_again = false;
            for (int ori : orientations) {
                if (Math.abs(ori0 - ori) > 90) {
                    try_again = true;
                    break;
                }
                sum += ori;
            }
            if (!try_again) {
                new_field[i++] = new int[]{p.x, p.y, sum / orientations.size()};
                continue;
            }
            sum = 0;
            for (int ori : orientations) {
                sum += (ori + 90) % 180;
            }
            new_field[i++] = new int[]{p.x, p.y, (sum / orientations.size() + 90) % 180};
        }
        List<int[][]> output = new ArrayList<>();
        output.add(new_field);
        return output;
    }

    private static <S, T> List<List<T>> unmap(Map<S, List<T>> map) {
        List<List<T>> list = new ArrayList<>();
        list.addAll(map.values());
        return list;
    }

    private static <T> List<List<T>> joinLists(List<List<T>> list) {
        for (List<T> sublist : list) {
            if (sublist.size() <= 1) {
                list.remove(sublist);
                return joinLists(list);
            }
            for (T item : sublist) {
                for (List<T> other : list) {
                    if (other.equals(sublist)) {
                        continue;
                    }
                    if (other.contains(item)) {
                        list.remove(other);
                        list.remove(sublist);
                        addAllUnique(sublist, other);
                        addUnique(list, sublist);
                        return joinLists(list);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Check if fields to be joined overlap by more than 50%.
     * Scores are only considered in case of large overlap.
     */
    private static boolean addIf(Map<Integer, List<Integer>> map, int index_this, int index_other,
                                 int[][][] other_fields, double[][] scores, boolean row) {
        if (!map.containsKey(index_this)) {
            map.put(index_this, new ArrayList<Integer>());
            return addUnique(map.get(index_this), index_other);
        }
        if (map.get(index_this).contains(index_other)) {
            return false;
        }
        List<Integer> kicked = new ArrayList<>();
        for (int a : map.get(index_this)) {
            // Count boxes where fields overlap
            int counter = 0;
            for (int[] p : other_fields[index_other]) {
                for (int[] q : other_fields[a]) {
                    if (p[0] == q[0] && p[1] == q[1]) {
                        counter++;
                        break;
                    }
                }
            }

            if (2 * counter < Math.min(other_fields[index_other].length, other_fields[a].length)) {
                continue;
            }

            // If overlap is large, check which field should be discarded.
            if (row) {
                if (scores[index_other][index_this] > scores[a][index_this]) {
                    return false;
                }
                kicked.add(a);
            } else {
                if (scores[index_this][index_other] > scores[index_this][a]) {
                    return false;
                }
                kicked.add(a);
            }
        }
        map.get(index_this).removeAll(kicked);
        return map.get(index_this).add(index_other);
    }

    /**
     * This class describes a match between two Blocks.
     */
    private static class Joint {
        private int[] m_block1, m_block2;
        private int m_field1, m_field2;
        private long m_score;

        private Joint(int[] block1, int field1, int[] block2, int field2,
                      int ori_diff, double dist_square) {
            m_block1 = block1;
            m_field1 = field1;
            m_block2 = block2;
            m_field2 = field2;
            m_score = Math.round(1e6 * ori_diff + 1e5 * Math.sqrt(dist_square));
        }

        private long score() {
            return m_score;
        }

        private int[] block1() {
            return m_block1;
        }

        private int[] block2() {
            return m_block2;
        }

        private int field1() {
            return m_field1;
        }

        private int field2() {
            return m_field2;
        }

        @Override
        public String toString() {
            return ("[(" + m_field1 + ", " + Arrays.toString(m_block1) + "), (" +
                    m_field2 + ", " + Arrays.toString(m_block2) + ")]");
        }
    }

    /**
     * Simple bag class to bundle data.
     */
    private static class joinInfo {
        private long m_min_score;
        private List<Integer> m_fields;

        private joinInfo(long min_score, int field) {
            m_min_score = min_score;
            m_fields = new ArrayList<>();
            m_fields.add(field);
        }

        private boolean add(int item) {
            if (m_fields == null) {
                m_fields = new ArrayList<>();
            }
            return m_fields.add(item);
        }
    }

}
