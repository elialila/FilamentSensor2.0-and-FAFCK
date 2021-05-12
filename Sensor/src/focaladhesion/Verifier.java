package focaladhesion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Verifier implements Serializable {
    enum VerificationMethod {focalAdhesion;}

    private VerificationMethod verificationMethod;
    private List<Integer> id;//id's of the related elements
    //its a list because in case of focal adhesion: a filament can be verified by two focal adhesion's (start and end)

    public Verifier() {
        id = new ArrayList<>();
    }

    private Verifier(VerificationMethod verificationMethod, Integer... ids) {
        this();
        id.addAll(Arrays.stream(ids).collect(Collectors.toList()));
        setVerificationMethod(verificationMethod);
    }

    public VerificationMethod getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(VerificationMethod verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public List<Integer> getId() {
        return id;
    }

    public void setId(List<Integer> id) {
        this.id = id;
    }

    public static Verifier focalAdhesion(Integer... ids) {
        return new Verifier(VerificationMethod.focalAdhesion, ids);
    }


}
