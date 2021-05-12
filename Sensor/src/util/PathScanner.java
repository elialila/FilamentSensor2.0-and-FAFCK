package util;

import util.Annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PathScanner {

    public static final String OME_TIF = ".ome.tif";

    //actually there is no need for adding ".ome.tif" since .ome.tif files are a subset of .tif, but for better
    //readability it is added
    public static final List<String> supportedImageExtensions = new ArrayList<>(Arrays.asList(".jpg", ".jpeg", ".png", OME_TIF, ".tif", ".tiff", ".gif"));

    public static final List<String> filteredNameSuffixes = new ArrayList<>(Arrays.asList("_filament", "_main", "_other", "_adhesion", "_orientation", "_noise", "_overview"));

    /**
     * Scans File root(directory) for files recursively, returning all leaf directories from the file tree in a list
     *
     * @param root                   directory where the scan starts
     * @param listOfImageDirectories the current result list
     * @param supportedExtensions    list of all supported file extensions
     */
    private static void getImageDirectories(File root, List<File> listOfImageDirectories, final List<String> supportedExtensions) {
        if (!root.isDirectory()) return;
        List<File> files = Arrays.asList(Objects.requireNonNull(root.listFiles()));

        //check if there are no "special" files
        if (files.stream().noneMatch(file -> filteredNameSuffixes.stream().anyMatch(file.getName().toLowerCase()::contains))) {

            //if (files.stream().noneMatch(file -> file.getName().contains("filament"))) {//check if there are no filament files
            //check if there are image files present
            //files.stream().anyMatch(file->file.getName().toLowerCase().contains(".png")||file.getName().toLowerCase().contains(".jpg"))
            //check for list of strings (list of all supported image extensions
            //if yes -> add root to listOfImageDirectories
            if (files.stream().anyMatch(file -> supportedExtensions.stream().anyMatch(file.getName().toLowerCase()::contains))) {
                //image file identified
                listOfImageDirectories.add(root);
            } else {
                files.stream().filter(File::isDirectory).forEach(file -> getImageDirectories(file, listOfImageDirectories, supportedExtensions));
            }
        }
    }


    public static List<File> scanPath(File root, final List<String> extensions) {
        List<File> result = new ArrayList<>();
        getImageDirectories(root, result, extensions);
        return result;
    }


    /**
     * Scan root for Files with extension contained in supportedExtensions
     *
     * @param root
     * @param supportedExtensions
     * @param filteredNameSuffixes
     * @param recursive
     * @return Filtered File List
     */
    public static List<File> getFilesInDirectory(@NotNull File root, @NotNull List<String> supportedExtensions, @NotNull List<String> filteredNameSuffixes, boolean recursive) {
        Objects.requireNonNull(root, "File root is null");
        Objects.requireNonNull(supportedExtensions, "SupportedExtensions is null");
        if (!root.exists() || !root.isDirectory())
            throw new IllegalArgumentException("File root is either no directory or doesn't even exist");
        List<File> files = Arrays.asList(Objects.requireNonNull(root.listFiles()));
        List<File> result = files.stream().filter(f -> filteredNameSuffixes.stream().noneMatch(f.getName().toLowerCase()::contains) &&
                supportedExtensions.stream().anyMatch(f.getName().toLowerCase()::contains)
        ).collect(Collectors.toList());
        if (!recursive) return result;
        files.stream().filter(File::isDirectory).forEachOrdered(f -> {
            result.addAll(getFilesInDirectory(f, supportedExtensions, filteredNameSuffixes, true));
        });
        return result;
    }


    /**
     * Return all supported image-files - which don't contain "filament" in their names - in the root directory given as a parameter
     *
     * @param root
     * @return
     */
    public static List<File> getImageFilesInDirectory(@NotNull File root) {
        return getFilesInDirectory(root, supportedImageExtensions, filteredNameSuffixes, false);
    }


    /**
     * Gets the common parent from multiple files
     *
     * @param files
     * @return
     */
    public static File getCommonParent(File... files) {
        int commonLength = 0;
        char currentSymbol;
        boolean finished = false;
        Objects.requireNonNull(files, "files is null");
        if (files.length < 1) throw new IllegalArgumentException("parameter is empty");
        if (files.length == 1) return files[0].getParentFile();//return parent in case of only one parameter
        while (!finished) {
            currentSymbol = files[0].getAbsolutePath().charAt(commonLength);
            for (int i = 0; i < files.length; i++) {
                //check if the current character is still common
                if ((files[i].getAbsolutePath().length() - 1 > commonLength && files[i].getAbsolutePath().charAt(commonLength) != currentSymbol)) {
                    i = files.length;
                    finished = true;
                }
            }
            //abort if files[0] would be out of index
            if (files[0].getAbsolutePath().length() - 1 < commonLength + 1) finished = true;
            if (!finished) commonLength++;
        }
        return new File(files[0].getAbsolutePath().substring(0, commonLength));
    }


}
