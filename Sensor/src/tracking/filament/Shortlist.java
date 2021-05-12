package tracking.filament;

/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2014 Carsten Gottschlich
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

public class Shortlist {

    private int[] m_distribution1;
    private int[] m_distribution2;
    private double[][] m_cost_matrix;

    // The first three arrays all contain the same information.
    // They exist in parallel to speed up lookups.
    private boolean[][] m_is_basis;
    private int[][] m_basis1;
    private int[][] m_basis2;
    private int[] m_basis1_count;
    private int[] m_basis2_count;

    private int m_size1;
    private int m_size2;
    private int[][] m_assignment;

    private double[] u;
    private double[] v;

    private boolean[] m_is_computed_u;
    private boolean[] m_is_computed_v;

    private final static double stoppingThreshold = -1e-6;

    private int[][] m_circle;
    private int m_count_circle;

    private boolean[] m_remember_search_along_same_producer;
    private int[] m_remember_fork_to_try_next;
    private int[] m_remember_count_circle;
    private int m_count_fork;

    private double m_search_list_fraction;
    private int m_search_n_most_negative;
    private int m_current_bin1;
    private int m_max_searched_list_index;

    int[] m_index_less;
    int[] m_index_greater;
    double[] m_cost_less;
    double[] m_cost_greater;


    public Shortlist(int[] distribution1, int[] distribution2, double[][] cost_matrix) {
        m_size1 = distribution1.length;
        m_size2 = distribution2.length;
        m_distribution1 = distribution1;
        m_distribution2 = distribution2;
        m_cost_matrix = cost_matrix;
    }

    public void run() {
        // Choose the three parameters
        //
        // 1) s := shortlist length
        // 2) k := max candidates
        // 3) m_size1 := max percent of shortlists
        //
        // automatically according to a rule of thumb:
        // s:= 15 if there are 200 or less consumer.
        // s increases by 15 for each doubling of the number of consumers
        //new PopUpDebug(ImageFactory.makeGreyImage(m_cost_matrix, 0, 1400, true), "Cost");

        int minimal_size = 200;
        int base_list_size = 15;
        int list_size_increment = 15;

        // 1) s := shortlist length
        int shortlist_size = shortListSize(m_size2, minimal_size, base_list_size, list_size_increment);

        // 2) k := max candidates
        m_search_n_most_negative = shortlist_size;

        // 3) m_size1 := max percent of shortlists
        m_search_list_fraction = 0.05;

        int shortlist_last = shortlist_size - 1;
        m_current_bin1 = 0;

        m_index_less = new int[shortlist_size];
        m_index_greater = new int[shortlist_size];
        m_cost_less = new double[shortlist_size];
        m_cost_greater = new double[shortlist_size];

        m_max_searched_list_index = (int) (m_search_list_fraction * m_size1);

        m_basis1 = new int[m_size1][m_size2];
        m_basis1_count = new int[m_size1];
        m_basis2 = new int[m_size2][m_size1];
        m_basis2_count = new int[m_size2];

        // Phase 1
        int[][] shortlist_from_producer_perspective = initShortlist(shortlist_size, shortlist_last);

        // Phase 2
        m_assignment = initAssignmentWithShortlist(shortlist_from_producer_perspective,
                shortlist_size);

        m_is_basis = findBasisForAssignmentAndFixDegenerationIfNecessary();

        u = new double[m_size1];
        v = new double[m_size2];
        m_is_computed_u = new boolean[m_size1];
        m_is_computed_v = new boolean[m_size2];

        m_circle = new int[m_size1 + m_size2][2];

        int max_size = Math.max(m_size1, m_size2);

        m_remember_fork_to_try_next = new int[max_size];
        m_remember_search_along_same_producer = new boolean[max_size];
        m_remember_count_circle = new int[max_size];

        initGraph();

        boolean finished = false;

        // *
        // Phase 3
        while (!finished) {
            finished = updateTransportPlanWithShortlist(shortlist_from_producer_perspective,
                    shortlist_size);
        }

        // Phase 4
        finished = false;
        while (!finished) {
            finished = updateTransportplanWithRowMostNegative();
            // finished = updateTransportplanWithMatrixMostNegative();
        }
        //new PopUpDebug(ImageFactory.makeGreyImage(m_assignment, 0, 15000, false), "Mass");
        // */

    }

