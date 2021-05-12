package util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class Annotations {

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public static @interface NotNull {

    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    public static @interface Nullable {

    }

    //Keep the Annotation-Info during runtime
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public static @interface FilterUI {//describes basic information of a filter (just its name), if not present(default) simple class name is used

        public String label() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface FilterUIField {
        public FilterUIType type() default FilterUIType.none;

        public String label() default "";//if left empty, annotated attribute name is taken
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface Min {
        public double value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface Max {
        public double value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface Step {
        public int value() default 1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface NumberFormat {
        public String value() default "Double";//Integer or Double
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface Values {
        public String value() default "";//for lists,combobox... is either a string with values separated by ,
        //or a full class name of a value supplier implementing CBSupplier
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public static @interface Default {
        public String value() default "";//either a settings-key (for example: Pre.fsigma) or a value itself
        //has to be a enum or a value nothing else!
    }

    /**
     * Represents the UI-type of a filter-attribute (none means no ui element needed)
     */
    public static enum FilterUIType {
        slider, checkbox, combobox, none
    }


}
