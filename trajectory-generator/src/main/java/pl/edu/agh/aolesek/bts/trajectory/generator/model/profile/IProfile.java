package pl.edu.agh.aolesek.bts.trajectory.generator.model.profile;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;

import java.util.Collection;

//interfejs udostępniający informacje na temat profilu
public interface IProfile {

    String getFullName();

    String getId();

    Collection<Pair<String, Double>> getInterests();

    Collection<Pair<String, Double>> getPrefferedTransportModes();

    Level getMaterialStatus();

    Preferences getPreferences();

    Point getPlaceOfDeparture();

    double getMaxRange();

    double rateAttractiveness(double interestRate, double priceRate, double distanceRate);

    ProfileLogger getLogger();
}