    /**
     * Method number 1 run from the control method.
     * <p>
     * Determines the length of the shortlist depending on the size of the data sets.
     */
    private int shortListSize(int size, int minimal_size, int base_list_size, int list_size_increment) {
        if (size <= minimal_size) {
            return Math.min(size, base_list_size);
        } else {
            double n = size;
            double logBase = minimal_size;

            double x = Math.log(n / logBase) / Math.log(2);
            int list_size = base_list_size + (int) Math.round(list_size_increment * x);

            return list_size;
        }
    }

    /**
     * Method number 2 run from the control method.
     * <p>
     * In this method the array m_shortlist_from_producer_perspective is created,
     * which is not changed afterwards.
     */
    private int[][] initShortlist(int shortlist_size, int shortlist_last) {
        int[][] shortlist = new int[m_size1][];

        for (int a = 0; a < m_size1; a++) {
            int[] index = new int[shortlist_size];
            double[] cost = new double[shortlist_size];

            for (int k = 0; k < shortlist_size; k++) {
                index[k] = k;
                cost[k] = m_cost_matrix[a][k];
            }

            quickSort(index, cost, 0, shortlist_last);

            for (int k = shortlist_size; k < m_size2; k++) {
                if (m_cost_matrix[a][k] < cost[shortlist_last]) {
                    index[shortlist_last] = k;
                    cost[shortlist_last] = m_cost_matrix[a][k];

                    oneBubble(index, cost);
                }
            }
            shortlist[a] = index;
        }
        return shortlist;
    }

    /**
     * Method number 3 run from the control method.
     * <p>
     * This method creates an initial transport plan using the shortlist.
     */
    private int[][] initAssignmentWithShortlist(int[][] shortlist, int shortlist_size) {
        int[] prod = new int[m_size1];
        int[] cons = new int[m_size2];

        int[][] initialAssignment = new int[m_size1][m_size2];

        int assignedMass = 0;
        int totalMass = 0;
        for (int a = 0; a < m_size1; a++) {
            totalMass += m_distribution1[a];
        }
        System.arraycopy(m_distribution1, 0, prod, 0, m_size1);
        System.arraycopy(m_distribution2, 0, cons, 0, m_size2);

        boolean[] producerDone = new boolean[m_size1];
        boolean[] consumerDone = new boolean[m_size2];

        while (assignedMass < totalMass) {
            for (int a = 0; a < m_size1; a++) {
                if (!producerDone[a]) {
                    int indexJ = -1;

                    for (int k = 0; indexJ == -1 && k < shortlist_size; k++) {
                        if (!consumerDone[shortlist[a][k]]) {
                            indexJ = shortlist[a][k];
                        }
                    }

                    if (indexJ == -1) {
                        double minimum = Double.MAX_VALUE;

                        for (int b = 0; b < m_size2; b++) {
                            if (!consumerDone[b]) {
                                if (m_cost_matrix[a][b] < minimum) {
                                    minimum = m_cost_matrix[a][b];
                                    indexJ = b;
                                }
                            }
                        }
                    }

                    int mass = prod[a];
                    if (indexJ == -1) {//for debug
                        System.out.println("debug");
                    }
                    if (mass > cons[indexJ]) {
                        mass = cons[indexJ];
                    }
                    initialAssignment[a][indexJ] += mass;
                    assignedMass += mass;

                    prod[a] -= mass;
                    cons[indexJ] -= mass;

                    if (prod[a] == 0) {
                        producerDone[a] = true;
                    }

                    if (cons[indexJ] == 0) {
                        consumerDone[indexJ] = true;
                    }
                }
            }
        }

        return initialAssignment;
    }

