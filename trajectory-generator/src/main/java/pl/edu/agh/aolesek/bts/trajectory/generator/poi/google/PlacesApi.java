package pl.edu.agh.aolesek.bts.trajectory.generator.poi.google;

import io.reactivex.Single;
import pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider.GoogleDetailsModel;
import retrofit2.http.GET;
import retrofit2.http.Query;

//interfejs odpowiedzialny za bezpośrednie przesyłanie danych do usługi z wykorzystaniem metod GET.
// Metoda searchPois znajduje najbliższe Poi znajdujące się w określonym promieniu od bieżącej lokalizacji.
// Metoda details znajduje szczegółowe informacje na temat Poi
public interface PlacesApi {

    @GET("place/nearbysearch/json")
    Single<GooglePoiModel> searchPois(@Query("location") String location, @Query("radius") Long radius, @Query("keyword") String keyword,
        @Query("key") String apiKey);

    @GET("place/details/json")
    Single<GoogleDetailsModel> details(@Query("place_id") String placeId, @Query("fields") String fields, @Query("key") String apiKey);
}
