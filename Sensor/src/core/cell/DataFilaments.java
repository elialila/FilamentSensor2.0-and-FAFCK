package core.cell;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
 *               2013-2014 Benjamin Eltzner
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


import core.image.BinaryImage;
import core.settings.Export;
import util.Annotations.NotNull;
import core.*;


import core.filaments.AbstractFilament;
import core.filaments.CurvedFilament;
import core.filaments.FilamentChain;
import filters.CloseHolesLegacy;
import ij.process.ImageProcessor;
import core.image.IBinaryImage;
import core.settings.Settings;
import core.settings.Trace;
import core.tracers.Tracer;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import java.util.Objects;
import java.util.stream.Collectors;

public class DataFilaments implements Serializable {

    private List<AbstractFilament> filaments;

    public DataFilaments() {
        filaments = new ArrayList<>();
    }


    public DataFilaments(List<AbstractFilament> filament_list) {
        filaments = (filament_list == null ? new ArrayList<>() : filament_list);
        AbstractFilament.sortList(filaments);
    }


    /**
     * Remember Parameter not implemented anymore, if you remove a filament it is gone, only rerunning the sensor will bring it back
     *
     * @param remember
     */
    public void cleanTracedFilaments(boolean remember) {
        if (filaments == null || filaments.isEmpty()) {
            return;
        }
        setFilaments(new ArrayList<>(getFilaments().stream().filter(fil -> fil.isKeep()).collect(Collectors.toList())));
        AbstractFilament.sortList(filaments);
    }

    public void keepDiscardAllFilaments() {
        if (getFilaments() == null || getFilaments().isEmpty()) {
            return;
        }
        //flip keep property of first filament and set all filaments to that keep-state
        boolean keep = !getFilaments().get(0).isKeep();
        for (AbstractFilament filament : getFilaments()) {
            if (filament instanceof FilamentChain) {
                ((FilamentChain) filament).setKeepAll(keep);
            } else {
                filament.setKeep(keep);
            }
        }
    }


    /**
     * Mask can be null, if it's null skip all the stuff which uses mask
     *
     * @param binary_image
     * @param mask
     * @param parameters
     * @return
     * @throws Exception
     */
    public IBinaryImage scanFilaments(ImageProcessor binary_image, IBinaryImage mask, Settings parameters, @NotNull Tracer tracer) {
        Objects.requireNonNull(tracer, "Tracer==null");

        cleanTracedFilaments(false);
        keepDiscardAllFilaments();
        cleanTracedFilaments(false);
        if (mask != null && parameters.getValue(Trace.no_boundary) != 0) {
            mask = mask.clone();//use a clone of mask, because we don't want to change the original
            mask.exitMemoryState();//we use a clone - no need for entering memory state again
        }

        List<AbstractFilament> fils = tracer.scanFilaments(binary_image,
                parameters.getValue(Trace.tolerance) / 100.0,
                parameters.getValue(Trace.minlen),
                parameters.getValue(Trace.minangle),
                parameters.getValue(Trace.step));

        boolean[][] binImage = new BinaryImage(binary_image.getIntArray(), 254).toBoolean();
        tracer.updateFilamentWidth(binImage);//update the width of the filaments to make it mostly independent from tolerance


        if (parameters.getValue(Trace.no_boundary) != 0 && mask != null) {
            IBinaryImage altMask = tracer.getPixelMask();
            altMask.dilate(Calc.circleMaskBinary(50));
            altMask.erode(Calc.circleMaskBinary(30));
            CloseHolesLegacy.apply(altMask, 0.01);
            mask.erode(Calc.circleMaskBinary(30));
            final int image_width = altMask.getWidth(), height = altMask.getHeight();
            for (int x = 0; x < image_width; x++) {
                for (int y = 0; y < height; y++) {
                    if (altMask.getPixel(x, y)) mask.setPixel(x, y);
                }
            }
            for (int i = fils.size() - 1; i >= 0; i--) {
                int counter = 0;
                List<Point> points = fils.get(i).getPoints();
                for (Point p : points) {
                    if (!mask.getPixel(p.x, p.y)) {
                        counter++;
                    }
                }
                if (counter > points.size() / 2) {
                    long width = fils.remove(i).getWidth();
                    if (counter < points.size() - 1) {
                        for (int j = points.size() - 1; j > 0; j--) {
                            Point p = points.get(j);
                            if (mask.getPixel(p.x, p.y)) {
                                break;
                            }
                            points.remove(j);
                        }
                        while (true) {
                            Point p = points.get(0);
                            if (mask.getPixel(p.x, p.y)) {
                                break;
                            }
                            points.remove(0);
                        }
                        fils.add(i, new CurvedFilament(points, width));
                    }
                }
            }
        }


        setFilaments(fils);

        return mask;
    }


    public void splitToLinear(int max_size) {
        List<AbstractFilament> new_list = new ArrayList<>();
        for (AbstractFilament f : getFilaments()) {
            new_list.addAll(f.splitIntoLinearPieces(max_size));
        }
        setFilaments(new_list);
    }

    public int count() {
        return getFilaments().size();
    }

    public void clearData() {
        getFilaments().clear();
    }

    public void setFilaments(List<AbstractFilament> filaments) {
        this.filaments = filaments;
    }

    public List<AbstractFilament> getFilaments() {
        return filaments;
    }


    public List<AbstractFilament> getFilteredFilaments(Settings dp) {
        boolean hideNonVerifiedFibers = dp.getValueAsBoolean(Export.hideNonVerifiedFibers);
        boolean hideSingleVerifiedFibers = dp.getValueAsBoolean(Export.hideSingleVerifiedFibers);
        boolean hideMultiVerifiedFibers = dp.getValueAsBoolean(Export.hideMultiVerifiedFibers);

        List<AbstractFilament> result = filaments;

        if (filaments.stream().noneMatch(fil -> fil.getVerifier() != null)) return filaments;

        if (hideSingleVerifiedFibers) {
            result = result.stream().filter(fil -> fil.getVerifier() != null && fil.getVerifier().getId().size() > 1).collect(Collectors.toList());
        }

        if (hideMultiVerifiedFibers) {
            result = result.stream().filter(fil -> fil.getVerifier() != null && fil.getVerifier().getId().size() < 2).collect(Collectors.toList());
        }

        if (!hideNonVerifiedFibers && (hideSingleVerifiedFibers || hideMultiVerifiedFibers)) {
            result.addAll(filaments.stream().filter(fil -> fil.getVerifier() == null).collect(Collectors.toList()));//add the non verified again
        }

        if (hideNonVerifiedFibers) {
            result = result.stream().filter(fil -> fil.getVerifier() != null).collect(Collectors.toList());
        }


        return result;
    }


}
