package pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm;

import io.reactivex.Single;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.model.OsmPoiModel;
import retrofit2.http.GET;
import retrofit2.http.Query;

//interfejs odpowiedzialny za bezpośrednie przesyłanied anych do usługi z wykorzystaniem metody GET.
// W przeciwieństwie do Google utworzona jest tylko jedna metoda searchPois.
public interface OverpassApi {

    @GET("interpreter")
    Single<OsmPoiModel> searchPois(@Query("data") String data);
}
