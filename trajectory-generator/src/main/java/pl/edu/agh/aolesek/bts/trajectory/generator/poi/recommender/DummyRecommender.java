package pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.util.Collection;
import java.util.Collections;

//słowo dummy w języku angielskim oznacza manekin. Jest to pusta klasa tworzona jeśli wybierzemy konfigurację bez rekomendacji.
// Składają się na nią trzy puste metody void i jedna zwracająca pusty zestaw danych. Taka konfiguracja jest przydatna, jeśli na komputerze nie ma Sparka
public class DummyRecommender implements IPoiRecommender {

    @Override
    public void storePoi(IPoi poi) {
    }

    @Override
    public void addRating(IProfile profile, IPoi poi, double rating) {
    }

    @Override
    public Collection<Pair<IPoi, Double>> recommend(IProfile profile) {
        return Collections.emptySet();
    }

    @Override
    public void stopContext() {
    }
}
