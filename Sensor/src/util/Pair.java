package util;

/**
 * Extra implementation chosen because javafx.util.Pair has no Setter methods only getter
 *
 * @param <K>
 * @param <V>
 */
public class Pair<K, V> {

    protected K key;
    protected V value;

    public Pair(K key, V value) {
        this.setKey(key);
        this.setValue(value);
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public javafx.util.Pair<K, V> toFxPair() {
        return new javafx.util.Pair<>(key, value);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (getKey() != null ? !getKey().equals(pair.getKey()) : pair.getKey() != null) return false;
        return getValue() != null ? getValue().equals(pair.getValue()) : pair.getValue() == null;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
