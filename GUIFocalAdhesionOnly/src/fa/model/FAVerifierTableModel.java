package fa.model;

import core.filaments.AbstractFilament;
import focaladhesion.FocalAdhesion;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polyline;

import java.util.List;
import java.util.stream.Collectors;

public class FAVerifierTableModel {
    private IntegerProperty filamentId;
    private StringProperty verifier;

    private AbstractFilament filament;
    private List<FocalAdhesion> listFocalAdhesion;

    private Polyline polyline;
    private List<Ellipse> ellipses;


    public FAVerifierTableModel() {
        filamentId = new SimpleIntegerProperty();
        verifier = new SimpleStringProperty();
    }

    public FAVerifierTableModel(AbstractFilament filament, Polyline polyline, List<Ellipse> ellipses) {
        this();
        setFilamentId(filament.getNumber());
        if (filament.getVerifier() != null)
            setVerifier(String.join(",", filament.getVerifier().getId().stream().map(i -> i.toString()).collect(Collectors.toList())));
        this.filament = filament;
        setPolyline(polyline);
        setEllipses(ellipses);
        setListFocalAdhesion(ellipses.stream().filter(ellipse -> ellipse.getUserData() instanceof FocalAdhesion).map(ellipse -> (FocalAdhesion) ellipse.getUserData()).collect(Collectors.toList()));
    }

    public List<FocalAdhesion> getListFocalAdhesion() {
        return listFocalAdhesion;
    }

    private void setListFocalAdhesion(List<FocalAdhesion> listFocalAdhesion) {
        this.listFocalAdhesion = listFocalAdhesion;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    private void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public List<Ellipse> getEllipses() {
        return ellipses;
    }

    private void setEllipses(List<Ellipse> ellipses) {
        this.ellipses = ellipses;
    }

    public AbstractFilament getFilament() {
        return filament;
    }

    public int getFilamentId() {
        return filamentId.get();
    }

    public IntegerProperty filamentIdProperty() {
        return filamentId;
    }

    public void setFilamentId(int filamentId) {
        this.filamentId.set(filamentId);
    }

    public String getVerifier() {
        return verifier.get();
    }

    public StringProperty verifierProperty() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier.set(verifier);
    }
}
