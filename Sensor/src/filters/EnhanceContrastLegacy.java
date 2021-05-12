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


public class EnhanceContrastLegacy {

    private int m_black, m_white;

    public EnhanceContrastLegacy(int black, int white) {
        m_black = black;
        m_white = white;
    }

    public int[][] apply(int[][] image) {
        double bw_scale = 255 / ((double) (m_white - m_black));
        int width = image.length;
        int height = image[0].length;
        int[][] out = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = image[i][j];
                pixel = (int) Math.round(bw_scale * (pixel - m_black));
                pixel = (pixel < 0 ? 0 : pixel);
                pixel = (pixel > 255 ? 255 : pixel);

                out[i][j] = pixel;
            }
        }
        return out;
    }
}
