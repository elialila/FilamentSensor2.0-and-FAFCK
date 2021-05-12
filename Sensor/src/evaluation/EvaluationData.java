package evaluation;

import ij.ImagePlus;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

public class EvaluationData {

    //should contain some description of the file? for example file name and path?

    //#region basic information on both images
    private int widthEval;
    private int heightEval;
    private int widthTruth;
    private int heightTruth;

    private int whiteEval;
    private int whiteTruth;
    //#endregion


    //#region pixel-comparison
    private int whiteMatches;
    private double hitRate;
    private double missRate;

    private int fpMatches;
    private double fpRate;

    private int fnMatches;
    private double fnRate;
    //#endregion


    //#region object-comparison
    private int objectsEval;
    private int objectsTruth;

    private int objectsFound;
    private int objectsMissed;
    private int objectsFP;//FP=false-positive
    private int multiMatchesOneToN;//one object of truth matched more than one of eval
    private int multiMatchesNToOne;//one object of eval matched more than one of truth


    //truthPixels,evalPixels, andPixels
    private List<Pair<Integer, Pair<Integer, Integer>>> matchAreas;

    private List<Pair<Integer, Pair<Integer, Integer>>> nonMatchEval;

    private List<Pair<Integer, Pair<Integer, Integer>>> nonMatchTruth;

    //include area data stuff?

    private ImagePlus diffImagePixels;
    private ImagePlus diffImageObjects;

    //#endregion


    public EvaluationData() {
        matchAreas = new ArrayList<>();
    }


    public int getWidthEval() {
        return widthEval;
    }

    public void setWidthEval(int widthEval) {
        this.widthEval = widthEval;
    }

    public int getHeightEval() {
        return heightEval;
    }

    public void setHeightEval(int heightEval) {
        this.heightEval = heightEval;
    }

    public int getWidthTruth() {
        return widthTruth;
    }

    public void setWidthTruth(int widthTruth) {
        this.widthTruth = widthTruth;
    }

    public int getHeightTruth() {
        return heightTruth;
    }

    public void setHeightTruth(int heightTruth) {
        this.heightTruth = heightTruth;
    }

    public int getWhiteEval() {
        return whiteEval;
    }

    public void setWhiteEval(int whiteEval) {
        this.whiteEval = whiteEval;
    }

    public int getWhiteTruth() {
        return whiteTruth;
    }

    public void setWhiteTruth(int whiteTruth) {
        this.whiteTruth = whiteTruth;
    }

    public int getWhiteMatches() {
        return whiteMatches;
    }

