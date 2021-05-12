package core;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2014 Benjamin Eltzner, Patricia Burger
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


import java.util.*;

/**
 * Convenience class that provides simple container classes.
 */
public final class Misc {

    private Misc() {
    } // No instances.

    public static final class Int1D {
        public int[] the;

        public Int1D(int[] in) {
            the = in;
        }

        @Override
        public String toString() {
            return Arrays.toString(the);
        }
    }

    public static final class Int2D {
        public int[][] the;

        public Int2D(int[][] in) {
            the = in;
        }

        @Override
        public String toString() {
            return Arrays.toString(the);
        }
    }

    public static final class Int3D {
        public int[][][] the;

        public Int3D(int[][][] in) {
            the = in;
        }

        @Override
        public String toString() {
            return Arrays.toString(the);
        }
    }

    public static final class Double1D {
        public double[] the;

        public Double1D(double[] in) {
            the = in;
        }

        @Override
        public String toString() {
            return Arrays.toString(the);
        }
    }

    public static final class Double2D {
        public double[][] the;

        public Double2D(double[][] in) {
            the = in;
        }

        @Override
        public String toString() {
            return Arrays.toString(the);
        }
    }

    public static final class Numbered<T> {
        private T value;
        private int key;

        public Numbered(T object, int key) {
            this.key = key;
            this.value = object;
        }

        public int key() {
            return key;
        }

        public T get() {
            return value;
        }

        @Override
        public String toString() {
            return "<" + key + ", " + value.toString() + ">";
        }
    }

    public final static <T> boolean addUnique(List<T> list, T item) {
        if (!list.contains(item)) {
            return list.add(item);
        }
        return false;
    }

    public static final <T> void addAllUnique(List<T> list, List<T> to_add) {
        for (T item : to_add) {
            if (!list.contains(item)) {
                list.add(item);
            }
        }
    }

    public static final <A, B> boolean addUnique(Map<A, List<B>> map, A key, B item) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        return addUnique(map.get(key), item);
    }


    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }


}
