package focaladhesion;

import core.settings.Settings;
import core.settings.Export;
import core.cell.DataFilaments;
import core.image.CorrelationData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.summingInt;

public class FocalAdhesionContainer implements CorrelationData {

    private List<FocalAdhesion> data;


    //add the settings in here? so they are stored in xml? like boolean bothEnds and int neighborHoodSize
    //actually not directly related to the FocalAdhesion itself, its related to the verification process

    public FocalAdhesionContainer() {
        data = new ArrayList<>();
    }

    public FocalAdhesionContainer(Collection<FocalAdhesion> data) {
        this();
        this.data.addAll(data);
    }

    public List<FocalAdhesion> getData() {
        return data;
    }

    public List<FocalAdhesion> getFilteredData(DataFilaments dataFilaments, Settings dp) {
        boolean hideUnusedFAs = dp.getValueAsBoolean(Export.hideUnusedFAs);
        boolean hideSingleUsedFAs = dp.getValueAsBoolean(Export.hideSingleUsedFAs);
        boolean hideMultiUsedFAs = dp.getValueAsBoolean(Export.hideMultiUsedFAs);


        List<FocalAdhesion> result = getData();

        if (dataFilaments == null || dataFilaments.getFilaments() == null || dataFilaments.getFilaments().size() == 0)
            return getData();

        //hides single used fa's
        if (hideSingleUsedFAs) {
            if (dataFilaments.getFilaments().stream().anyMatch(f -> f.getVerifier() != null)) {
                Map<Integer, Integer> collect = dataFilaments.getFilaments().stream().filter(f -> f.getVerifier() != null).flatMap(f -> f.getVerifier().getId().stream()).collect(Collectors.groupingBy(Function.identity(), summingInt(e -> 1)));
                List<Integer> faIds = collect.entrySet().stream().filter(e -> e.getValue() != 1).map(Map.Entry::getKey).collect(Collectors.toList());
                result = result.stream().filter(fa -> faIds.contains(fa.getNumber())).collect(Collectors.toList());
            }
        }


        if (hideMultiUsedFAs) {
            if (dataFilaments.getFilaments().stream().anyMatch(f -> f.getVerifier() != null)) {
                Map<Integer, Integer> collect = dataFilaments.getFilaments().stream().filter(f -> f.getVerifier() != null).flatMap(f -> f.getVerifier().getId().stream()).collect(Collectors.groupingBy(Function.identity(), summingInt(e -> 1)));
                List<Integer> faIds = collect.entrySet().stream().filter(e -> e.getValue() < 2).map(Map.Entry::getKey).collect(Collectors.toList());
                result = result.stream().filter(fa -> faIds.contains(fa.getNumber())).collect(Collectors.toList());
            }
        }

        if (!hideUnusedFAs && (hideSingleUsedFAs || hideMultiUsedFAs)) {
            //add unused fa's (since they are filtered out in hide single and hide multi used)
            List<Integer> faIdsAnyVerified = dataFilaments.getFilaments().stream().filter(f -> f.getVerifier() != null).flatMap(f -> f.getVerifier().getId().stream()).distinct().collect(Collectors.toList());
            result.addAll(getData().stream().filter(fa -> !faIdsAnyVerified.contains(fa.getNumber())).collect(Collectors.toList()));
        }

        if (hideUnusedFAs) {
            if (dataFilaments.getFilaments().stream().anyMatch(f -> f.getVerifier() != null)) {
                List<Integer> faIds = dataFilaments.getFilaments().stream().filter(f -> f.getVerifier() != null).flatMap(f -> f.getVerifier().getId().stream()).distinct().collect(Collectors.toList());
                result = result.stream().filter(fa -> faIds.contains(fa.getNumber())).collect(Collectors.toList());
            }
        }

        return result;

    }

    public void setData(List<FocalAdhesion> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "FocalAdhesionContainer{" +
                "data=" + data +
                '}';
    }
}