    /**
     * Method number 4 run from the control method.
     * <p>
     * This method does something mysterious.
     */
    private boolean[][] findBasisForAssignmentAndFixDegenerationIfNecessary() {
        boolean[][] isPartOfBasis = new boolean[m_size1][m_size2];

        int countBasisEntries = 0;

        for (int a = 0; a < m_size1; a++) {
            for (int b = 0; b < m_size2; b++) {
                if (m_assignment[a][b] > 0) {
                    isPartOfBasis[a][b] = true;
                    countBasisEntries++;
                } else {
                    isPartOfBasis[a][b] = false;
                }
            }
        }

        int targetEntries = m_size1 + m_size2 - 1;

        // If m_assignment is a valid assignment matrix:
        // Return boolean form of initial assignment
        if (countBasisEntries == targetEntries) {
            return isPartOfBasis;
        }
        // If m_assignment has too many entries:
        // Return null, which later causes NullPointerException
        else if (countBasisEntries > targetEntries) {
            return null;
        }
        // If m_assignment has too few entries:
        // Do something obscure, which does not change m_assignment, then return.
        else {
            boolean[][] isConnected = new boolean[m_size1][m_size2];

            int[] first = findFirstUnconnectedBasisEntry(isPartOfBasis, isConnected);
            labelConnectedEntries(first, isPartOfBasis, isConnected);

            while (countBasisEntries < targetEntries) {
                int[] second = findFirstUnconnectedBasisEntry(isPartOfBasis, isConnected);
                labelConnectedEntries(second, isPartOfBasis, isConnected);

                // connect first and second tree
                int x = first[0];
                int y = second[1];
                isPartOfBasis[x][y] = true;
                isConnected[x][y] = true;
                countBasisEntries++;
            }

            return isPartOfBasis;
        }
    }

    /**
     * Method number 5 run from the control method.
     * <p>
     * Initializes m_basis1 and m_basis2. At this point, they are simply
     * alternative representations of m_is_basis.
     */
    private void initGraph() {
        // m_size1+m_size2-1 nodes (vertices)

        for (int prodID = 0; prodID < m_size1; prodID++) {
            for (int consID = 0; consID < m_size2; consID++) {
                if (m_is_basis[prodID][consID]) {
                    m_basis1[prodID][m_basis1_count[prodID]] = consID;
                    m_basis1_count[prodID]++;

                    m_basis2[consID][m_basis2_count[consID]] = prodID;
                    m_basis2_count[consID]++;
                }
            }
        }
    }

    /**
     * Method number 6 run from the control method.
     * <p>
     * THIS METHOD CHANGES m_assignment!
     */
    private boolean updateTransportPlanWithShortlist(int[][] shortlist, int shortlist_size) {

        // determine entering basis variable

        int[] index = determineNewBasisEntryBySearchingShortlists(shortlist, shortlist_size);

        if (index == null) {
            // everything optimal
            return true;
        }

        int indexU = index[0];
        int indexV = index[1];

        // find flow m_circle starting at (indexU|indexV)

        addToBasis(indexU, indexV);

        findCircle(indexU, indexV);

        // How much mass shall be transported and which basis variable shall be removed?

        int removeK = -1;

        int mass = Integer.MAX_VALUE;
        for (int k = 1; k < m_count_circle; k += 2) {
            if (m_assignment[m_circle[k][0]][m_circle[k][1]] < mass) {
                mass = m_assignment[m_circle[k][0]][m_circle[k][1]];
                removeK = k;
            }
        }

        if (mass > 0) {
            for (int k = 0; k < m_count_circle; k += 2) {
                m_assignment[m_circle[k][0]][m_circle[k][1]] += mass;
            }
            for (int k = 1; k < m_count_circle; k += 2) {
                m_assignment[m_circle[k][0]][m_circle[k][1]] -= mass;
            }
        }

        removeFromBasis(m_circle[removeK][0], m_circle[removeK][1]);

        return false;

    }

