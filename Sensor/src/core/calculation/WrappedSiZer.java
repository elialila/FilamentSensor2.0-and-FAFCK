package core.calculation;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2015 Benjamin Eltzner
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
import core.settings.WiZer;
import util.ImageFactory;
import util.Pair;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class WrappedSiZer {

    private double[][] m_data;
    private double m_quantile_term, m_binning_step;
    private int m_bins;
    private static final int RISING = 0xFF0000FF, FALLING = 0xFFFF0000, INDETERMINATE = 0xFFAAAAAA,
            SPARSE = 0xFFFFFFFF, WHITE = 0xFFFFFFFF, BLACK = 0xFF000000,
            MAXIMA = RISING, MINIMA = FALLING;
    public static final int LINES = 0, EXPERIMENTAL = 1, PIXELS = 2;

    public WrappedSiZer(List<AbstractFilament> data) {
        m_data = new double[data.size()][2];
        for (int i = 0; i < data.size(); i++) {
            AbstractFilament filament = data.get(i);
            m_data[i] = new double[]{filament.getOrientation() / Const.MF, filament.getMass()};
        }
    }

    public BufferedImage[] calculateWiZer(int bins, int sigmas, double quantile, Settings parameters) {
        m_bins = bins;
        m_binning_step = 180. / m_bins;
        m_quantile_term = -2 * Math.log(2 * Math.PI * quantile);
        int pad_top = 30,
                pad_bottom = 130,
                pad_left = 150,
                pad_right = 50;

        double[] histogram = new double[m_bins];
        double histogram_sum = 0.0;
        for (double[] line : m_data) {
            int bin = (int) Math.round(line[0] * m_bins / 180.);
            histogram[bin] += line[1];
            histogram_sum += line[1];
        }

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= histogram_sum;
        }

        double n_filaments = -1;
        int type = parameters.getValue(WiZer.sample_size);
        switch (type) {
            case LINES:
                n_filaments = m_data.length;
                break;
            case EXPERIMENTAL:
                n_filaments = 0.1 * histogram_sum;
                break;
            case PIXELS:
                n_filaments = histogram_sum;
                break;
            default:
                System.err.println("WrappedSiZer.calculateWiZer(): Unknown type " + type);
        }

        double sigma = m_binning_step;
        int[][] image = new int[2][(bins + pad_left + pad_right) * (sigmas + pad_bottom + pad_top)];
        for (int y = 0; y < sigmas + pad_top + pad_bottom; y++) {
            for (int x = 0; x < bins + pad_left + pad_right; x++) {
                if (x == pad_left && y > pad_top - 1 && y < sigmas + pad_top) {
                    x = pad_left + bins;
                }
                image[0][(bins + pad_left + pad_right) * y + x] = WHITE;
                image[1][(bins + pad_left + pad_right) * y + x] = WHITE;
            }

        }
        for (int x = pad_left - 1; x < bins + pad_left + 1; x++) {
            image[0][(bins + pad_left + pad_right) * (pad_top - 1) + x] = BLACK;
            image[0][(bins + pad_left + pad_right) * (pad_top + sigmas) + x] = BLACK;
            image[1][(bins + pad_left + pad_right) * (pad_top - 1) + x] = BLACK;
            image[1][(bins + pad_left + pad_right) * (pad_top + sigmas) + x] = BLACK;
        }
        for (int y = pad_top - 1; y < sigmas + pad_top + 1; y++) {
            image[0][(bins + pad_left + pad_right) * y + (pad_left - 1)] = BLACK;
            image[0][(bins + pad_left + pad_right) * y + (pad_left + bins)] = BLACK;
            image[1][(bins + pad_left + pad_right) * y + (pad_left - 1)] = BLACK;
            image[1][(bins + pad_left + pad_right) * y + (pad_left + bins)] = BLACK;
        }
        List<Pair<String, Integer>> labels = new ArrayList<>();
        if (parameters.getValue(WiZer.log_view) == 0) {
            double sigma_step = (90. - sigma) / (sigmas - 1);
            int[] label_numbers = new int[]{15, 30, 45, 60, 75, 90};
            int label_index = 0;
            int label = label_numbers[label_index];

            for (int i = 0; i < sigmas; i++) {
                if (Math.abs(sigma - label) <= 0.5 * sigma_step) {
                    labels.add(new Pair<>("" + label, i + pad_bottom));
                    label_index++;
                    label = (label_index < label_numbers.length ? label_numbers[label_index] : 0);
                    int a = (bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i);
                    image[0][a + (pad_left - 2)] = BLACK;
                    image[0][a + (pad_left - 3)] = BLACK;
                    image[0][a + (pad_left - 4)] = BLACK;
                    image[1][a + (pad_left - 2)] = BLACK;
                    image[1][a + (pad_left - 3)] = BLACK;
                    image[1][a + (pad_left - 4)] = BLACK;
                }

                int[][] tmp = singleWiZer(histogram, sigma, n_filaments);
                for (int j = 0; j < bins; j++) {
                    image[0][(bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i) +
                            j + pad_left] = tmp[0][j];
                    image[1][(bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i) +
                            j + pad_left] = tmp[1][j];
                }
                sigma += sigma_step;
            }
        } else {
            double sigma_factor = Math.pow(90. / sigma, 1. / (sigmas - 1));
            int[] label_numbers = new int[]{3, 10, 30, 100, 300, 900};
            int label_index = 0;
            int label = label_numbers[label_index];

            for (int i = 0; i < sigmas; i++) {
                if (Math.abs(sigma - 0.1 * label) <= 0.5 * (sigma_factor - 1) * sigma) {
                    if (label % 10 != 0) {
                        labels.add(new Pair<>((label / 10) + "." + (label % 10), i + pad_bottom));
                    } else {
                        labels.add(new Pair<>("" + (label / 10), i + pad_bottom));
                    }
                    label_index++;
                    label = (label_index < label_numbers.length ? label_numbers[label_index] : 0);
                    int a = (bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i);
                    image[0][a + (pad_left - 2)] = BLACK;
                    image[0][a + (pad_left - 3)] = BLACK;
                    image[0][a + (pad_left - 4)] = BLACK;
                    image[1][a + (pad_left - 2)] = BLACK;
                    image[1][a + (pad_left - 3)] = BLACK;
                    image[1][a + (pad_left - 4)] = BLACK;
                }

                int[][] tmp = singleWiZer(histogram, sigma, n_filaments);
                for (int j = 0; j < bins; j++) {
                    image[0][(bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i) +
                            j + pad_left] = tmp[0][j];
                    image[1][(bins + pad_left + pad_right) * (sigmas + pad_top - 1 - i) +
                            j + pad_left] = tmp[1][j];
                }
                sigma *= sigma_factor;
            }
        }
        for (int i = 0; i < 181; i += 30) {
            int a = (bins + pad_left + pad_right),
                    b = pad_left + (int) Math.round(i / m_binning_step);
            image[0][a * (pad_top + sigmas + 1) + b] = BLACK;
            image[0][a * (pad_top + sigmas + 2) + b] = BLACK;
            image[0][a * (pad_top + sigmas + 3) + b] = BLACK;
            image[1][a * (pad_top + sigmas + 1) + b] = BLACK;
            image[1][a * (pad_top + sigmas + 2) + b] = BLACK;
            image[1][a * (pad_top + sigmas + 3) + b] = BLACK;
        }
        BufferedImage wizer_image = ImageFactory.makeCustomImage(image[0], bins + pad_left + pad_right);
        BufferedImage mode_image = ImageFactory.makeCustomImage(image[1], bins + pad_left + pad_right);
        Graphics2D canvas = wizer_image.createGraphics();
        makeAxesLabels(canvas, sigmas, labels, pad_top, pad_left, pad_bottom, pad_right, bins);
        canvas = mode_image.createGraphics();
        makeAxesLabels(canvas, sigmas, labels, pad_top, pad_left, pad_bottom, pad_right, bins);
        return new BufferedImage[]{wizer_image, mode_image};
    }

    private void makeAxesLabels(Graphics2D canvas, int sigmas, List<Pair<String, Integer>> labels,
                                int pad_top, int pad_left, int pad_bottom, int pad_right, int bins) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(canvas.getFont().deriveFont(2.5F * canvas.getFontMetrics().getHeight()));
        int label_height = canvas.getFontMetrics().getHeight() / 2;
        for (Pair<String, Integer> label : labels) {
            String l = label.getKey();
            int label_width = canvas.getFontMetrics().stringWidth(l);
            canvas.drawString(l, pad_left - 5 - label_width,
                    sigmas + pad_top + pad_bottom - 10 + label_height - label.getValue());
        }
        for (int i = 0; i < 181; i += 30) {
            int label_width = canvas.getFontMetrics().stringWidth("" + i) / 2;
            canvas.drawString("" + i, pad_left + (int) Math.round(i / m_binning_step) - label_width,
                    sigmas + pad_top + 4 + 2 * label_height);
        }
        canvas.setFont(canvas.getFont().deriveFont(canvas.getFontMetrics().getHeight() + 5F));
        AffineTransform orig = canvas.getTransform();
        canvas.rotate(-Math.PI / 2);
        String y_label = "Bandwidth [°]";
        int label_width = canvas.getFontMetrics().stringWidth(y_label) / 2;
        canvas.drawString(y_label, -sigmas / 2 - pad_top - label_width, 10 + 2 * label_height);
        canvas.setTransform(orig);
        String x_label = "Orientation [°]";
        label_width = canvas.getFontMetrics().stringWidth(x_label) / 2;
        canvas.drawString(x_label, bins / 2 + pad_left - label_width,
                sigmas + pad_top + pad_bottom - 11);
        canvas.dispose();
    }

    /**
     * Data array has shape nx2. For each line, it contains orientation and mass.
     */
    private int[][] singleWiZer(double[] histogram, double sigma, double n_filaments) {
        final int smear_range = (int) Math.round(4 * sigma / m_binning_step);

        // Convolve histogram (f) with Gaussian derivative (D_g).
        double[] gaussian_derivative = Calc.gaussianDerivative(smear_range, sigma, m_binning_step);
        double[] gaussian_2nd_derivative = Calc.gaussianDerivative2(smear_range, sigma, m_binning_step);

        /*
         * The smeared histogram is D_{Gauss} f (x)
         * 
         * The threshold is calculated following chapter 3.1.1 in
         * P. Chaudhuri & J. S. Marron
         * SiZer for Exploration of Structures in Curves
         * Journal of the American Statistical Association
         * Volume 94, Issue 447 (1999)
         * where we use the Gaussian kinematic formula.
         * 
         * For the calculation of the estimate for the Gaussian kinematic
         * formula, we need quadratic terms in derivatives of the kernel.
         * Derivatives are denoted here as d1, d2, d11, d12, d22.
         */
        double[] deriv_histogram = new double[m_bins];
        double threshold = 0.0, sum_std_dev_der = 0.0;
        for (int i = 0; i < m_bins; i++) {
            int i2 = m_bins + i - (smear_range % m_bins);
            double d1 = 0.0, d2 = 0.0, d11 = 0.0, d12 = 0.0, d22 = 0.0;
            for (int j = 0; j < 2 * smear_range + 1; j++) {
                int k = (i2 + j) % m_bins;
                if (histogram[k] != 0) {
                    d1 += histogram[k] * gaussian_derivative[j];
                    d2 += histogram[k] * gaussian_2nd_derivative[j];
                    d11 += histogram[k] * gaussian_derivative[j] *
                            gaussian_derivative[j];
                    d12 += histogram[k] * gaussian_derivative[j] *
                            gaussian_2nd_derivative[j];
                    d22 += histogram[k] * gaussian_2nd_derivative[j] *
                            gaussian_2nd_derivative[j];
                }
            }
            deriv_histogram[i] = d1;
            if (d12 > 0 && d11 <= 0) {
                System.err.println("Strange things happen.");
            }
            if (d11 > 0) {
                d11 = d11 - d1 * d1;
                d12 = d12 - d1 * d2;
                d22 = d22 - d2 * d2;
                sum_std_dev_der += Math.sqrt(Math.max(0, d11 * d22 - d12 * d12)) / d11;
                if (d11 > threshold * threshold) {
                    threshold = Math.sqrt(d11);
                }
            }
        }

        // Calculate estimated effective sample size and thresholds
        // 2 * sum_std_dev_der due to absolute value of process.
        threshold *= Math.sqrt((2 * Math.log(2 * sum_std_dev_der) + m_quantile_term) / n_filaments);
        double[] gaussian = Calc.gaussian1d(smear_range, sigma, n_filaments, m_binning_step);
        double[] smeared_histogram = new double[m_bins];
        int[][] colors = new int[2][m_bins];
        for (int i = 0; i < m_bins; i++) {
            int i2 = m_bins + i - (smear_range % m_bins);
            for (int j = 0; j < 2 * smear_range + 1; j++) {
                smeared_histogram[i] += histogram[(i2 + j) % m_bins] * gaussian[j];
            }
            if (smeared_histogram[i] < 5) {
                deriv_histogram[i] = 0;
                colors[0][i] = SPARSE;
            }
        }

        for (int i = 0; i < deriv_histogram.length; i++) {
            if (colors[0][i] == SPARSE) {
                continue;
            }
            if (deriv_histogram[i] > threshold) {
                colors[0][i] = RISING;
            } else if (deriv_histogram[i] < -threshold) {
                colors[0][i] = FALLING;
            } else {
                colors[0][i] = INDETERMINATE;
            }
        }

        // Identify intervals in which extrema must lie.
        colors[1] = getExtrema(colors[0], deriv_histogram.length, smeared_histogram);

        return colors;
    }

    private static int[] getExtrema(int[] slopes, int n, double[] smeared_histogram) {
        // Identify intervals in which extrema must lie.
        List<Integer[]> maxima_regions = new ArrayList<>(), minima_regions = new ArrayList<>();
        boolean rising_slope = false, falling_slope = false;
        int peak_of_rising_slope = 0, crest_of_falling_slope = 0;
        for (int i = 0; i < n; i++) {
            if (slopes[i] == RISING) {
                if (falling_slope) {
                    falling_slope = false;
                    minima_regions.add(new Integer[]{crest_of_falling_slope, i});
                }
                rising_slope = true;
                peak_of_rising_slope = i;
            } else if (slopes[i] == FALLING) {
                if (rising_slope) {
                    rising_slope = false;
                    maxima_regions.add(new Integer[]{peak_of_rising_slope, i});
                }
                falling_slope = true;
                crest_of_falling_slope = i;
            }
        }

        if (rising_slope && slopes[0] != RISING) {
            for (int i = 0; i < n; i++) {
                if (slopes[i] == RISING) {
                    rising_slope = false;
                    break;
                }
                if (slopes[i] == FALLING && rising_slope) {
                    rising_slope = false;
                    maxima_regions.add(new Integer[]{peak_of_rising_slope, n + i});
                    break;
                }
            }
        }
        if (falling_slope && slopes[0] != FALLING) {
            for (int i = 0; i < n; i++) {
                if (slopes[i] == FALLING) {
                    falling_slope = false;
                    break;
                }
                if (slopes[i] == RISING && falling_slope) {
                    falling_slope = false;
                    minima_regions.add(new Integer[]{crest_of_falling_slope, n + i});
                    break;
                }
            }
        }

        int[] extrema = new int[slopes.length];
        for (int k = 0; k < extrema.length; k++) {
            extrema[k] = WHITE;
        }
        for (int k = 0; k < maxima_regions.size(); k++) {
            Integer[] max_pair = maxima_regions.get(k), min_pair = minima_regions.get(k);
            int max_position = -1, min_position = -1;
            double max_value = 0, min_value = Double.MAX_VALUE;
            for (int i = max_pair[0]; i < max_pair[1] + 1; i++) {
                int j = i % n;
                if (smeared_histogram[j] > max_value) {
                    max_value = smeared_histogram[j];
                    max_position = j;
                }
            }
            extrema[max_position] = MAXIMA;
            for (int i = min_pair[0]; i < min_pair[1] + 1; i++) {
                int j = i % n;
                if (smeared_histogram[j] < min_value) {
                    min_value = smeared_histogram[j];
                    min_position = j;
                }
            }
            extrema[min_position] = MINIMA;
        }
        return extrema;
    }
}
