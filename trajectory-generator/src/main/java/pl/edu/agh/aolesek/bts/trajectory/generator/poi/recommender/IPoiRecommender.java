package pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.io.Serializable;
import java.util.Collection;

//interfejs przechowujący metody dziedziczone przez dwie powyższe klasy.
// Zawiera funkcje do przechowywania Poi, wyzanczania ocen, zatrzymywania działania recommendera i strukturę z wykorzystywanymi zmiennymi
public interface IPoiRecommender {

    void storePoi(IPoi poi);

    void addRating(IProfile profile, IPoi poi, double rating);

    Collection<Pair<IPoi, Double>> recommend(IProfile profile);

    public void stopContext();

    @RequiredArgsConstructor
    public class Rating implements Serializable {

        private static final long serialVersionUID = 1L;

        @Getter
        private final long userId;

        @Getter
        private final long poiId;

        @Getter
        private final double rating;

        @Getter
        private final long timestamp;
    }
}
