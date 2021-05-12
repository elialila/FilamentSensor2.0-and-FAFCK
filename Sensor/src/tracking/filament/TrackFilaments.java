package tracking.filament;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2018 Benjamin Eltzner
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
import core.filaments.Filament;

import java.util.ArrayList;
import java.util.List;


public class TrackFilaments {
    private static final double LENGTH_FACTOR = 1000.0;
    private static final double LONG_LENGTH_FACTOR = LENGTH_FACTOR / 1e6;
    private double m_max_dist_square;
    private double m_factor_angle;
    private double m_factor_length;
    private List<Integer> m_labels1;
    private List<Integer> m_labels2;
    private int[] m_distribution1;
    private int[] m_distribution2;
    private double[][] m_cost_matrix;

    public TrackFilaments(double max_dist, double factor_angle, double factor_length) {
        m_max_dist_square = max_dist * max_dist;
        m_factor_angle = factor_angle;
        m_factor_length = factor_length;
        m_labels1 = new ArrayList<>();
        m_labels2 = new ArrayList<>();
    }

    public void setupTimeSeries(List<AbstractFilament> list1, List<AbstractFilament> list2, int length) {

        List<Filament> fragment_list1 = splitLines(list1, length, m_labels1);
        List<Filament> fragment_list2 = splitLines(list2, length, m_labels2);
        m_distribution1 = makeDistribution(fragment_list1);
        m_distribution2 = makeDistribution(fragment_list2);

        // Swap sums which are used for line pixel creation or annihilation.
        int tmp = m_distribution1[m_distribution1.length - 1];
        m_distribution1[m_distribution1.length - 1] = m_distribution2[m_distribution2.length - 1];
        m_distribution2[m_distribution2.length - 1] = tmp;

        m_cost_matrix = distanceMatrix(fragment_list1, fragment_list2);
    }

    public double[][] track() {
        Shortlist transport = new Shortlist(m_distribution1, m_distribution2, m_cost_matrix);
        transport.run();
        int[][] transport_matrix = transport.getAssignment();

        int n_from = m_labels1.get(m_labels1.size() - 1) + 2;
        int n_to = m_labels2.get(m_labels2.size() - 1) + 2;
        double[][] filament_transport_matrix = new double[n_from][n_to];
        for (int li = 0; li < transport_matrix.length; li++) {
            int i = n_from - 1;
            if (li < m_labels1.size()) {
                i = m_labels1.get(li);
            }
            for (int lj = 0; lj < transport_matrix[0].length; lj++) {
                if (transport_matrix[li][lj] <= 0) {
                    continue;
                }
                int j = n_to - 1;
                if (lj < m_labels2.size()) {
                    j = m_labels2.get(lj);
                }
                filament_transport_matrix[i][j] += transport_matrix[li][lj] / LENGTH_FACTOR;
            }
        }

        //TODO: What to do with fragment-level data? Maybe only store "relative dump bin weights"?
        return filament_transport_matrix;
    }

    /**
     * @param old_list
     * @param length   parameter which decides in how many linear pieces a filament gets segmented
     * @param labels
     * @return
     */
    private static List<Filament> splitLines(List<AbstractFilament> old_list, int length, List<Integer> labels) {
        final List<Filament> new_list = new ArrayList<>();
        int counter = 0;
        for (final AbstractFilament f : old_list) {
            List<Filament> tmp = f.splitIntoLinearPieces(length);
            double l = 0;
            for (int j = 0; j < tmp.size(); j++) {
                labels.add(counter);
                l += 1e-6 * tmp.get(j).getLength();
            }
            new_list.addAll(tmp);
            counter++;
        }
        return new_list;
    }

    private static int[] makeDistribution(List<Filament> fragment_list) {
        // Length of fragments must be passed to the shortlist class as ints.
        // Therefore, lengths are multiplied with 1000 to emulate three digit precision.
        int sum = 0;
        int[] distribution = new int[fragment_list.size() + 1];
        for (int k = 0; k < fragment_list.size(); k++) {
            // Length of filaments is stored with 6 digits.
            distribution[k] = (int) Math.round(LONG_LENGTH_FACTOR * fragment_list.get(k).getLength());
            sum += distribution[k];
        }
        distribution[fragment_list.size()] = sum;
        return distribution;
    }

    private double[][] distanceMatrix(List<Filament> list1, List<Filament> list2) {
        // Final row and column of distance matrix allow creation and destruction
        // of lines at fixed cost per pixel given by m_max_dist_square.
        double[][] distance_matrix = new double[list1.size() + 1][list2.size() + 1];
        for (int n1 = 0; n1 < list1.size(); n1++) {
            distance_matrix[n1][list2.size()] = m_max_dist_square;
            for (int n2 = 0; n2 < list2.size(); n2++) {
                distance_matrix[n1][n2] = distance(list1.get(n1), list2.get(n2));
            }
        }
        for (int n2 = 0; n2 < list2.size(); n2++) {
            distance_matrix[list1.size()][n2] = m_max_dist_square;
        }
        return distance_matrix;
    }

    private double distance(Filament filament1, Filament filament2) {
        double[] dists = filament1.distanceSquares(filament2);
        double dist = dists[0] * m_factor_angle + dists[1] * m_factor_length;
        if (dist > 3 * m_max_dist_square) {
            dist = 3 * m_max_dist_square;
        }
        return dist;
    }
}
