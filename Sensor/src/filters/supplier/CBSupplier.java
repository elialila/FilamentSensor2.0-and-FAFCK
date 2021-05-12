package filters.supplier;


import java.util.List;

public interface CBSupplier<T> {

    T getValue(String toMap);

    String get(Object value);

    List<String> get();
}