    /**
     * Method number 7 run from the control method.
     * <p>
     * THIS METHOD CHANGES m_assignment!
     */
    private boolean updateTransportplanWithRowMostNegative() {

        // determine entering basis variable

        int[] index = determineNewBasisEntryByRowMostNegative();

        if (index == null) {
            // everything optimal
            return true;
        }

        int indexU = index[0];
        int indexV = index[1];

        // find flow m_circle starting at (indexU|indexV)

        addToBasis(indexU, indexV);

        findCircle(indexU, indexV);

        // How much mass shall be transported and which basis variable shall be removed?

        int removeK = -1;

        int mass = Integer.MAX_VALUE;
        for (int k = 1; k < m_count_circle; k += 2) {
            if (m_assignment[m_circle[k][0]][m_circle[k][1]] < mass) {
                mass = m_assignment[m_circle[k][0]][m_circle[k][1]];
                removeK = k;
            }
        }

        if (mass > 0) {
            for (int k = 0; k < m_count_circle; k += 2) {
                m_assignment[m_circle[k][0]][m_circle[k][1]] += mass;
            }
            for (int k = 1; k < m_count_circle; k += 2) {
                m_assignment[m_circle[k][0]][m_circle[k][1]] -= mass;
            }
        }

        removeFromBasis(m_circle[removeK][0], m_circle[removeK][1]);

        return false;

    }


    /**
     * This method is run by method 7.
     */
    private int[] determineNewBasisEntryByRowMostNegative() {
        // Compute dual variables u und v

        for (int i = 0; i < m_size1; i++) {
            m_is_computed_u[i] = false;
        }
        for (int j = 0; j < m_size2; j++) {
            m_is_computed_v[j] = false;
        }

        // For all basic variables:
        // m_size2[i][j] = u[i] + v[j]

        int[] list = new int[m_size1 + m_size2];
        boolean[] isProd = new boolean[m_size1 + m_size2];
        int count = 0;
        int done = 0;

        u[0] = 0.0;
        m_is_computed_u[0] = true;
        list[count] = 0;
        isProd[count] = true;
        count++;

        while (done < count) {
            if (isProd[done]) {
                // iterate over consumer
                for (int k = 0; k < m_basis1_count[list[done]]; k++) {
                    if (!m_is_computed_v[m_basis1[list[done]][k]]) {
                        v[m_basis1[list[done]][k]] = m_cost_matrix[list[done]][m_basis1[list[done]][k]] -
                                u[list[done]];
                        m_is_computed_v[m_basis1[list[done]][k]] = true;
                        list[count] = m_basis1[list[done]][k];
                        isProd[count] = false;
                        count++;
                    }
                }
            } else {
                // iterate over producer
                for (int k = 0; k < m_basis2_count[list[done]]; k++) {
                    if (!m_is_computed_u[m_basis2[list[done]][k]]) {
                        u[m_basis2[list[done]][k]] = m_cost_matrix[m_basis2[list[done]][k]][list[done]] -
                                v[list[done]];
                        m_is_computed_u[m_basis2[list[done]][k]] = true;
                        list[count] = m_basis2[list[done]][k];
                        isProd[count] = true;
                        count++;
                    }
                }
            }

            done++;
        }

        double bestReducedCosts = 0.0;
        int indexU = -1;
        int indexV = -1;

        for (int k = 0; k < m_size1; k++) {
            int i = m_current_bin1;

            for (int j = 0; j < m_size2; j++) {
                if (!m_is_basis[i][j]) {
                    double reducedCosts = m_cost_matrix[i][j] - u[i] - v[j];

                    if (reducedCosts < bestReducedCosts) {
                        bestReducedCosts = reducedCosts;
                        indexU = i;
                        indexV = j;
                    }
                }
            }

            // m_size1("m_current_bin1: "+m_current_bin1);
            // m_size1("bestReducedCosts: "+bestReducedCosts);

            m_current_bin1++;
            m_current_bin1 %= m_size1;

            if (bestReducedCosts < stoppingThreshold) {
                return new int[]{indexU, indexV};
            }
        }

        if (bestReducedCosts > stoppingThreshold) {
            // everything is optimal
            return null;
        }

        return new int[]{indexU, indexV};
    }

