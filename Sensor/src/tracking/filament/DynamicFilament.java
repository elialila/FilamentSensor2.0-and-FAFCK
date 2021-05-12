package tracking.filament;

import core.filaments.AbstractFilament;
import core.filaments.FilamentChain;
import javafx.beans.property.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2019 Benjamin Eltzner
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

public class DynamicFilament implements Serializable {
    //switched from chain to abstract filament 2020-10-07
    private Map<Integer, AbstractFilament> filaments;
    //properties don't get set
    private transient BooleanProperty keep;
    private transient DoubleProperty length;
    private transient IntegerProperty birth;
    private transient IntegerProperty death;
    private transient BooleanProperty selected;

    private DynamicFilament() {
        birth = new SimpleIntegerProperty();
        death = new SimpleIntegerProperty();
        length = new SimpleDoubleProperty();
        keep = new SimpleBooleanProperty(true);
        selected = new SimpleBooleanProperty(false);
    }

    public DynamicFilament(int birth,
                           FilamentChain initial) {
        this();
        filaments = new HashMap<>();
        add(birth, initial);
    }

    public void add(int time,
                    FilamentChain filament) {
        filaments.put(time, filament);
    }

    public void calcProperties(boolean combineMultiMatches) {
        setBirth(filaments.keySet().stream().min(Integer::compare).orElse(-1));
        setDeath(filaments.keySet().stream().max(Integer::compare).orElse(-1));
        setLength(filaments.keySet().size());
        setKeep(true);

        //this part could (if implemented completely and correctly) stitch filaments together who should be together based on the single filament tracking
        //@todo this part should be deactivated until working correctly (and checked if wanted anyway)
        /*Map<Integer,AbstractFilament> modifiedMap=new HashMap<>();
        filaments.forEach((key,value)->{
            if(value instanceof FilamentChain){
                //the original filaments are wrapped with FilamentChain
               List<AbstractFilament> distinctFilaments= ((FilamentChain) value).getFilaments().stream().
                       distinct().collect(Collectors.toList());
                if(distinctFilaments.size()==0){
                    // no filament found, should not be?
                    System.out.println("DynFilament::calcProperties() --- no parent found");
                    //@todo inspect why this is happening
                }else if(distinctFilaments.size()==1){
                    //one single filament matching
                    modifiedMap.put(key,distinctFilaments.get(0));
                    System.out.println("DynFilament::calcProperties() --- single filament found");
                }else{
                    //more than one filament matching, combine

                    //@todo 2020-10-07 16:50 keep the combine stuff on hold currently
                    //doesn't work properly and could create problems
                    //keep FilamentChain in DynamicFilaments and fix the filamentwidth=0 problem
                    //that the chain can be handled like a AbstractFilament


                    //@todo create setting combine-multimatches{1,0} fallback strategy is take longest?
                   if(combineMultiMatches){
                       AbstractFilament first=distinctFilaments.get(0);
                       distinctFilaments.remove(first);
                       distinctFilaments.forEach(first::combine);
                       modifiedMap.put(key,first);
                       //@todo if combined the other filaments should be removed from filament-list?


                   }else {
                       distinctFilaments.sort(Comparator.comparingLong(AbstractFilament::getLength));
                       AbstractFilament first = distinctFilaments.get(distinctFilaments.size() - 1);
                       modifiedMap.put(key, first);
                   }
                    System.out.println("DynFilament::calcProperties() --- multiple filaments found");
                }

            }
        });
        setFilaments(modifiedMap);*/


    }


    public Map<Integer, AbstractFilament> getFilaments() {
        return filaments;
    }

    public void setFilaments(Map<Integer, AbstractFilament> filaments) {
        this.filaments = filaments;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public boolean isKeep() {
        return keep.get();
    }

    public BooleanProperty keepProperty() {
        return keep;
    }

    public void setKeep(boolean keep) {
        this.keep.set(keep);
    }

    public double getLength() {
        return length.get();
    }

    public DoubleProperty lengthProperty() {
        return length;
    }

    public void setLength(double length) {
        this.length.set(length);
    }

    public int getBirth() {
        return birth.get();
    }

    public IntegerProperty birthProperty() {
        return birth;
    }

    public void setBirth(int birth) {
        this.birth.set(birth);
    }

    public int getDeath() {
        return death.get();
    }

    public IntegerProperty deathProperty() {
        return death;
    }

    public void setDeath(int death) {
        this.death.set(death);
    }
}
