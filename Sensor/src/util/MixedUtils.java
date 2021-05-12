package util;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixedUtils {


    public static final int parallelMinSize = 10;
    public static final int freeMemoryMin = 2;//in GB


    public static long getApproxAvailableMemory() {

        Runtime runtime = Runtime.getRuntime();
        double freeMemory = runtime.freeMemory() / Math.pow(10, 9);
        double max = runtime.maxMemory() / Math.pow(10, 9);
        double total = runtime.totalMemory() / Math.pow(10, 9);

        double used = total - freeMemory;
        return Math.round(max - used);//round and approx image size, just some observed memory usages

    }


    public static <T> Stream<T> getStream(List<T> list, boolean forceParallel) {
        boolean useParallel = forceParallel || (list.size() > parallelMinSize && getApproxAvailableMemory() > freeMemoryMin);
        if (useParallel) return list.parallelStream();
        return list.stream();
    }

    public static final List<String> colorsForColorCoding = Arrays.asList("#EE6677", "#228833", "#4477AA", "#CCBB44", "#66CCEE", "#AA3377");

    public static List<Color> getColorsForColorCoding() {
        return colorsForColorCoding.stream().sequential().map(s -> {
            javafx.scene.paint.Color tmp = javafx.scene.paint.Color.valueOf(s);
            return new Color((float) tmp.getRed(), (float) tmp.getGreen(), (float) tmp.getBlue(), (float) tmp.getOpacity());
        }).collect(Collectors.toList());
    }

    public static List<javafx.scene.paint.Color> getColorsForColorCodingFX() {
        return colorsForColorCoding.stream().sequential().map(s -> javafx.scene.paint.Color.valueOf(s)).collect(Collectors.toList());
    }


    public static List<Color> getColors() {
        String colorcodes = "#0048BA \n" + "#B0BF1A \n" +
                "#B0BF1A \n" + "#7CB9E8 \n" + "#C9FFE5 \n" + "#72A0C1 \n" + "#EDEAE0 \n" + "#F0F8FF \n" + "#C5E17A \n" + "#C46210 \n" + "#EFDECD \n" +
                "#3B7A57 \n" + "#FFBF00 \n" + "#9966CC \n" + "#F2F3F4 \n" + "#CD9575 \n" + "#665D1E \n" + "#915C83 \n" + "#841B2D \n" + "#FAEBD7 \n" +
                "#008000 \n" + "#8DB600 \n" + "#FBCEB1 \n" + "#00FFFF \n" + "#7FFFD4 \n" + "#D0FF14 \n" + "#4B5320 \n" + "#8F9779 \n" +
                "#E9D66B \n" + "#B2BEB5 \n" + "#87A96B \n" + "#FF9966 \n" + "#A52A2A \n" + "#FDEE00 \n" + "#6E7F80 \n" + "#568203 \n" + "#007FFF \n" +
                "#F0FFFF \n" + "#89CFF0 \n" + "#A1CAF1 \n" + "#F4C2C2 \n" + "#FEFEFA \n" + "#FF91AF \n" + "#FAE7B5 \n" + "#E94196 \n" + "#E0218A \n" +
                "#7C0A02 \n" + "#848482 \n" + "#BCD4E6 \n" + "#9F8170 \n" + "#F5F5DC \n" + "#2E5894 \n" + "#9C2542 \n" + "#D99A6C \n" + "#FFE4C4 \n" + "#3D2B1F \n" + "#967117 \n" + "#CAE00D \n" + "#BFFF00 \n" + "#FE6F5E \n" + "#BF4F51 \n" + "#000000 \n" + "#3D0C02 \n" +
                "#1B1811 \n" + "#3B2F2F \n" + "#54626F \n" + "#3B3C36 \n" + "#BFAFB2 \n" + "#FFEBCD \n" + "#A57164 \n" + "#318CE7 \n" + "#ACE5EE \n" + "#FAF0BE \n" + "#660000 \n" + "#0000FF \n" + "#1F75FE \n" + "#0093AF \n" + "#0087BD \n" + "#0018A8 \n" +
                "#333399 \n" + "#0247FE \n" + "#A2A2D0 \n" + "#6699CC \n" + "#0D98BA \n" + "#064E40 \n" + "#5DADEC \n" + "#126180 \n" + "#8A2BE2 \n" + "#7366BD \n" + "#4D1A7F \n" + "#5072A7 \n" + "#3C69E7 \n" + "#DE5D83 \n" + "#79443B \n" +
                "#0095B6 \n" + "#E3DAC9 \n" + "#006A4E \n" + "#87413F \n" + "#CB4154 \n" + "#66FF00 \n" + "#D891EF \n" + "#C32148 \n" + "#1974D2 \n" + "#FFAA1D \n" + "#FF55A3 \n" + "#FB607F \n" + "#004225 \n" + "#CD7F32 \n" +
                "#88540B \n" + "#AF6E4D \n" + "#1B4D3E \n" + "#E7FEFF \n" + "#7BB661 \n" + "#F0DC82 \n" + "#800020 \n" + "#DEB887 \n" + "#A17A74 \n" + "#CC5500 \n" + "#E97451 \n" + "#8A3324 \n" + "#BD33A4 \n" + "#702963 \n" + "#536872 \n" + "#5F9EA0 \n" +
                "#A9B2C3 \n" + "#91A3B0 \n" + "#006B3C \n" + "#ED872D \n" + "#E30022 \n" + "#FFF600 \n" + "#A67B5B \n" + "#4B3621 \n" + "#A3C1AD \n" + "#C19A6B \n" + "#EFBBCC \n" + "#FFFF99 \n" + "#FFEF00 \n" + "#FF0800 \n" + "#E4717A \n" +
                "#00BFFF \n" + "#592720 \n" + "#C41E3A \n" + "#00CC99 \n" + "#960018 \n" + "#D70040 \n" + "#FFA6C9 \n" + "#B31B1B \n" + "#56A0D3 \n" + "#ED9121 \n" + "#00563F \n" + "#703642 \n" + "#C95A49 \n" + "#ACE1AF \n" + "#007BA7 \n" + "#2F847C \n" + "#B2FFFF \n" + "#246BCE \n" + "#DE3163 \n" + "#007BA7 \n" +
                "#2A52BE \n" + "#6D9BC3 \n" + "#1DACD6 \n" + "#007AA5 \n" + "#E03C31 \n" + "#F7E7CE \n" + "#F1DDCF \n" + "#36454F \n" + "#232B2B \n" + "#E68FAC \n" + "#DFFF00 \n" + "#7FFF00 \n" + "#FFB7C5 \n" + "#954535 \n" + "#DE6FA1 \n" + "#A8516E \n" + "#AA381E \n" + "#856088 \n" + "#FFB200 \n" + "#7B3F00 \n" +
                "#D2691E \n" + "#FFA700 \n" + "#98817B \n" + "#E34234 \n" + "#CD607E \n" + "#E4D00A \n" + "#9FA91F \n" + "#7F1734 \n" + "#0047AB \n" + "#D2691E \n" + "#6F4E37 \n" + "#B9D9EB \n" + "#F88379 \n" + "#8C92AC \n" + "#B87333 \n" + "#DA8A67 \n" + "#AD6F69 \n" + "#CB6D51 \n" + "#996666 \n" + "#FF3800 \n" + "#FF7F50 \n" + "#F88379 \n" + "#893F45 \n" + "#FBEC5D \n" + "#6495ED \n" + "#FFF8DC \n" + "#2E2D88 \n" + "#FFF8E7 \n" + "#FEBCFF \n" + "#81613C \n" + "#FFBCD9 \n" + "#FFFDD0 \n" + "#DC143C \n" + "#9E1B32 \n" + "#F5F5F5 \n" + "#00FFFF \n" + "#00B7EB \n" + "#58427C \n" + "#FFD300 \n" + "#F56FA1 \n" + "#666699 \n" + "#654321 \n" + "#5D3954 \n" + "#26428B \n" + "#008B8B \n" + "#536878 \n" + "#B8860B \n" + "#013220 \n" + "#006400 \n" +
                "#1A2421 \n" + "#BDB76B \n" + "#483C32 \n" + "#534B4F \n" + "#543D37 \n" + "#8B008B \n" + "#4A5D23 \n" + "#556B2F \n" + "#FF8C00 \n" + "#9932CC \n" + "#03C03C \n" + "#301934 \n" + "#8B0000 \n" + "#E9967A \n" + "#8FBC8F \n" + "#3C1414 \n" + "#8CBED6 \n" + "#483D8B \n" + "#2F4F4F \n" + "#177245 \n" + "#00CED1 \n" + "#9400D3 \n" + "#00703C \n" + "#555555 \n" + "#DA3287 \n" + "#FAD6A5 \n" + "#B94E48 \n" + "#004B49 \n" + "#FF1493 \n" + "#FF9933 \n" + "#00BFFF \n" + "#4A646C \n" + "#7E5E60 \n" + "#1560BD \n" + "#2243B6 \n" +
                "#C19A6B \n" + "#EDC9AF \n" + "#696969 \n" + "#C53151 \n" + "#1E90FF \n" + "#D71868 \n" + "#967117 \n" + "#00009C \n" + "#EFDFBB \n" + "#E1A95F \n" + "#555D50 \n" + "#C2B280 \n" + "#1B1B1B \n" + "#614051 \n" + "#F0EAD6 \n" + "#1034A6 \n" + "#7DF9FF \n" + "#00FF00 \n" + "#6F00FF \n" +
                "#CCFF00 \n" + "#BF00FF \n" + "#8F00FF \n" + "#50C878 \n" + "#6C3082 \n" + "#1B4D3E \n" + "#B48395 \n" + "#AB4B52 \n" + "#CC474B \n" + "#563C5C \n" + "#00FF40 \n" + "#96C8A2 \n" + "#C19A6B \n" + "#801818 \n" + "#B53389 \n" + "#DE5285 \n" + "#F400A1 \n" + "#E5AA70 \n" +
                "#4D5D53 \n" + "#4F7942 \n" + "#6C541E \n" + "#FF5470 \n" + "#B22222 \n" + "#CE2029 \n" + "#E95C4B \n" + "#E25822 \n" + "#EEDC82 \n" + "#FFE9D1 \n" + "#0063dc \n" + "#FB0081 \n" + "#A2006D \n" + "#FFFAF0 \n" + "#15F4EE \n" + "#5FA777 \n" + "#014421 \n" + "#228B22 \n" + "#A67B5B \n" + "#856D4D \n" + "#0072BB \n" + "#FD3F92 \n" + "#86608E \n" + "#9EFD38 \n" + "#D473D4 \n" + "#FD6C9E \n" + "#C72C48 \n" + "#F64A8A \n" + "#77B5FE \n" + "#8806CE \n" + "#E936A7 \n" + "#FF00FF \n" + "#C154C1 \n" + "#CC397B \n" + "#C74375 \n" + "#E48400 \n" + "#CC6666 \n";
        String colors[] = colorcodes.replace(" \n", ",").split(",");

        List<Color> colorList = Arrays.stream(colors).sequential().map(s -> {
            javafx.scene.paint.Color tmp = javafx.scene.paint.Color.valueOf(s);
            Color tmpCol = new Color((float) tmp.getRed(), (float) tmp.getGreen(), (float) tmp.getBlue(), (float) tmp.getOpacity());
            if (tmpCol.getRed() > 200 && tmpCol.getGreen() > 200 && tmpCol.getBlue() > 200) return null;
            return tmpCol;

        }).filter(c -> c != null).collect(Collectors.toList());
        return colorList;
    }

    /**
     * Round to a specified number of decimals
     *
     * @param number
     * @param decimals
     * @return "rounded" number
     */
    public static double round(double number, int decimals) {
        long exp = (long) Math.pow(10, decimals);
        long tmp = (long) (number * exp);
        //Double d = n.doubleValue() + 1e-6;// round up to 6decimals

        return (((double) tmp) / exp);//does not work 100%
    }

}
