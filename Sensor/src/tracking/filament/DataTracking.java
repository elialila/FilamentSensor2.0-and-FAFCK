package tracking.filament;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2019 Benjamin Eltzner
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


import core.filaments.AbstractFilament;
import core.filaments.FilamentChain;
import core.image.Entry;
import core.image.ImageWrapper;
import core.FilamentSensor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataTracking {


    private List<DynamicFilament> m_tracked_filaments;
    private List<List<AbstractFilament>> m_filament_data;
    private List<List<Integer>> m_valid_filaments;

    public DataTracking() {
        m_tracked_filaments = new ArrayList<>();
        m_filament_data = new ArrayList<>();
        m_valid_filaments = new ArrayList<>();
    }

    public void loadData(ImageWrapper images) {
        if (images == null) {
            return;
        }
        m_filament_data.clear();
        m_tracked_filaments.clear();
        m_valid_filaments.clear();
        //filter by keep property (before processing to prevent inconsistencies)
        m_filament_data.addAll(
                images.getEntryList().stream().sequential().
                        map(e -> e.getDataFilament().getFilaments().
                                stream().filter(AbstractFilament::isKeep).
                                collect(Collectors.toList())
                        ).collect(Collectors.toList())
        );
    }

    public void loadData(List<Entry> entries) {
        if (entries == null) {
            return;
        }
        m_filament_data.clear();
        m_tracked_filaments.clear();
        m_valid_filaments.clear();
        m_filament_data.addAll(
                entries.stream().sequential().map(e -> e.getDataFilament().getFilaments().
                        stream().filter(AbstractFilament::isKeep).
                        collect(Collectors.toList())
                ).collect(Collectors.toList())
        );

    }


    public int getMaxTime() {
        return m_filament_data.size();
    }


    /**
     * Identify those filaments which can be matched to some degree to one or more filaments
     * on either the previous or following image. If a filament cannot be matched, it could
     * be regarded as detection error and excluded from further consideration.
     * <p>
     * The function returns a "success flag".
     */
    public boolean findSolitaryFilaments(double max_dist,
                                         double factor_angle,
                                         double factor_length,
                                         int length) {
        if (m_filament_data.isEmpty()) {
            return false;
        }
        m_valid_filaments.clear();//clear valid filaments list, added to combine methods (use getdistancematrices also in trackfilaments)
        //since it does nearly the same and with an if it works for both

        long debugTime = System.currentTimeMillis();
        //this call is the slowest part of the method (taking most of the time)
        List<double[][]> matrices = getDistanceMatrices(max_dist, factor_angle, factor_length, length);
        FilamentSensor.debugPerformance("getDistanceMatrices()", debugTime);
        if (matrices == null) return false;

        debugTime = System.currentTimeMillis();
        int max_time = m_filament_data.size() - 1;//MAX_TIME;//m_filament_data.size()-1
        for (int time = 0; time < max_time; time++) {
            List<Integer> valid_filaments_from = new ArrayList<>();
            double[][] matrix = matrices.get(time);

            if (matrix == null) {
                //placeholder for the "from"
                m_valid_filaments.add(new ArrayList<>());//add an empty list to prevent errors?
                //placeholder for the "to"
                m_valid_filaments.add(new ArrayList<>());


                continue;
            }
            //if data containing error's (some filaments not found due to area calc)
            //skip that time

            double[] lengths_to = new double[matrix[0].length];
            double[] dump_to = new double[matrix[0].length];

            for (int from = 0; from < matrix.length; from++) {
                double length_from = 0;
                for (int to = 0; to < matrix[from].length; to++) {
                    if (matrix[from][to] > 0) {
                        length_from += matrix[from][to];
                        lengths_to[to] += matrix[from][to];
                        if (from == matrix.length - 1) {
                            dump_to[to] = matrix[from][to];
                        }
                    }
                }
                if (from >= matrix.length - 1) {
                    continue;
                }
                // Record valid lines of "from" list.
                if (matrix[from][matrix[from].length - 1] < 0.5 * length_from) {
                    if (time == 0) { //valid_filaments_lists should be empty
                        valid_filaments_from.add(from);
                    } else { //valid_filaments_lists should contain the time-list
                        m_valid_filaments.get(time).add(from);
                    }
                }
            }
            // Add list for the first image.
            if (time == 0) {
                m_valid_filaments.add(valid_filaments_from);
            }

            // Now collect valid lines for the to-list
            List<Integer> valid_filaments_to = new ArrayList<>();
            for (int to = 0; to < lengths_to.length - 1; to++) {
                if (dump_to[to] < 0.5 * lengths_to[to]) {
                    valid_filaments_to.add(to);
                }
            }
            m_valid_filaments.add(valid_filaments_to);
        }
        FilamentSensor.debugPerformance("For Loop DataTracking.findSolitaryFilaments", debugTime);


        //remove duplicates and sort
        m_valid_filaments = m_valid_filaments.stream().map(l -> l.stream().distinct().sorted().collect(Collectors.toList())).collect(Collectors.toList());


        // deduplicate lists
        /*for (int time = 0; time < m_valid_filaments.size(); time++) {
            List<Integer> this_list = m_valid_filaments.get(time);
            Collections.sort(this_list);
            List<Integer> deduplicated_list = new ArrayList<>();
            int size = -1;
            for (int j = 0; j < this_list.size(); j++) {
                int k = this_list.get(j);
                if (deduplicated_list.isEmpty() || deduplicated_list.get(size) < k) {
                    deduplicated_list.add(k);
                    size++;
                }
            }
            m_valid_filaments.set(time, deduplicated_list);
        }*/
        return true;
    }

    public boolean trackFilaments(double max_dist,
                                  double factor_angle,
                                  double factor_length,
                                  int length, boolean combineMultiMatches) {
        if (m_filament_data.isEmpty()) {
            return false;
        }

        int max_time = m_filament_data.size() - 1;//MAX_TIME;//m_filament_data.size()-1
        Map<Integer, Integer> current_filaments = new HashMap<>();

        List<double[][]> matrices = getDistanceMatrices(max_dist, factor_angle, factor_length, length);
        if (matrices == null) {
            return false;
        }

        long start = System.currentTimeMillis();
        for (int time = 0; time < max_time; time++) {
            List<AbstractFilament> list_from = filterList(m_filament_data.get(time),
                    (m_valid_filaments.size() >= max_time + 1) ? m_valid_filaments.get(time) : null);
            List<AbstractFilament> list_to = filterList(m_filament_data.get(time + 1),
                    (m_valid_filaments.size() >= max_time + 1) ? m_valid_filaments.get(time + 1) : null);
            double[][] matrix = matrices.get(time);

            if (matrix == null) {
                continue;
            }


            double[] lengths_from = new double[matrix.length];
            double[] lengths_to = new double[matrix[0].length];
            Map<Integer, Integer> future_filaments = new HashMap<>();
            for (int from = 0; from < matrix.length; from++) {
                for (int to = 0; to < matrix[from].length; to++) {
                    if (matrix[from][to] > 0) {
                        lengths_from[from] += matrix[from][to];
                        lengths_to[to] += matrix[from][to];
                    }
                }
            }
            for (int from = 0; from < matrix.length; from++) {
                Map<Double, Integer> to_which = new HashMap<>();
                for (int to = 0; to < matrix[from].length; to++) {
                    if ((matrix[from][to] > 0) &&
                            (to < matrix[from].length - 1) &&
                            (from < matrix.length - 1)) {
                        to_which.put(matrix[from][to], to);
                    }
                }
                if (from >= matrix.length - 1) {
                    continue;
                }
                List<Double> scores = new ArrayList<>(to_which.keySet());
                Collections.sort(scores);
                Collections.reverse(scores);
                FilamentChain to_chain = new FilamentChain();
                double so_far = 0;
                for (Double s : scores) {
                    if (s > Math.max(10, 0.1 * lengths_from[from]) && so_far < 0.75 * lengths_from[from]) {
                        to_chain.add(list_to.get(to_which.get(s)));
                        so_far += s;
                    }
                }
                int position = -1;
                if (current_filaments.get(from) != null) {
                    position = current_filaments.get(from);
                    m_tracked_filaments.get(position).add(time + 1, to_chain);
                } else {
                    DynamicFilament fil = new DynamicFilament(time, new FilamentChain(list_from.get(from)));
                    fil.add(time + 1, to_chain);
                    m_tracked_filaments.add(fil);
                    position = m_tracked_filaments.size() - 1;
                }
                if (to_chain.getListSize() == 1) {
                    double s = scores.get(0);
                    if ((s > 0.5 * lengths_from[from]) &&
                            (s > 0.5 * lengths_to[to_which.get(s)])) {
                        future_filaments.put(to_which.get(s), position);
                    }
                }
            }
            current_filaments.clear();
            current_filaments.putAll(future_filaments);

        }
        FilamentSensor.debugPerformance("TrackFilaments:forLoop", start);

        //sort longest time-line first
        m_tracked_filaments.sort((o1, o2) -> {
            int result = Integer.compare(o1.getFilaments().size(), o2.getFilaments().size());
            if (result != 0) return result * -1;
            return Integer.compare(o1.getFilaments().keySet().stream().mapToInt(i -> i).max().orElse(0),
                    o2.getFilaments().keySet().stream().mapToInt(i -> i).max().orElse(0));
        });


        m_tracked_filaments.forEach(df -> df.calcProperties(combineMultiMatches));

        return true;
    }

    /**
     * @param max_dist      defined maxDistance if a distance exceeds it, it will get set to max_dist * 3
     * @param factor_angle  weight used in distance calculation to weight the angle (TrackFilaments::distance())
     * @param factor_length weight used in distance calculation to weight the length (TrackFilaments::distance())
     * @param length        parameter for splitting filaments in linear pieces (decides the number and length of the pieces) ... value range unknown
     * @return
     */
    private List<double[][]> getDistanceMatrices(double max_dist,
                                                 double factor_angle,
                                                 double factor_length,
                                                 int length) {

        //if no filament is found this line is -1 and results in exception
        final int max_time = m_filament_data.size() - 1;//MAX_TIME;//m_filament_data.size()-1
        if (max_time < 0) return null;
        return IntStream.range(0, max_time).parallel().mapToObj(time -> {
            TrackFilaments tracker = new TrackFilaments(max_dist, factor_angle, factor_length);
            List<Integer> filter_from = null;
            List<Integer> filter_to = null;
            if (m_valid_filaments.size() >= max_time + 1) {//this can only be true in trackFilaments (findSolitaryFilaments resets valid filaments)
                filter_from = m_valid_filaments.get(time);
                filter_to = m_valid_filaments.get(time + 1);
            }
            List<AbstractFilament> list_from = filterList(m_filament_data.get(time), filter_from);
            List<AbstractFilament> list_to = filterList(m_filament_data.get(time + 1), filter_to);
            if (list_from.size() == 0 || list_to.size() == 0) return null;
            tracker.setupTimeSeries(list_from, list_to, length);
            return tracker.track();
        }).collect(Collectors.toList());
    }

    private static <T> List<T> filterList(List<T> object_list, List<Integer> index_list) {
        if (index_list == null || index_list.isEmpty() || index_list.size() == 0 || object_list == null || object_list.size() == 0) {
            return object_list;
        }
        //catch the case when objectList.size() smaller than index_list
        return index_list.stream().filter(idx -> idx < object_list.size()).map(object_list::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<DynamicFilament> getTrackedFilaments() {
        return m_tracked_filaments;
    }

    public List<DynamicFilament> filterTrackedFilaments(boolean chkMinLength, int minLength, boolean chkMaxLength, int maxLength) {
        List<DynamicFilament> result = m_tracked_filaments;
        if (chkMinLength) {
            result = result.stream().filter(df -> df.getFilaments().size() >= minLength).collect(Collectors.toList());
        }
        if (chkMaxLength) {
            result = result.stream().filter(df -> df.getFilaments().size() <= maxLength).collect(Collectors.toList());
        }
        return result;
    }
}
