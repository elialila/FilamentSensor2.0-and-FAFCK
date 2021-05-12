package filters;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2016 Benjamin Eltzner
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


import core.image.IBinaryImage;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CloseHolesLegacy {

    private CloseHolesLegacy() {
    }

    public static void apply(IBinaryImage image, double ratio) {
        Objects.requireNonNull(image, "FilterCloseHoles::apply() --- Image must exist.");
        if (image.getWidth() == 0 || image.getHeight() == 0) {
            throw new IllegalArgumentException("FilterCloseHoles::apply() --- Image must not be empty.");
        }

        final int width = image.getWidth();
        final int height = image.getHeight();

        int count = image.getPixelSetCount();

        final int[][] map = new int[width][height];
        final Map<Integer, List<Point>> fragments = new HashMap<>();
        int index = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!image.getPixel(x, y)) {
                    if (x > 0 && !image.getPixel(x - 1, y)) {
                        // If left neighbor pixel is also black
                        final int label = map[x][y] = map[x - 1][y];
                        fragments.get(map[x][y])
                                .add(new Point(x, y));
                        if (y > 0 && label != map[x][y - 1] && fragments.containsKey(map[x][y - 1])) {
                            final List<Point> to_dissolve = fragments.remove(map[x][y - 1]);
                            for (final Point p : to_dissolve) {
                                map[p.x][p.y] = label;
                            }
                            fragments.get(map[x][y])
                                    .addAll(to_dissolve);
                        }
                    } else if (y > 0 && !image.getPixel(x, y - 1)) {
                        map[x][y] = map[x][y - 1];
                        fragments.get(map[x][y])
                                .add(new Point(x, y));
                    } else {
                        // If the pixel's left and lower neighbor are white
                        // or we are at the left or lower margin,
                        // create new fragment and add this point to it.
                        fragments.put(++index, new ArrayList<>());
                        map[x][y] = index;
                        fragments.get(map[x][y])
                                .add(new Point(x, y));
                    }
                }
            }
        }

        int max_size = (int) Math.min(count, ratio * width * height);
        for (final List<Point> fragment : fragments.values()) {
            // only fill holes that are at most 10% of the image!
            if (fragment.size() < max_size) {
                for (final Point p : fragment) {
                    image.setPixel(p.x, p.y);
                }
            }
        }
    }
}
