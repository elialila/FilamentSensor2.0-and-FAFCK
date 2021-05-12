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
import core.Const;

public class HistogramMethodsLegacy {

    public static final int OTSU = 0;
    public static final int LI = 1;
    public static final int TRIANGLE = 2;

    private boolean[][] m_binary_image;
    private int[] m_brightness_histogram;
    private int m_width;
    private int m_height;
    private int m_threshold;
    private int m_black;
    private int m_white;

    public HistogramMethodsLegacy(final int[][] int_image,
                                  final boolean make_images,
                                  final int type) {
        this(int_image, make_images, 0, type);
    }

    /**
     * This is the constructor to obtain the Otsu binarized image.
     */
    public HistogramMethodsLegacy(final int[][] int_image,
                                  final boolean make_images,
                                  final int lower_bound,
                                  final int type) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            m_threshold = -1;
            return;
        }

        m_width = int_image.length;
        m_height = int_image[0].length;

        final int[] range = Calc.getRange(int_image);
        m_brightness_histogram = makeHistogram(int_image, range[0], range[1]);

        int number_of_pixels = m_width * m_height;
        if (lower_bound > 0) {
            number_of_pixels = 0;
            for (int i = lower_bound; i < 256; i++) {
                number_of_pixels += m_brightness_histogram[i];
            }
            m_brightness_histogram[0] = number_of_pixels;
            for (int i = 1; i < lower_bound; i++) {
                m_brightness_histogram[i] = 0;
            }
            number_of_pixels *= 2;
        }

        m_threshold = calcThreshold(number_of_pixels, 0, 255, type);

        if (make_images) {
            makeImages(int_image);
        }
    }

    /**
     * MAY CHANGE INPUT int_image!
     */
    private int[] makeHistogram(final int[][] int_image,
                                final int min,
                                final int max) {
        final int[] histogram = new int[256];
        final double scale = 255 / ((double) (max - min));

        for (int i = 0; i < m_width; i++) {
            for (int j = 0; j < m_height; j++) {
                if (min != 0 || max != 255) {
                    int_image[i][j] = (int_image[i][j] < min ? 0 :
                            (int_image[i][j] > max ? 255 :
                                    (int) Math.round(scale * (int_image[i][j] - min))));
                }
                histogram[int_image[i][j]]++;
            }
        }

        return histogram;
    }

    private int calcThreshold(int number_of_pixels,
                              int min,
                              int max,
                              int type) {
        long sum_below = 0, sum_above = 0;
        double goodness_max = 0.0;

        long n_below = 0;
        long n_above = number_of_pixels;
        int thresh = 0;

        for (int i = min; i < max + 1; i++) {
            sum_above += (i + 1) * m_brightness_histogram[i];
        }

        switch (type) {
            case OTSU:
                for (int t = min; t < max; t++) {
                    n_below += m_brightness_histogram[t];
                    n_above -= m_brightness_histogram[t];

                    sum_below += (t + 1) * m_brightness_histogram[t];  // including t
                    sum_above -= (t + 1) * m_brightness_histogram[t];  // excluding t

                    final double goodness = ((n_below * n_above) <= 0 ? 0 :
                            (double) (n_below * sum_above - n_above * sum_below) *
                                    (double) (n_below * sum_above - n_above * sum_below) /
                                    (double) (n_below * n_above));

                    if (goodness > goodness_max) {
                        goodness_max = goodness;
                        thresh = t;
                    }
                }
                break;
            case LI:
                final double[] mean_below = new double[max - min];
                final double[] mean_above = new double[max - min];
                for (int t = min; t < max; t++) {
                    n_below += m_brightness_histogram[t];
                    n_above -= m_brightness_histogram[t];

                    sum_below += (t + 1) * m_brightness_histogram[t];  // including t
                    sum_above -= (t + 1) * m_brightness_histogram[t];  // excluding t

                    mean_below[t - min] = (n_below == 0 ? 0 : sum_below / (double) n_below);
                    mean_above[t - min] = (n_above == 0 ? (t == min ? max : mean_above[t - 1 - min]) :
                            sum_above / (double) n_above);
                }

                int t_old = -1;
                int t_new = 1;
                while (t_old != t_new) {
                    t_old = t_new;
                    t_new = (Math.log(mean_below[t_old]) <= 0 ?
                            (int) Math.round(mean_above[t_old] / Math.log(mean_above[t_old])) :
                            (int) Math.round((mean_below[t_old] - mean_above[t_old]) /
                                    (Math.log(mean_below[t_old]) - Math.log(mean_above[t_old]))));
                }
                thresh = t_new - 1;
                break;
            case TRIANGLE:
                int max_value = 0;
                int max_pos = 0;
                int min_pos = 0;

                for (int i = m_brightness_histogram.length - 1; i > 0; i--) {
                    if (m_brightness_histogram[i] > 0) {
                        min_pos = i;
                        break;
                    }
                }
                if (min_pos < m_brightness_histogram.length - 1) {
                    min_pos++;
                }

                for (int i = 0; i <= min_pos; i++) {
                    if (m_brightness_histogram[i] > max_value) {
                        max_pos = i;
                        max_value = m_brightness_histogram[i];
                    }
                }

                if (min_pos == max_pos) {
                    return min_pos;
                }

                // use up-right pointing normal to the connecting line of max and min
                final int normal_x = m_brightness_histogram[max_pos];
                final int normal_y = min_pos - max_pos;
                int min_intercept = Integer.MAX_VALUE;
                thresh = min_pos;
                for (int i = max_pos; i < min_pos; i++) {
                    final int intercept = normal_x * i + normal_y * m_brightness_histogram[i];
                    // Lowest scalar product with normal gives the line parallel to
                    // the min-max-connection which is furthest below it.
                    if (intercept < min_intercept) {
                        thresh = i;
                        min_intercept = intercept;
                    }
                }
                thresh--;
                break;
            default:
                thresh = 0;
        }

        return thresh;
    }

    private void makeImages(final int[][] int_image) {
        m_binary_image = new boolean[m_width][m_height];
        final int[][] m_truncated_image = new int[m_width][m_height];

        for (int i = 0; i < m_width; i++) {
            for (int j = 0; j < m_height; j++) {
                m_binary_image[i][j] = int_image[i][j] > m_threshold;
                m_truncated_image[i][j] = (int_image[i][j] > m_threshold ? int_image[i][j] : 0);
            }
        }

        // clean single pixels
        for (int i = 1; i < m_width - 1; i++) {
            for (int j = 1; j < m_height - 1; j++) {
                if (m_binary_image[i][j] && !m_binary_image[i - 1][j] &&
                        !m_binary_image[i][j - 1] && !m_binary_image[i + 1][j] &&
                        !m_binary_image[i][j + 1]) {
                    m_binary_image[i][j] = false;
                    m_truncated_image[i][j] = 0;
                }
            }
        }
    }

    /**
     * Idea of contrast enhancement:
     * 1. Split brightness histogram at OTSU threshold.
     * 2. Let 10% of the dark part have brightness 0.
     * 3. Let 1% of the bright part have brightness 255.
     */
    public HistogramMethodsLegacy(final int[][] int_image,
                                  final int min,
                                  final int max) {
        if (int_image == null || int_image.length <= 0 || int_image[0].length <= 0) {
            m_threshold = -1;
            return;
        }

        m_width = int_image.length;
        m_height = int_image[0].length;

        m_brightness_histogram = rawHistogram(int_image, min, max);

        // Get Otsu m_threshold.
        int otsu_threshold = calcThreshold(m_width * m_height, 0, max - min, OTSU);
        int above_threshold = 0;
        int below_threshold = 0;

        for (int i = 0; i < max - min + 1; i++) {
            if (i < otsu_threshold) {
                below_threshold += m_brightness_histogram[i];
            } else {
                above_threshold += m_brightness_histogram[i];
            }
        }

        int n_over = 0;
        int n_under = 0;
        for (int k = 0; k < max - min + 1; k++) {
            if (100 * n_over < above_threshold) {
                n_over += m_brightness_histogram[max - min - k];
                m_white = max - k;
            }
            if (10 * n_under < below_threshold) {
                n_under += m_brightness_histogram[k];
                m_black = min + k;
            }
        }
    }

    private int[] rawHistogram(int[][] int_image,
                               int min,
                               int max) {
        int[] histogram = new int[max - min + 1];

        for (int x = 0; x < m_width; x++) {
            for (int y = 0; y < m_height; y++) {
                histogram[int_image[x][y] - min]++;
            }
        }
        return histogram;
    }

    public boolean[][] binaryImage() {
        return m_binary_image;
    }

    public int black() {
        return m_black;
    }

    public static int[][] enhanceContrast(final int[][] image,
                                          final int black,
                                          final int white) {
        final EnhanceContrastLegacy contrastFilter = new EnhanceContrastLegacy(black, white);
        return contrastFilter.apply(image);
    }

    public int threshold() {
        return m_threshold;
    }

    public int white() {
        return m_white;
    }

    @Deprecated
    private void calcBlackWhite(final int bright_pixels) {
        m_black = 0;
        m_white = 255;
        int step = bright_pixels / Const.CONTRAST_FACTOR;
        step = (step > 10 ? 10 : step);

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 255; i++) {
            /*if (FilamentSensor.VERBOSE()) {
                if ((i - 1) % 10 == 0) {
                    LOG.debug("Histogram " + i + "-" + (i + 9) + ": ");
                }

                sb.append(m_brightness_histogram[i]);
                if (i % 10 == 0 || i == 254) {
                    LOG.debug(sb.toString());
                    // reset string
                    sb = new StringBuilder();
                } else {
                    sb.append(", ");
                }
            }*/

            // Compensate for bright disturbances.
            if (m_brightness_histogram[i] > m_brightness_histogram[i + 1] + step &&
                    m_brightness_histogram[i + 1] != 0) {
                m_white = i + 1;
            }

            // Compensate for bright background.
            if (m_brightness_histogram[254 - i] > m_brightness_histogram[255 - i] + 400 * step &&
                    m_brightness_histogram[255 - i] != 0) {
                m_black = 254 - i;
            }
        }
    }
}