    /**
     * This method is run by method 6.
     */
    private int[] determineNewBasisEntryBySearchingShortlists(int[][] shortlist, int shortlist_size) {

        for (int i = 0; i < m_size1; i++) {
            m_is_computed_u[i] = false;
        }
        for (int j = 0; j < m_size2; j++) {
            m_is_computed_v[j] = false;
        }

        // For all basic variables:
        // m_size2[i][j] = u[i] + v[j]

        int[] list = new int[m_size1 + m_size2];
        boolean[] isProd = new boolean[m_size1 + m_size2];
        int count = 0;
        int done = 0;

        u[0] = 0.0;
        m_is_computed_u[0] = true;
        list[count] = 0;
        isProd[count] = true;
        count++;

        while (done < count) {
            if (isProd[done]) {
                // iterate over consumer

                for (int k = 0; k < m_basis1_count[list[done]]; k++) {
                    if (!m_is_computed_v[m_basis1[list[done]][k]]) {
                        v[m_basis1[list[done]][k]] = m_cost_matrix[list[done]][m_basis1[list[done]][k]] -
                                u[list[done]];
                        m_is_computed_v[m_basis1[list[done]][k]] = true;
                        list[count] = m_basis1[list[done]][k];
                        isProd[count] = false;
                        count++;
                    }
                }
            } else {
                // iterate over producer

                for (int k = 0; k < m_basis2_count[list[done]]; k++) {
                    if (!m_is_computed_u[m_basis2[list[done]][k]]) {
                        u[m_basis2[list[done]][k]] = m_cost_matrix[m_basis2[list[done]][k]][list[done]] -
                                v[list[done]];
                        m_is_computed_u[m_basis2[list[done]][k]] = true;
                        list[count] = m_basis2[list[done]][k];
                        isProd[count] = true;
                        count++;
                    }
                }
            }

            done++;
        }

        // Search shortlists for new basic variable

        double bestReducedCosts = 0.0;
        int indexU = -1;
        int indexV = -1;

        int countCandidate = 0;
        int countSearchedLists = 0;

        while (countSearchedLists < m_max_searched_list_index) {
            int i = m_current_bin1;

            for (int k = 0; k < shortlist_size; k++) {
                int j = shortlist[i][k];

                if (!m_is_basis[i][j]) {
                    double reducedCosts = m_cost_matrix[i][j] - u[i] - v[j];

                    if (reducedCosts < 0) {
                        countCandidate++;

                        if (reducedCosts < bestReducedCosts) {
                            bestReducedCosts = reducedCosts;
                            indexU = i;
                            indexV = j;
                        }
                    }
                }
            }

            m_current_bin1++;
            m_current_bin1 %= m_size1;

            if (countCandidate >= m_search_n_most_negative) {
                if (bestReducedCosts < stoppingThreshold) {
                    return new int[]{indexU, indexV};
                } else {
                    return null;
                }
            }

            countSearchedLists++;
        }

        if (bestReducedCosts > stoppingThreshold) {
            return null;
        } else {
            return new int[]{indexU, indexV};
        }
    }

    /**
     * This method is run by method 2, initShortlist.
     * <p>
     * MAY CHANGE INPUT ARRAYS!
     */
    private void oneBubble(int[] index, double[] cost) {
        for (int a = index.length - 2; a >= 0; a--) {
            if (cost[a] > cost[a + 1]) {
                int i = index[a];
                double co = cost[a];
                index[a] = index[a + 1];
                cost[a] = cost[a + 1];
                index[a + 1] = i;
                cost[a + 1] = co;
            } else {
                return;
            }
        }
    }

