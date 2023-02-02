# Tutorial for Console-Application usage #

## Requirements ##
Installed Java SDK (8 or ~12+)

## Call ##
On Java 8 the call is:

java -jar __JARNAME__.jar _HERE THE PARAMETERS - __SEE PARAMETER SECTION___

On Java Version >8:
JavaFX has to be installed separately. (Setup OpenJDK & JavaFX).

JAVA_FX_HOME Environment-Variable has to be set.

The call looks like that (on Windows):

java --module-path %JAVA_FX_HOME%\lib --add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.swt -jar __JARNAME__.jar _HERE THE PARAMETERS - __SEE PARAMETER SECTION___

## Parameters ##
- _-cmd_: currently only the command "batch" is supported
- _-settings_: path to the settings-xml file (should be under the same directory as the root)
- _-pre_: path to the filter-queue-xml file (should be under the same directory as the root) 
- _-area_: area plugin class with full-qualified-name, for example core.cell.plugins.CellPluginBenjamin or core.cell.plugins.CellPluginSimple
- _-root_: the root directory from which the batch processing should start

## Example Call ##
java --module-path %JAVA_FX_HOME%\lib --add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.swt -jar __JARNAME__.jar -root "D:\Development\IdeaProjects\FilamentSensor\test\TestSmallSet\orig" -cmd batch -settings "D:\Development\IdeaProjects\FilamentSensor\test\TestSmallSet\specialSettings.xml" -pre "D:\Development\IdeaProjects\FilamentSensor\test\TestSmallSet\testFilterQueue.xml" -area core.cell.plugins.CellPluginSimple