package pl.edu.agh.aolesek.bts.trajectory.generator.model.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.ProfileValidator;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

//tworzenie obiektów wchodzących w skład profilu.
@Data
public class Profile implements IProfile {

    private final String fullName;

    private final String id;

    private final Collection<Pair<String, Double>> interests;

    private final Level materialStatus;

    private final Collection<Pair<String, Double>> prefferedTransportModes;

    private final Preferences preferences;

    private final Point placeOfDeparture;

    private final double maxRange;

    @JsonIgnore
    private transient final ProfileLogger log;

    //konstruowanie profilu - tworzenie obiektów
    public Profile() {
        fullName = NamesRepository.createNewName();
        id = UUID.randomUUID().toString();
        interests = null;
        materialStatus = null;
        prefferedTransportModes = null;
        preferences = null;
        placeOfDeparture = null;
        maxRange = 0;
        log = new ProfileLogger(this);
    }

    //konstruowanie profilu - przypisywanie obiektów
    @Builder
    public Profile(String id, Collection<Pair<String, Double>> interests, Level materialStatus,
        Collection<Pair<String, Double>> prefferedTransportModes, Preferences preferences, Point placeOfDeparture, double maxRange) {
        fullName = NamesRepository.createNewName();
        log = new ProfileLogger(this);

        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.interests = interests;
        this.materialStatus = materialStatus;
        this.prefferedTransportModes = prefferedTransportModes;
        this.preferences = preferences;
        this.placeOfDeparture = placeOfDeparture;
        this.maxRange = maxRange;
        ProfileValidator.validate(this);
    }

    //ocenianie atrakcyjności obiektu
    @Override
    public double rateAttractiveness(double interestRate, double priceRate, double distanceRate) {
        return preferences.getInterestFactor() * interestRate + preferences.getPriceFactor() * priceRate
            + preferences.getDistanceFactor() * distanceRate;
    }

    //sprawdzanie identyczności profili
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Profile)) {
            return false;
        }
        Profile other = (Profile)obj;
        return Objects.equals(id, other.id) && Objects.equals(interests, other.interests) && materialStatus == other.materialStatus
            && Double.doubleToLongBits(maxRange) == Double.doubleToLongBits(other.maxRange)
            && Objects.equals(placeOfDeparture, other.placeOfDeparture) && Objects.equals(preferences, other.preferences)
            && Objects.equals(prefferedTransportModes, other.prefferedTransportModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, interests, materialStatus, maxRange, placeOfDeparture, preferences, prefferedTransportModes);
    }

    //konwersja do stringa
    @Override
    public String toString() {
        return "Profile [id=" + id + ", interests=" + interests + ", materialStatus=" + materialStatus + ", prefferedTransportModes="
            + prefferedTransportModes + ", preferences=" + preferences + ", placeOfDeparture=" + placeOfDeparture + ", maxRange="
            + maxRange + "]";
    }

    //pobieranie loggera
    @Override
    public ProfileLogger getLogger() {
        return log;
    }
}