    private void labelConnectedEntries(int[] start, boolean[][] isBasis, boolean[][] isConnected) {
        int countTodo = 1;
        int countDone = 0;
        int[][] list = new int[isBasis.length + isBasis[0].length][2];
        list[0] = start;
        isConnected[start[0]][start[1]] = true;

        while (countTodo > countDone) {
            int i = list[countDone][0];
            int j = list[countDone][1];
            countDone++;

            for (int a = 0; a < isBasis.length; a++) {
                if (isBasis[a][j] && !isConnected[a][j]) {
                    list[countTodo][0] = a;
                    list[countTodo][1] = j;
                    countTodo++;
                    isConnected[a][j] = true;
                }
            }

            for (int b = 0; b < isBasis[0].length; b++) {
                if (isBasis[i][b] && !isConnected[i][b]) {
                    list[countTodo][0] = i;
                    list[countTodo][1] = b;
                    countTodo++;
                    isConnected[i][b] = true;
                }
            }
        }
    }

    private int[] findFirstUnconnectedBasisEntry(boolean[][] isBasis, boolean[][] isConnected) {
        for (int a = 0; a < isBasis.length; a++) {
            for (int b = 0; b < isBasis[0].length; b++) {
                if (isBasis[a][b] && !isConnected[a][b]) {
                    return new int[]{a, b};
                }
            }
        }
        return null;
    }

    private void addToBasis(int prodID, int consID) {
        m_is_basis[prodID][consID] = true;

        m_basis1[prodID][m_basis1_count[prodID]] = consID;
        m_basis1_count[prodID]++;

        m_basis2[consID][m_basis2_count[consID]] = prodID;
        m_basis2_count[consID]++;
    }

    private void removeFromBasis(int prodID, int consID) {
        m_is_basis[prodID][consID] = false;

        if (m_basis1_count[prodID] == 1) {
            m_basis1_count[prodID] = 0;
        } else {
            for (int k = 0; k < m_basis1_count[prodID]; k++) {
                if (consID == m_basis1[prodID][k]) {
                    m_basis1[prodID][k] = m_basis1[prodID][m_basis1_count[prodID] - 1];
                    m_basis1_count[prodID]--;
                    break;
                }
            }
        }

        if (m_basis2_count[consID] == 1) {
            m_basis2_count[consID] = 0;
        } else {
            for (int k = 0; k < m_basis2_count[consID]; k++) {
                if (prodID == m_basis2[consID][k]) {
                    m_basis2[consID][k] = m_basis2[consID][m_basis2_count[consID] - 1];
                    m_basis2_count[consID]--;
                    break;
                }
            }
        }
    }

