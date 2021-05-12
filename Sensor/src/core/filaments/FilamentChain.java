package core.filaments;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2013-2014 Benjamin Eltzner
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
import javafx.beans.property.SimpleBooleanProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import core.FilamentSensor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class FilamentChain extends AbstractFilament {

    private List<AbstractFilament> m_filament_list;


    // m_angle, m_length and m_width are longs that are scaled by 1,000,000
    // to avoid the problems of floating point arithmetics.


    public FilamentChain(List<AbstractFilament> filament_list) {
        m_filament_list = (filament_list == null ? new ArrayList<>() : filament_list);
        keep = new SimpleBooleanProperty(false);

        //simple operation no use for parallel stream
        if (m_filament_list.stream().anyMatch(AbstractFilament::isKeep)) {
            setKeep(true);
        }

        chainAverages();
    }

    @Override
    public void setVerified(boolean verified) {
        super.setVerified(verified);
        m_filament_list.forEach(f -> f.setVerified(verified));
    }


    public FilamentChain(AbstractFilament filament) {
        keep = new SimpleBooleanProperty(false);
        if (filament == null) {
            m_filament_list = new ArrayList<>();
            chainAverages();
            return;
        }
        m_filament_list = new ArrayList<>();
        // This is ok, because m_filament_list has only one filament afterwards.
        if (!add(filament)) {
            chainAverages();
        } else {
            setKeep(filament.isKeep());
        }
    }

    public FilamentChain() {
        m_filament_list = new ArrayList<>();
        chainAverages();
        keep = new SimpleBooleanProperty(true);
    }

    /**
     * @param chain
     * @deprecated not used anywhere
     */
    public FilamentChain(Node chain) {

        m_filament_list = new ArrayList<>();
        keep = new SimpleBooleanProperty(false);
        for (Node filament = chain.getFirstChild(); filament != null; filament = filament.getNextSibling()) {
            try {
                if (filament.getNodeType() != Node.ELEMENT_NODE || !filament.hasAttributes()) {
                    continue;
                }

                CurvedFilament fil = new CurvedFilament((Element) filament);
                setKeep(isKeep() || fil.isKeep());
                m_filament_list.add(fil);
            } catch (Exception e) {
                FilamentSensor.debugError("Error parsing Filament '" + filament + "': " + e.getMessage());
            }
        }

        chainAverages();
    }

    @Override
    public double getMass() {
        return 0;
    }

    @Override
    public List<Point> getPoints() {
        List<Point> points = new ArrayList<>();
        m_filament_list.forEach(fil -> points.addAll(fil.getPoints()));
        return points;
    }

    @Override
    public List<Filament> splitIntoLinearPieces() {
        List<Filament> linearPieces = new ArrayList<>();
        m_filament_list.forEach(fil -> linearPieces.addAll(fil.splitIntoLinearPieces()));
        return linearPieces;
    }

    @Override
    public List<Filament> splitIntoLinearPieces(int max_size) {
        List<Filament> linearPieces = new ArrayList<>();
        m_filament_list.forEach(fil -> linearPieces.addAll(fil.splitIntoLinearPieces(max_size)));
        return linearPieces;
    }

    @Override
    public void combine(AbstractFilament filament) {
        add(filament);
    }


    // The following methods allow direct manipulation of the filament chain.
    // They exist mostly for testing. If they are ever called by running code
    // a sanity check for the filament chain should be performed.

    public boolean add(AbstractFilament filament) {
        if (filament != null && m_filament_list.add(filament)) {
            chainAverages();
            return true;
        }
        return false;
    }

    public boolean removeFilament(AbstractFilament filament) {
        if (m_filament_list.remove(filament)) {
            chainAverages();
            return true;
        }
        return false;
    }

    // Getter methods.


    public boolean getKeepAny() {
        for (AbstractFilament filament : m_filament_list) {
            if (filament.isKeep()) {
                return true;
            }
        }
        return isKeep();
    }


    public List<AbstractFilament> getFilaments() {
        //in this method a clone was returned, switched clone with this
        //FilamentChain chain = (FilamentChain) clone();
        return this.m_filament_list;
    }

    public AbstractFilament getFilament(int position) {
        if (m_filament_list == null || position >= m_filament_list.size()) {
            return null;
        }
        return (AbstractFilament) (m_filament_list.get(position).clone());
    }

    public AbstractFilament changeFilament(int position) {
        if (m_filament_list == null || position >= m_filament_list.size()) {
            return null;
        }
        return m_filament_list.get(position);
    }

    public int getListSize() {
        if (m_filament_list == null) {
            return -1;
        }

        return m_filament_list.size();
    }

    public boolean contains(AbstractFilament filament) {
        if (m_filament_list == null) {
            return false;
        }

        return m_filament_list.contains(filament);
    }

    @Override
    public void setKeep(boolean keep) {
        keepProperty().set(keep);

        if (getListSize() == 1) {
            m_filament_list.get(0).setKeep(keep);
        }
    }

    public void setKeepAll(boolean keep) {
        keepProperty().set(keep);

        for (AbstractFilament filament : m_filament_list) {
            filament.setKeep(keep);
        }
    }

    @Override
    public void setWidth(long width) {
        if (getListSize() == 1) {
            m_filament_list.get(0).setWidth(width);
            width();
        } else {
           FilamentSensor.debugMessage("FilamentSensor FilamentChain.setWidth: Width can only be changed for single filaments!");
        }
    }

    @Override
    public void invert() {
        if (m_filament_list == null || m_filament_list.isEmpty()) {
            return;
        }

        List<AbstractFilament> new_list = new ArrayList<>();

        for (int i = m_filament_list.size() - 1; i >= 0; i--) {
            AbstractFilament filament = m_filament_list.get(i);
            filament.invert();
            new_list.add(filament);
        }

        m_filament_list = new_list;
    }



    @Override
    public Element toXML(Document document, int chain_number, int dump) {
        Element chain_element = document.createElement("chain" + chain_number);

        chain_element.setAttribute("center_x", "" + getCenter().x);
        chain_element.setAttribute("center_y", "" + getCenter().y);
        chain_element.setAttribute("length", "" + getLength());
        chain_element.setAttribute("angle", "" + getOrientation());
        chain_element.setAttribute("width", "" + getWidth());

        for (int j = 0; j < getListSize(); j++) {
            chain_element.appendChild(getFilament(j).toXML(document, chain_number, j));
        }
        return chain_element;
    }

    // Methods for calculation of chain properties.

    private void chainAverages() {
        if (m_filament_list == null || m_filament_list.isEmpty()) {
            setCenter(new Point(0, 0));
            setOrientation(0);
            setLength(0);
            setWidth(0);
            return;
        }

        // Speed up treatment of trivial filament chains.
        // Necessary due to frequent conversions.
        if (m_filament_list.size() == 1) {
            setCenter(m_filament_list.get(0).getCenter());
            setOrientation(m_filament_list.get(0).getOrientation());
            setLength(m_filament_list.get(0).getLength());
            setWidth(m_filament_list.get(0).getWidth());
            return;
        }

        setCenter(center());
        setOrientation(angle());
        setLength(length());
        setWidth(width());
    }

    // The center is calculated as a length*width-weighted average.
    private Point center() {
        double x_numerator = 0;
        double y_numerator = 0;
        double denominator = m_filament_list.size();

        for (int i = 0; i < m_filament_list.size(); i++) {
            denominator += m_filament_list.get(i).getLength() * m_filament_list.get(i).getWidth();
            x_numerator += m_filament_list.get(i).getLength() * m_filament_list.get(i).getWidth()
                    * m_filament_list.get(i).getCenter().x;
            y_numerator += m_filament_list.get(i).getLength() * m_filament_list.get(i).getWidth()
                    * m_filament_list.get(i).getCenter().y;
        }
        return new Point((int) Math.floor(x_numerator / denominator), (int) Math.floor(y_numerator / denominator));
        // Use floor instead of round, because center is scaled 10-fold:
        // Math.round( ((int)Math.round(1234,5))/10f ) = Math.round( 1235/10f )
        // = 124

    }

    // The angle is calculated as a length*width-weighted average.
    // The implementation is probably very inefficient.
    private long angle() {
        long unchanged_angle = 0;
        long changed_angle = 0;
        long[] angles = new long[m_filament_list.size()];
        for (int i = 0; i < m_filament_list.size(); i++) {
            angles[i] = m_filament_list.get(i).getOrientation();
            for (int j = 0; j < i; j++) {
                // Sum of angle differences
                unchanged_angle += Math.abs(angles[i] - angles[j]);
                // Sum of differences between angles rotated by 90°
                changed_angle += Math.abs(((angles[i] + 90 * Const.M) % (180 * Const.M)) -
                        ((angles[j] + 90 * Const.M) % (180 * Const.M)));
            }
        }

        // The condition will be true, if orientations accumulate around 0° = 180°.
        if (changed_angle < unchanged_angle) {
            double numerator = 0;
            double denominator = m_filament_list.size();
            for (int i = 0; i < m_filament_list.size(); i++) {
                denominator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth();
                numerator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth()
                        * (((m_filament_list.get(i).getOrientation() + 90 * Const.M) % (180 * Const.M)) - 90 * Const.M);
            }
            // +180*M because in java, modulo can be negative.
            return Math.round((numerator / denominator + 180 * Const.M) % (180 * Const.M));
        } else {
            double numerator = 0;
            double denominator = m_filament_list.size();
            for (int i = 0; i < m_filament_list.size(); i++) {
                denominator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth();
                numerator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth()
                        * m_filament_list.get(i).getOrientation();
            }
            return Math.round(numerator / denominator);
        }
    }

    /**
     * The length calculation for a filament chain requires the filaments
     * to be ordered such that neighboring elements of the list identify
     * neighboring filaments.
     */
    private long length() {
        sortFilamentList();
        long length = 0;
        for (int i = 0; i < m_filament_list.size() - 1; i++) {
            length += Calc.distanceM(m_filament_list.get(i).getCenter(), m_filament_list.get(i + 1).getCenter()) / 10;
        }
        length += Math.round(0.5 * m_filament_list.get(0).getLength() + 0.5
                * m_filament_list.get(m_filament_list.size() - 1).getLength());

        return length;
    }

    // The width of the chain is calculated as the length-weighted average of
    // filament widths.
    private long width() {
        double numerator = 0;
        double denominator = m_filament_list.size();
        for (int i = 0; i < m_filament_list.size(); i++) {
            denominator += m_filament_list.get(i).getLength();
            numerator += (double) m_filament_list.get(i).getLength() * (double) m_filament_list.get(i).getWidth();
        }
        return Math.round(numerator / denominator);
    }

    /**
     * This method sorts the list such that neighboring elements of the list
     * identify neighboring filaments. Returns true, if the implied sanity
     * check succeeds.
     */
    private boolean sortFilamentList() {
        if (m_filament_list.size() == 0) {
            return false;
        }
        return true;
        // CalculateFilamentChains.
    }

    @Override
    public Object clone() {
        List<AbstractFilament> list = new ArrayList<>();
        for (int i = 0; i < m_filament_list.size(); i++) {
            list.add((AbstractFilament) m_filament_list.get(i).clone());
        }
        FilamentChain dup = new FilamentChain(list);
        return dup;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object.getClass().equals(this.getClass()))) {
            return false;
        }

        FilamentChain fil = (FilamentChain) object;
        return (getCenter().x == fil.getCenter().x && getCenter().y == fil.getCenter().y && getLength() == fil.getLength()
                && getWidth() == fil.getWidth() && getOrientation() % (180 * Const.M) == fil.getOrientation() % (180 * Const.M)
                && m_filament_list.containsAll(fil.m_filament_list) && fil.m_filament_list.containsAll(m_filament_list));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (getOrientation() ^ (getOrientation() >>> 32));
        result = prime * result + ((getCenter() == null) ? 0 : getCenter().hashCode());
        result = prime * result + ((m_filament_list == null) ? 0 : m_filament_list.hashCode());
        result = prime * result + (int) (getLength() ^ (getLength() >>> 32));
        result = prime * result + (int) (getWidth() ^ (getWidth() >>> 32));
        return result;
    }

}
