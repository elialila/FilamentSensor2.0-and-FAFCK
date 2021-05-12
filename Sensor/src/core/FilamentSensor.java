package core;
/*
 * FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
 *               2013-2014 Patricia Burger
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

import java.util.function.Consumer;

public abstract class FilamentSensor {
    public static final String NAME = "FilamentSensor", VERSION = "0.2.2j";

    public static boolean DEBUG, VERBOSE, ERROR, PERFORMANCE;

    private static Consumer<String> messageStream;
    private static Consumer<String> debugStream;
    private static Consumer<String> errorStream;

    public static void setMessageStream(Consumer<String> stream) {
        messageStream = stream;
    }

    public static void setDebugStream(Consumer<String> stream) {
        debugStream = stream;
    }

    public static void setErrorStream(Consumer<String> stream) {
        errorStream = stream;
    }


    public static boolean DEBUG() {
        return DEBUG;
    }

    public static boolean VERBOSE() {
        return VERBOSE;
    }

    public static void debugMessage(String message) {
        if (DEBUG) {
            if (debugStream != null) debugStream.accept(message);
            else
                System.out.println(message);
        }
    }

    public static void debugError(String message) {
        if (ERROR) {
            if (errorStream != null) errorStream.accept(message);
            else
                System.err.println(message);
        }
    }

    public static void verboseMessage(String message) {
        if (VERBOSE) {
            if (messageStream != null) messageStream.accept(message);
            else
                System.out.println(message);
        }
    }

    public static void debugPerformance(String text, long timeStart) {
        if (PERFORMANCE) {
            String message = text + ":" + (((double) System.currentTimeMillis() - timeStart) / 1000) + "s";
            if (debugStream != null) debugStream.accept(message);
            else
                System.out.println(message);
        }
    }

}