    private void findCircle(int indexU, int indexV) {
        m_circle[0][0] = indexU;
        m_circle[0][1] = indexV;
        m_count_circle = 1;
        m_count_fork = 0;

        boolean finished = false;

        boolean searchAlongTheSameProducer = true;
        int forkToTryNext = 0;

        while (!finished) {
            if (searchAlongTheSameProducer) {
                // search along same producer
                int lastProdID = m_circle[m_count_circle - 1][0];
                int lastConsID = m_circle[m_count_circle - 1][1];

                int countCandidate = 0;
                int firstConsID = -1;

                for (int a = 0; a < m_basis1_count[lastProdID]; a++) {
                    int candidateConsID = m_basis1[lastProdID][a];

                    if (m_basis2_count[candidateConsID] > 1 && candidateConsID != lastConsID) {
                        if (countCandidate == 0) {
                            firstConsID = candidateConsID;
                        }
                        countCandidate++;
                    }
                }

                if (countCandidate == 0) {
                    // dead end.
                    // jump back to last fork

                    // set:
                    // - m_count_circle
                    // - searchAlongTheSameProducer
                    // - forkToTryNext

                    m_count_circle = m_remember_count_circle[m_count_fork - 1];
                    searchAlongTheSameProducer = m_remember_search_along_same_producer[m_count_fork - 1];
                    forkToTryNext = m_remember_fork_to_try_next[m_count_fork - 1];
                } else if (countCandidate == 1) {
                    // easy. add only choice to m_circle

                    m_circle[m_count_circle][0] = lastProdID;
                    m_circle[m_count_circle][1] = firstConsID;
                    m_count_circle++;

                    searchAlongTheSameProducer = false;

                    forkToTryNext = 0;
                } else if (countCandidate >= 2) {
                    // fork.

                    if (forkToTryNext == 0) {
                        m_circle[m_count_circle][0] = lastProdID;
                        m_circle[m_count_circle][1] = firstConsID;
                        m_count_circle++;

                        m_remember_search_along_same_producer[m_count_fork] = searchAlongTheSameProducer;
                        m_remember_fork_to_try_next[m_count_fork] = 1;
                        m_remember_count_circle[m_count_fork] = m_count_circle - 1;
                        m_count_fork++;

                        searchAlongTheSameProducer = false;

                        forkToTryNext = 0;
                    } else if (forkToTryNext >= countCandidate) {
                        // all possibilities explored, dead end.
                        // jump back to last fork

                        m_count_fork--;
                        m_count_circle = m_remember_count_circle[m_count_fork - 1];
                        searchAlongTheSameProducer = m_remember_search_along_same_producer[m_count_fork - 1];
                        forkToTryNext = m_remember_fork_to_try_next[m_count_fork - 1];
                    } else {
                        m_remember_fork_to_try_next[m_count_fork - 1]++;

                        int k = 0;

                        for (int a = 0; a < m_basis1_count[lastProdID]; a++) {
                            int candidateConsID = m_basis1[lastProdID][a];

                            if (m_basis2_count[candidateConsID] > 1 && candidateConsID != lastConsID) {
                                if (k == forkToTryNext) {
                                    m_circle[m_count_circle][0] = lastProdID;
                                    m_circle[m_count_circle][1] = candidateConsID;
                                    m_count_circle++;
                                    searchAlongTheSameProducer = false;
                                }

                                k++;
                            }
                        }

                        forkToTryNext = 0;
                    }
                }
            } else {
                // search along the same consumer. check, if m_circle can be closed.

                int lastProdID = m_circle[m_count_circle - 1][0];
                int lastConsID = m_circle[m_count_circle - 1][1];

                int countCandidate = 0;
                int firstProdID = -1;

                for (int b = 0; b < m_basis2_count[lastConsID]; b++) {
                    int candidateProdID = m_basis2[lastConsID][b];

                    if (candidateProdID == indexU && m_count_circle > 3) {
                        finished = true;
                    } else if (!finished && m_basis1_count[candidateProdID] > 1 && candidateProdID != lastProdID) {
                        if (countCandidate == 0) {
                            firstProdID = candidateProdID;
                        }
                        countCandidate++;
                    }
                }

                if (!finished) {
                    if (countCandidate == 0) {
                        // dead end.
                        // jump back to last fork

                        // set:
                        // - m_count_circle
                        // - searchAlongTheSameProducer
                        // - forkToTryNext

                        m_count_circle = m_remember_count_circle[m_count_fork - 1];
                        searchAlongTheSameProducer = m_remember_search_along_same_producer[m_count_fork - 1];
                        forkToTryNext = m_remember_fork_to_try_next[m_count_fork - 1];
                    } else if (countCandidate == 1) {
                        // easy. add only choice to m_circle

                        m_circle[m_count_circle][0] = firstProdID;
                        m_circle[m_count_circle][1] = lastConsID;
                        m_count_circle++;

                        searchAlongTheSameProducer = true;

                        forkToTryNext = 0;
                    } else if (countCandidate >= 2) {
                        // fork.

                        if (forkToTryNext == 0) {
                            m_circle[m_count_circle][0] = firstProdID;
                            m_circle[m_count_circle][1] = lastConsID;
                            m_count_circle++;

                            m_remember_search_along_same_producer[m_count_fork] = searchAlongTheSameProducer;
                            m_remember_fork_to_try_next[m_count_fork] = 1;
                            m_remember_count_circle[m_count_fork] = m_count_circle - 1;
                            m_count_fork++;

                            searchAlongTheSameProducer = true;

                            forkToTryNext = 0;
                        } else if (forkToTryNext >= countCandidate) {
                            // all possibilities explored, dead end.
                            // jump back to last fork

                            m_count_fork--;
                            m_count_circle = m_remember_count_circle[m_count_fork - 1];
                            searchAlongTheSameProducer = m_remember_search_along_same_producer[m_count_fork - 1];
                            forkToTryNext = m_remember_fork_to_try_next[m_count_fork - 1];
                        } else {
                            m_remember_fork_to_try_next[m_count_fork - 1]++;

                            int k = 0;

                            for (int b = 0; b < m_basis2_count[lastConsID]; b++) {
                                int candidateProdID = m_basis2[lastConsID][b];

                                if (candidateProdID == indexU && m_count_circle > 3) {
                                    finished = true;

                                } else if (!finished && m_basis1_count[candidateProdID] > 1 &&
                                        candidateProdID != lastProdID) {
                                    if (k == forkToTryNext) {
                                        m_circle[m_count_circle][0] = candidateProdID;
                                        m_circle[m_count_circle][1] = lastConsID;
                                        m_count_circle++;

                                        searchAlongTheSameProducer = true;
                                    }

                                    k++;
                                }
                            }

                            forkToTryNext = 0;
                        }
                    }
                }
            }
        }
    }

