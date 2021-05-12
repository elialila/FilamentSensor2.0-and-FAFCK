package core.settings;
/*
 * This file is part of FilamentSensor - A tool for filament tracking from cell images
 *
 * Copyright (C) 2011-2013 Julian Rüger
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

import util.Annotations.NotNull;

import java.beans.Transient;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;

import java.util.*;

/**
 * FilamentSensor configuration class.
 * This class contains the Application-Settings
 *
 *
 * Pseudo Singleton(no private constructor, standard constructor is needed for xml serialization)
 */
public class Config implements Serializable {
    // Default values
    private Map<String, String> settings;

    private String configurationFile;


    public Config() {
        settings = new HashMap<>();
    }

    public void init() {
        mapDefaults(settings);
    }


    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Transient
    public File getConfigDirectory() {
        if (configurationFile == null) return null;
        File config = new File(configurationFile);
        if (!config.getParentFile().exists()) return null;
        return config.getParentFile();
    }

    @Transient
    public File getFiltersDirectory() {
        if (getConfigDirectory() == null) return null;
        File file = new File(getConfigDirectory().getAbsolutePath() + File.separator + "filters");
        if (!file.exists() && !file.mkdir()) return null;
        return file;
    }


    private void mapDefaults(Map<String, String> map) {
        map.put("window.width", "800");
        map.put("window.height", "640");
        map.put("window.title", "Filament Sensor");

    }


    public void store() throws FileNotFoundException, IllegalArgumentException {
        if (configurationFile == null) throw new IllegalArgumentException("Configuration-File not set.");

        File config = new File(configurationFile);
        config.getParentFile().mkdirs();

        XMLEncoder encoder = new XMLEncoder(new FileOutputStream(config));
        encoder.writeObject(this);
        encoder.flush();
        encoder.close();
    }

    public static Config load(@NotNull File config) throws FileNotFoundException, IllegalArgumentException {
        Objects.requireNonNull(config, "config is null");
        if (!config.exists()) throw new IllegalArgumentException("config File does not exist");

        XMLDecoder decoder = new XMLDecoder(new FileInputStream(config));
        instance = (Config) decoder.readObject();
        return instance;
    }


    private transient static Config instance = null;

    @Transient
    public static Config getInstance() {
        if (instance == null) instance = new Config();
        return instance;
    }



}