    public void setWhiteMatches(int whiteMatches) {
        this.whiteMatches = whiteMatches;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    public int getFpMatches() {
        return fpMatches;
    }

    public void setFpMatches(int fpMatches) {
        this.fpMatches = fpMatches;
    }

    public double getFpRate() {
        return fpRate;
    }

    public void setFpRate(double fpRate) {
        this.fpRate = fpRate;
    }

    public int getFnMatches() {
        return fnMatches;
    }

    public void setFnMatches(int fnMatches) {
        this.fnMatches = fnMatches;
    }

    public double getFnRate() {
        return fnRate;
    }

    public void setFnRate(double fnRate) {
        this.fnRate = fnRate;
    }

    public double getMissRate() {
        return missRate;
    }

    public void setMissRate(double missRate) {
        this.missRate = missRate;
    }

    public int getObjectsEval() {
        return objectsEval;
    }

    public void setObjectsEval(int objectsEval) {
        this.objectsEval = objectsEval;
    }

    public int getObjectsTruth() {
        return objectsTruth;
    }

    public void setObjectsTruth(int objectsTruth) {
        this.objectsTruth = objectsTruth;
    }

    public int getObjectsFound() {
        return objectsFound;
    }

    public void setObjectsFound(int objectsFound) {
        this.objectsFound = objectsFound;
    }

    public int getObjectsMissed() {
        return objectsMissed;
    }

    public void setObjectsMissed(int objectsMissed) {
        this.objectsMissed = objectsMissed;
    }

    public int getObjectsFP() {
        return objectsFP;
    }

    public void setObjectsFP(int objectsFP) {
        this.objectsFP = objectsFP;
    }

    public List<Pair<Integer, Pair<Integer, Integer>>> getNonMatchEval() {
        return nonMatchEval;
    }

    public void setNonMatchEval(List<Pair<Integer, Pair<Integer, Integer>>> nonMatchEval) {
        this.nonMatchEval = nonMatchEval;
    }

    public List<Pair<Integer, Pair<Integer, Integer>>> getNonMatchTruth() {
        return nonMatchTruth;
    }

    public void setNonMatchTruth(List<Pair<Integer, Pair<Integer, Integer>>> nonMatchTruth) {
        this.nonMatchTruth = nonMatchTruth;
    }

    public List<Pair<Integer, Pair<Integer, Integer>>> getMatchAreas() {
        return matchAreas;
    }

    public void setMatchAreas(List<Pair<Integer, Pair<Integer, Integer>>> matchAreas) {
        this.matchAreas = matchAreas;
    }

    public int getMultiMatchesOneToN() {
        return multiMatchesOneToN;
    }

    public void setMultiMatchesOneToN(int multiMatchesOneToN) {
        this.multiMatchesOneToN = multiMatchesOneToN;
    }

    public int getMultiMatchesNToOne() {
        return multiMatchesNToOne;
    }

    public void setMultiMatchesNToOne(int multiMatchesNToOne) {
        this.multiMatchesNToOne = multiMatchesNToOne;
    }

    public ImagePlus getDiffImagePixels() {
        return diffImagePixels;
    }

    public void setDiffImagePixels(ImagePlus diffImagePixels) {
        this.diffImagePixels = diffImagePixels;
    }

    public ImagePlus getDiffImageObjects() {
        return diffImageObjects;
    }

    public void setDiffImageObjects(ImagePlus diffImageObjects) {
        this.diffImageObjects = diffImageObjects;
    }

    @Override
    public String toString() {
        return "EvaluationData{" +
                "widthEval=" + widthEval +
                ", heightEval=" + heightEval +
                ", widthTruth=" + widthTruth +
                ", heightTruth=" + heightTruth +
                ", whiteEval=" + whiteEval +
                ", whiteTruth=" + whiteTruth +
                ", whiteMatches=" + whiteMatches +
                ", hitRate=" + hitRate +
                ", missRate=" + missRate +
                ", fpMatches=" + fpMatches +
                ", fpRate=" + fpRate +
                ", fnMatches=" + fnMatches +
                ", fnRate=" + fnRate +
                ", objectsEval=" + objectsEval +
                ", objectsTruth=" + objectsTruth +
                ", objectsFound=" + objectsFound +
                ", objectsMissed=" + objectsMissed +
                ", objectsFP=" + objectsFP +
                ", multiMatchesOneToN=" + multiMatchesOneToN +
                ", multiMatchesNToOne=" + multiMatchesNToOne +
                ", matchAreas=" + matchAreas +
                '}';
    }

    public String getCsv() {
        StringBuilder builder = new StringBuilder("");
        builder.append("#" +
                "widthEval" + "," +
                "heightEval" + "," +
                "widthTruth" + "," +
                "heightTruth" + "," +
                "whiteEval" + "," +
                "whiteTruth" + "," +
                "whiteMatches" + "," +
                "hitRate" + "," +
                "missRate" + "," +
                "fpMatches" + "," +
                "fpRate" + "," +
                "fnMatches" + "," +
                "fnRate" + "," +
                "objectsEval" + "," +
                "objectsTruth" + "," +
                "objectsFound" + "," +
                "objectsMissed" + "," +
                "objectsFP" + "," +
                "multiMatchesOneToN" + "," +
                "multiMatchesNToOne\n");
        builder.append(widthEval).append(",").append(heightEval).append(",").append(widthTruth).append(",").append(heightTruth).append(",").append(whiteEval).append(",").append(whiteTruth).append(",").append(whiteMatches).append(",").append(hitRate).append(",").append(missRate).append(",").append(fpMatches).append(",").append(fpRate).append(",").append(fnMatches).append(",").append(fnRate).append(",").append(objectsEval).append(",").append(objectsTruth).append(",").append(objectsFound).append(",").append(objectsMissed).append(",").append(objectsFP).append(",").append(multiMatchesOneToN).append(",").append(multiMatchesNToOne).append("\n");
        builder.append("#pixel's ground truth,pixels to eval, pixels match\n");
        matchAreas.forEach(p -> builder.append(p.getKey()).append(",").
                append(p.getValue().getKey()).append(",").append(p.getValue().getValue()).append("\n")
        );
        builder.append("#pixels' eval not matched\n");
        nonMatchEval.forEach(p -> builder.append(p.getKey()).append(",").
                append(p.getValue().getKey()).append(",").append(p.getValue().getValue()).append("\n")
        );
        builder.append("#pixels' truth not matched\n");
        nonMatchTruth.forEach(p -> builder.append(p.getKey()).append(",").
                append(p.getValue().getKey()).append(",").append(p.getValue().getValue()).append("\n")
        );

        return builder.toString();
    }

}