    private void quickSort(int[] index, double[] cost, int startIndex, int endIndex) {
        // Sort ascending, smallest cost first

        if (endIndex - startIndex > 0) { // zwei oder mehr Elemente
            int countLess = 0;
            int countGreater = 0;

            double pivotCost = cost[endIndex];
            int pivotIndex = index[endIndex];

            // m_size1("pivotCost: "+pivotCost);

            for (int k = startIndex; k < endIndex; k++) {
                if (cost[k] < pivotCost) {
                    m_index_less[countLess] = index[k];
                    m_cost_less[countLess] = cost[k];
                    countLess++;
                } else {
                    m_index_greater[countGreater] = index[k];
                    m_cost_greater[countGreater] = cost[k];
                    countGreater++;
                }
            }

            for (int k = 0; k < countLess; k++) {
                index[k + startIndex] = m_index_less[k];
                cost[k + startIndex] = m_cost_less[k];
            }

            index[startIndex + countLess] = pivotIndex;
            cost[startIndex + countLess] = pivotCost;

            int start2 = startIndex + countLess + 1;

            for (int k = 0; k < countGreater; k++) {
                index[k + start2] = m_index_greater[k];
                cost[k + start2] = m_cost_greater[k];
            }

            // less
            quickSort(index, cost, startIndex, start2 - 2);

            // greater
            quickSort(index, cost, start2, endIndex);
        }
    }

    public int[][] getAssignment() {
        int[][] out = new int[m_size1][m_size2];
        for (int b = 0; b < m_size2; b++) {
            for (int a = 0; a < m_size1; a++) {
                out[a][b] = m_assignment[a][b];
            }
        }
        return out;
    }

    private double computeTotalCosts() {
        double total = 0.0;
        for (int b = 0; b < m_size2; b++) {
            for (int a = 0; a < m_size1; a++) {
                if (m_assignment[a][b] > 0) {
                    total += (m_assignment[a][b] * m_cost_matrix[a][b]);
                }
            }
        }
        return total;
    }

    private int countFlows() {
        int countFlow = 0;
        for (int b = 0; b < m_size2; b++) {
            for (int a = 0; a < m_size1; a++) {
                if (m_assignment[a][b] > 0) {
                    countFlow++;
                }
            }
        }
        return countFlow;
    }

    private int computeTotalMassFlow() {
        int totalFlow = 0;
        for (int b = 0; b < m_size2; b++) {
            for (int a = 0; a < m_size1; a++) {
                if (m_assignment[a][b] > 0) {
                    totalFlow += m_assignment[a][b];
                }
            }
        }
        return totalFlow;
    }

    private int countBasisEntries() {
        int countBasisEntries = 0;
        for (int b = 0; b < m_size2; b++) {
            for (int a = 0; a < m_size1; a++) {
                if (m_is_basis[a][b]) {
                    countBasisEntries++;
                }
            }
        }
        return countBasisEntries;
    }

}
