package pl.edu.agh.aolesek.bts.trajectory.generator.retrofit;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.HashMap;
import java.util.Map;

//klient URL; moduł do tworzenia połączeń html
public class RetrofitClientFactory {

	private static Map<String, Retrofit> clientsCache = new HashMap<>();

	private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

	public static Retrofit getRetrofitInstance(String baseApiUrl) {
		if (clientsCache.get(baseApiUrl) == null) {
			clientsCache.put(baseApiUrl, new Retrofit.Builder()
					.client(CLIENT)
					.baseUrl(baseApiUrl)
					.addConverterFactory(GsonConverterFactory.create())
					.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
					.build());
		}
		return clientsCache.get(baseApiUrl);
	}

	public static Retrofit getRetrofitInstanceRaw(String baseApiUrl) {
		if (clientsCache.get(baseApiUrl) == null) {
			clientsCache.put(baseApiUrl, new Retrofit.Builder()
					.client(CLIENT)
					.baseUrl(baseApiUrl)
					.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
					.build());
		}
		return clientsCache.get(baseApiUrl);
	}

	public static void shutdown() {
		CLIENT.dispatcher().executorService().shutdown();
		CLIENT.connectionPool().evictAll();
	}
}
