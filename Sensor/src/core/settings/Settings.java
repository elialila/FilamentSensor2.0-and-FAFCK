package core.settings;

import core.Const;
import core.calculation.WrappedSiZer;
import javafx.beans.property.*;
import util.Annotations;

import java.awt.*;
import java.beans.Transient;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * This class will contain all known parameters as properties for easier binding
 * there should be a mechanism to store scaling parameters (since every parameter is stored as integer, double parameters have to be
 * divided by a factor, this factor should be stored with the value)
 * For Serialization use preSerialize before ObjectSerializers, Properties are not Serializable
 * the parameter map is put into a simple integer map for the serialization
 * after loading from serialized data use postSerialize and cleanUp to reconstruct the Property Map.
 */
public class Settings implements Serializable, Cloneable {

    private transient Map<Any, IntegerProperty> parameters;

    //region Serialization-Stuff
    private Map<Any, Integer> serializeableParams;//just for testing if Property is the problem with serialization

    public void preSerialize() {
        serializeableParams = new HashMap<>();
        parameters.keySet().forEach(key -> serializeableParams.put(key, parameters.get(key).getValue()));
    }

    public void cleanUp() {
        serializeableParams.clear();
        serializeableParams = null;
    }

    public void postSerialize() {
        parameters.clear();
        serializeableParams.keySet().forEach(key -> parameters.put(key, new SimpleIntegerProperty(serializeableParams.get(key))));
        serializeableParams.clear();
        serializeableParams = null;
    }

    public void store(File file) throws FileNotFoundException {
        preSerialize();
        XMLEncoder encoder = new XMLEncoder(new FileOutputStream(file));
        encoder.writeObject(this);
        encoder.flush();
        encoder.close();
        cleanUp();
    }

