package core.tracers;

import core.Const;
import core.filaments.CurvedFilament;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Gatherer {
    private List<int[]> m_points;
    private int[] m_start;
    private int m_score;
    private boolean m_xcross;
    private int m_direction;


    private int m_orientations;
    private int m_directions;
    private int m_direction_step;


    private Point[][] m_lattice_sense;
    private Point[][] m_lattice_step;

    public Gatherer(int x_pos, int y_pos, int width, int direction, int m_orientations, int m_directions, int m_direction_step, Point[][] m_lattice_sense, Point[][] m_lattice_step) {
        this.m_orientations = m_orientations;
        this.m_lattice_sense = m_lattice_sense;
        this.m_lattice_step = m_lattice_step;
        this.m_directions = m_directions;
        this.m_direction_step = m_direction_step;


        m_start = new int[]{x_pos, y_pos, width, direction % m_orientations};
        m_direction = direction;
        m_xcross = false;
    }

    /**
     * Collects points meeting condition starting at m_start, and returns a score( which is the sum of all width's from width_map per point)
     *
     * @param width_map
     * @return
     */
    public int sense(int[][] width_map) {
        m_points = new ArrayList<>();
        m_points.add(m_start);
        m_score = m_start[2];
        int dir = m_direction;
        Point[] ray = m_lattice_sense[dir];
        int step_length = m_lattice_step[dir].length;
        int x = m_start[0], y = m_start[1], score = m_score;
        int i = 0;
        boolean one_black_pixel = false;
        for (Point p : ray) {
            int px = x + p.x, py = y + p.y;
            if (px >= 0 && py >= 0 && px < width_map.length && py < width_map[0].length) {
                int w = width_map[px][py];
                // Accept at most one 0 in a row.
                if (w == 0) {
                    if (one_black_pixel) {
                        break;
                    } else {
                        one_black_pixel = true;
                    }
                } else if (one_black_pixel) {
                    one_black_pixel = false;
                }
                if (i < step_length) {
                    m_points.add(new int[]{px, py, w, dir % m_orientations});
                    m_score += w;
                }
                score += w;
                i++;
            }
        }
        // Eliminate empty points from end of list.
        for (i = length(); i >= 0; i--) {
            int w = m_points.get(i)[2];
            if (w > 0 && m_score >= m_points.size()) {
                break;
            }
            m_points.remove(i);
            m_score -= w;
        }
        return score;
    }

    public void gather(int[][] width_map) {
        gather(m_points.get(m_points.size() - 1), width_map);//width_map,
    }

    /**
     * Gather points on curve
     *
     * @param start
     * @param m_width_map
     */
    public void gather(int[] start, int[][] m_width_map)//int[][] width_map,
    {
        int dir = m_direction;

        // Current direction and both neighboring directions.
        int[] dirs = new int[]{dir, (dir + 1) % m_directions,
                (dir + m_directions - 1) % m_directions};
        double best_score = 0, best_small_score = 0;
        int full_score = 0;
        List<int[]> best_path = new ArrayList<>(), path = new ArrayList<>();
        for (int d : dirs) {
            path.clear();
            Point[] ray = m_lattice_sense[d];
            int step_length = m_lattice_step[d].length;
            int x = start[0], y = start[1];
            int small_score = 0;
            double mean_score = 0, mean_small_score = 0;
            int i = 0;
            boolean one_black_pixel = false;
            for (Point p : ray) {
                int px = x + p.x, py = y + p.y;
                if (px < m_width_map.length && py < m_width_map[0].length && px >= 0 && py >= 0) {//otherwise out of bounds could happen
                    int w = m_width_map[px][py];
                    // Accept at most one 0 in a row.
                    if (w == 0) {
                        if (one_black_pixel) {
                            break;
                        } else {
                            one_black_pixel = true;
                        }
                    } else if (one_black_pixel) {
                        one_black_pixel = false;
                    }

                    if (i < step_length) {
                        path.add(new int[]{px, py, w, d % m_orientations});
                        small_score += w;
                    }
                    mean_score += w;
                    i++;
                }
            }
            mean_score /= (double) ray.length;
            mean_small_score = (double) small_score / (double) step_length;

            if ((mean_score > best_score && mean_small_score > 1) ||
                    (mean_small_score <= 1 && mean_small_score > best_small_score)) {
                best_score = mean_score;
                best_small_score = mean_small_score;
                best_path.clear();
                best_path.addAll(path);
                full_score = small_score;
                m_direction = d;
            }
        }
        // Set flag, if line orientation crosses x-axis.
        if (!best_path.isEmpty()) {
            int new_ori = best_path.get(0)[3];
            if (Math.abs(new_ori - (dir % m_orientations)) > 1) {
                m_xcross = true;
            }
        }
        if (best_small_score > 1) {
            m_points.addAll(best_path);
            m_score += full_score;
            gather(m_points.get(m_points.size() - 1), m_width_map);
        } else {
            // Eliminate empty points from end of list.
            for (int i = best_path.size() - 1; i >= 0; i--) {
                if (best_path.get(i)[2] > 0) {
                    break;
                }
                best_path.remove(i);
            }
            if (!best_path.isEmpty()) {
                int score = 0;
                for (int[] p : best_path) {
                    score += p[2];
                }
                if (score >= best_path.size()) {
                    m_points.addAll(best_path);
                    m_score += score;
                } else {
                    List<int[]> rest = new ArrayList<>();

                    score = 0;
                    for (int[] p : best_path) {
                        if (p[2] == 0) {
                            break;
                        }
                        rest.add(p);
                        score += p[2];
                    }
                    m_points.addAll(rest);
                    m_score += score;
                }
            }
        }
    }

    public void rescore() {
        m_score = 0;
        for (int[] p : m_points) {
            m_score += p[2];
        }
    }

    public int score() {
        return m_score;
    }

    public int length() {
        // TODO: Return real length?
        return m_points.size() - 1;
    }

    public List<int[]> points() {
        return m_points;
    }

    public CurvedFilament asFilament() {
        List<Point> bend_points = new ArrayList<>();
        int[] p0 = m_points.get(0);
        bend_points.add(new Point(p0[0], p0[1]));
        int ori = p0[3], abs_curv = 0, signed_curv = 0;
        for (int[] p : m_points) {
            if (p[3] != ori) {
                abs_curv += m_direction_step;
                bend_points.add(new Point(p[0], p[1]));
                if (p[3] == (ori + 1) % m_orientations) {
                    signed_curv += m_direction_step;
                } else if (ori == (p[3] + 1) % m_orientations) {
                    signed_curv -= m_direction_step;
                }
                ori = p[3];
            }
        }
        int[] tmp = m_points.get(m_points.size() - 1);
        Point p_last = new Point(tmp[0], tmp[1]);
        if (!bend_points.get(bend_points.size() - 1).equals(p_last)) {
            bend_points.add(p_last);
        }

        return new CurvedFilament(bend_points,
                Math.round(m_score * Const.MF / (double) m_points.size()),
                signed_curv, abs_curv, true);
    }

    public boolean xcross() {
        return m_xcross;
    }

    public void abandon() {
        m_points = new ArrayList<>();
        m_start = new int[4];
        m_score = 0;
    }

    public int[] remove(int index) {
        return m_points.remove(index);
    }

    public void join(Gatherer other) {
        List<int[]> other_points = other.points();
        if (Math.abs(other_points.get(0)[3] - m_points.get(0)[3]) > 1 || other.m_xcross) {
            m_xcross = true;
        }
        List<int[]> new_list = new ArrayList<>();
        for (int i = other_points.size() - 1; i > 0; i--) {
            new_list.add(other_points.get(i));
        }
        new_list.addAll(m_points);
        m_points = new_list;
        // add scores, correcting for starting point.
        m_score += other.score() - other_points.get(0)[2];
    }
}