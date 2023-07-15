package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.ors;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

//interfejs bezpośrednio wysyłający zapytanie do usługi z wykorzystaniem metody GET.
// Posiada jedną funkcję route wyznaczającą trasę między początkiem i końcem odcinka z uwzględnieniem środków transportu
public interface OpenRouteServiceApi {

    @GET("directions/{transportMode}")
    Single<ResponseBody> route(@Path("transportMode") String transportMode, @Query("api_key") String apiKey,
        @Query(value = "start", encoded = false) String start,
        @Query(value = "end", encoded = false) String end);
}