    public static Settings load(@Annotations.NotNull File location) throws FileNotFoundException {
        Objects.requireNonNull(location, "location is null");
        if (!location.exists()) throw new IllegalArgumentException("location does not exist");
        try {
            XMLDecoder decoder = new XMLDecoder(new FileInputStream(location));
            Object result = decoder.readObject();
            decoder.close();
            if (result instanceof Settings) {
                ((Settings) result).postSerialize();
                return (Settings) result;
            }
            throw new IllegalArgumentException("Wrong Object Type in Serialized File:" + result.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Something is wrong with the provided File");
        }
    }
    //endregion

    public Map<Any, Integer> getSerializeableParams() {
        return serializeableParams;
    }

    public void setSerializeableParams(Map<Any, Integer> serializeableParams) {
        this.serializeableParams = serializeableParams;
    }

    public Settings() {
        parameters = new HashMap<>();
        initDefaults();
    }

    /**
     * @param dp the current object should copy all values from this parameter-object
     * This method should set all values to the values of {dp}.
     * It should keep all bindings active
     */
    public void init(Settings dp) {
        //this should theoretically work, test it to be sure!
        dp.getParameters().keySet().forEach(key -> {
            IntegerProperty toCopy = dp.getParameters().get(key);
            getParameters().get(key).set(toCopy.getValue());
        });
    }


    @Transient
    public Map<Any, IntegerProperty> getParameters() {
        return parameters;
    }

    @Transient
    public void setParameters(Map<Any, IntegerProperty> parameters) {
        this.parameters = parameters;
    }

    @Transient
    public IntegerProperty getProperty(Any key) {
        return getParameters().get(key);
    }

    @Transient
    public void setProperty(Any key, Integer value) {
        if (getProperty(key) == null) {
            getParameters().put(key, new SimpleIntegerProperty(value));
        } else {
            getProperty(key).setValue(value);
        }
    }


    @Transient
    public DoubleProperty getPropertyAsDouble(Any key) {
        IntegerProperty p1 = getProperty(key);
        DoubleProperty p2 = new SimpleDoubleProperty();

        p2.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p1.setValue(newValue.doubleValue() * key.getScale());
            }
        });
        p1.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p2.setValue(newValue.doubleValue() / key.getScale());
            }
        });
        p2.set(((double) p1.getValue()) / key.getScale());

        return p2;
    }

    public BooleanProperty getPropertyAsBoolean(Any key) {
        IntegerProperty p1 = getProperty(key);
        BooleanProperty p2 = new SimpleBooleanProperty();
        p2.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p1.set((newValue) ? 1 : 0);
            }
        });
        p1.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p2.set(newValue.intValue() != 0);
            }
        });
        p2.set(p1.get() != 0);
        return p2;
    }

    public ObjectProperty<Color> getPropertyAsColor(Any key) {
        IntegerProperty p1 = getProperty(key);
        ObjectProperty<Color> p2 = new SimpleObjectProperty<>();
        p2.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p1.set(newValue.getRGB());
            }
        });
        p1.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !oldValue.equals(newValue)) {
                p2.set(new Color(newValue.intValue()));
            }
        });
        p2.set(new Color(p1.get()));
        return p2;
    }


    @Transient
    public Integer getValue(Any key) {
        if (getProperty(key) != null)
            return getProperty(key).getValue();
        return null;
    }

    @Transient
    public Double getValueAsDouble(Any key) {
        if (getProperty(key) != null)
            return ((double) getProperty(key).get()) / ((double) key.getScale());
        return null;
    }

    @Transient
    public Boolean getValueAsBoolean(Any key) {
        if (getProperty(key) != null) {
            return getProperty(key).get() > 0;
        }
        return null;
    }

    public Color getValueAsColor(Any key) {
        if (getProperty(key) != null)
            return new Color(getProperty(key).get());
        return null;
    }


    public Settings clone() {
        Settings clone = new Settings();

        parameters.keySet().forEach(key -> {
            clone.parameters.put(key, new SimpleIntegerProperty(getValue(key)));
        });
        return clone;
    }


    private void initDefaults() {
        initPreProcessing();
        initBinarization();
        initChaining();
        initElog();
        initHough();
        initOrientation();
        initTracing();
        initWizer();
        initBatch();
        initFocAdh();
        initLoadSettings();
        initEvalSettings();
        initSFTracking();
        initExportSettings();
        initATracking();
    }

    private void initExportSettings() {
        setProperty(Export.hideNonVerifiedFibers, 0);
        setProperty(Export.hideUnusedFAs, 0);
        setProperty(Export.hideSingleUsedFAs, 0);
        setProperty(Export.hideSingleVerifiedFibers, 0);
        setProperty(Export.hideMultiUsedFAs, 0);
        setProperty(Export.hideMultiVerifiedFibers, 0);
    }

    private void initATracking() {
        setProperty(ATracking.intersectTolerance, 50);
        setProperty(ATracking.existsInMin, 4);
        setProperty(ATracking.chkExistsInMin, 1);
        setProperty(ATracking.chkExistsInMax, 0);
        setProperty(ATracking.existsInMax, 7);
    }


    private void initSFTracking() {
        setProperty(SFTracking.max_dist, 15);
        setProperty(SFTracking.factor_angle, 4);
        setProperty(SFTracking.factor_length, 1);
        setProperty(SFTracking.length, 10);
        setProperty(SFTracking.existsInMin, 4);
        setProperty(SFTracking.chkExistsInMin, 1);
        setProperty(SFTracking.chkExistsInMax, 0);
        setProperty(SFTracking.existsInMax, 7);
        setProperty(SFTracking.combineMultiMatches, 1);

    }

    private void initEvalSettings() {
        setProperty(Eval.matchMinPixels, 1);
        setProperty(Eval.thickenLines, 0);
        setProperty(Eval.createImage, 1);
    }

    private void initLoadSettings() {
        setProperty(Load.keepBitRange, 0);//default is 0, do not keep Bit Range (16bit etc. should be changed to 8bit by default)
    }


    private void initFocAdh() {
        //values are set according to test images(1024 x 1024)
        setProperty(FocAdh.minSize, 27);//3 is min to get a convex hull
        setProperty(FocAdh.maxSize, 10000);//this are just tested values
        setProperty(FocAdh.maxClusterAmount, 1300);//tested values
        setProperty(FocAdh.bothEnds, 0);//boolean value {0,1}
        setProperty(FocAdh.neighborHoodSize, 1);
        setProperty(FocAdh.doClosing, 1);//boolean value {0,1}
        setProperty(FocAdh.doFillHoles, 1);//boolean value {0,1}
        setProperty(FocAdh.showOnlyFoundFA, 0);//boolean value {0,1}
    }


    private void initPreProcessing() {
        setProperty(Pre.min_area, 50);
        setProperty(Pre.scale, 1);
        setProperty(Pre.minbright, 5);
        setProperty(Pre.min_range, 30);
        setProperty(Pre.contrast, 0);
        setProperty(Pre.black, -1);
        setProperty(Pre.white, -1);
        setProperty(Pre.rm_sng_px, 1);
        setProperty(Pre.do_laplace, 1);
        setProperty(Pre.lpfac, 20);//factor 10
        setProperty(Pre.lpmask, 1);
        setProperty(Pre.do_gauss, 1);
        setProperty(Pre.fsigma, 10);//factor 10
        setProperty(Pre.order, 0);
        setProperty(Pre.do_line_gauss, 1);
        setProperty(Pre.lg_with_mask, 1);
        setProperty(Pre.line_sigma, 50);//factor 10
        setProperty(Pre.cross_corr, 0);
        setProperty(Pre.corr_mask_size, 3);
        setProperty(Pre.corr_zero, 35);
        setProperty(Pre.widths, 3);
        setProperty(Pre.preview, 1);
    }

    private void initBinarization() {
        setProperty(Bin.method, 1);
        setProperty(Bin.radius, 3);
        setProperty(Bin.sigma, 20);//factor 10
        setProperty(Bin.minmean, 25);
        setProperty(Bin.area_significance, 100);//factor 10
        setProperty(Bin.rod_significance, 40);//factor 10
        setProperty(Bin.rm_sng_px, 1);
        setProperty(Bin.thicken, 0);
        setProperty(Bin.restrict, 0);
        setProperty(Bin.is_area_or_ext, 0);
    }

    private void initElog() {
        setProperty(Elog.input, 0);
        setProperty(Elog.image, 0);
        setProperty(Elog.tolerance, 0);
        setProperty(Elog.zero_crossings, 0);
        setProperty(Elog.threshold, -1);
        setProperty(Elog.contrast, 100);
        setProperty(Elog.orientations, 18);
        setProperty(Elog.sigma_x, 100);
        setProperty(Elog.sigma_y, 7);
    }

    private void initHough() {
        setProperty(Hough.image, 2);
        setProperty(Hough.orientations, 179);
        setProperty(Hough.sigma, 100);
        setProperty(Hough.minmean, 50);
    }

    private void initTracing() {
        setProperty(Trace.curve, 1);
        setProperty(Trace.split, 0);
        setProperty(Trace.minlen, 30);
        setProperty(Trace.minangle, 20);
        setProperty(Trace.tolerance, 5);
        setProperty(Trace.step, 10);
        setProperty(Trace.no_boundary, 0);
    }

    private void initChaining() {
        setProperty(Chain.dist_max, 10);
        setProperty(Chain.dist_max_gap, 2);
        setProperty(Chain.skew_max, 15);
        setProperty(Chain.gap_skew_max, 20);
    }

    private void initOrientation() {
        setProperty(Ori.min_area, 50);//factor 10
        setProperty(Ori.min_filaments, 5);
    }

    private void initWizer() {
        setProperty(WiZer.log_view, 1);
        setProperty(WiZer.sample_size, WrappedSiZer.LINES);
    }

    private void initBatch() {
        setProperty(Batch.determineOrientationFields, 1);
        setProperty(Batch.postProcessOrientationFields, 1);
        setProperty(Batch.saveOrientationFieldImages, 0);
        setProperty(Batch.restrictAreaChanges, 0);
        setProperty(Batch.calculateFingerprints, 0);
        setProperty(Batch.saveExcursionImages, 0);
        setProperty(Batch.color, Const.makeColorMap().get("Dark Orange").getRGB());
        setProperty(Batch.doSingleFilamentTracking, 0);
    }


}
