package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.google;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;

//maps/api/directions/json?origin=Toronto&destination=Montreal&avoid=highways &mode=bicycling&key=YOUR_API_KEY
//interfejs bezpośrednio wysyłający zapytanie do usługi z wykorzystaniem metody GET.
// Posiada jedną funkcję route wyznaczającą trasę między początkiem i końcem odcinka z uwzględnieniem środków transportu
public interface DirectionsApi {

	@GET("directions/json")
	Single<ResponseBody> route(@Query("origin") String origin, @Query("destination") String destination,
			@Query("mode") String mode, @Query("key") String apiKey);
}
