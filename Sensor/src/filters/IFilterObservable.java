package filters;

import javafx.beans.value.ChangeListener;

public interface IFilterObservable {

    void addListener(ChangeListener<Object> listener);

    void removeListener(ChangeListener<Object> listener);

}
