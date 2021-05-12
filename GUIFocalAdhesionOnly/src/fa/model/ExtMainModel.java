package fa.model;

import filters.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.MainModel;

public class ExtMainModel extends MainModel {


    private ObservableList<String> applicableFiltersFa;


    public ExtMainModel() {
        super();
        applicableFiltersFa = FXCollections.observableArrayList();
        //applicableFiltersFa.add(FilterClosing.class.getName());
        //applicableFiltersFa.add(FilterDilate.class.getName());
        applicableFiltersFa.add(FilterGauss.class.getName());
        //applicableFiltersFa.add(FilterErode.class.getName());
        //applicableFiltersFa.add(FilterFillHoles.class.getName());
        applicableFiltersFa.add(FilterLaPlace.class.getName());
        applicableFiltersFa.add(FilterLineGauss.class.getName());
        //applicableFiltersFa.add(FilterOpening.class.getName());
        applicableFiltersFa.add(FilterCrossCorrelation.class.getName());
        applicableFiltersFa.add(FilterEnhanceContrast.class.getName());
    }

    public ObservableList<String> getApplicableFiltersFa() {
        return applicableFiltersFa;
    }
}
