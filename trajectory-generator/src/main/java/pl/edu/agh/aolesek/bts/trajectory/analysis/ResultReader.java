package pl.edu.agh.aolesek.bts.trajectory.analysis;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Profile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.Trajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.Poi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.google.GooglePoiAdapter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.OsmPoiAdapter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.RoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.RoutePlan;

public class ResultReader {

    private final Collection<String> resultFilePath;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(IProfile.class, InterfaceSerializer.interfaceSerializer(Profile.class))
        .registerTypeAdapter(IPoi.class, new PoiInterfaceSerializer())
        .registerTypeAdapter(ITrajectory.class, InterfaceSerializer.interfaceSerializer(Trajectory.class))
        .registerTypeAdapter(IRoutePlan.class, InterfaceSerializer.interfaceSerializer(RoutePlan.class))
        .registerTypeAdapter(IRoutePart.class, InterfaceSerializer.interfaceSerializer(RoutePart.class))
        .create();

    public ResultReader(Collection<String> resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public Collection<ProfileResult> read() {
        return resultFilePath.stream().map(this::readSingleFile).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private Collection<ProfileResult> readSingleFile(String path) {
        try (Reader reader = Files.newBufferedReader(Paths.get(path));) {
            final Type typeToken = new TypeToken<Map<String, List<ProfileResult>>>() {}.getType();
            final Map<String, List<ProfileResult>> results = gson.fromJson(reader, typeToken);
            return results.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class InterfaceSerializer<T> implements JsonSerializer<T>, JsonDeserializer<T> {

        private final Class<T> implementationClass;

        private InterfaceSerializer(final Class<T> implementationClass) {
            this.implementationClass = implementationClass;
        }

        public static <T> InterfaceSerializer<T> interfaceSerializer(final Class<T> implementationClass) {
            return new InterfaceSerializer<>(implementationClass);
        }

        @Override
        public JsonElement serialize(final T value, final Type type, final JsonSerializationContext context) {
            final Type targetType = value != null
                ? value.getClass() // `type` can be an interface so Gson would not even try to traverse the fields, just pick the
                                   // implementation class
                : type; // if not, then delegate further
            return context.serialize(value, targetType);
        }

        @Override
        public T deserialize(final JsonElement jsonElement, final Type typeOfT, final JsonDeserializationContext context) {
            return context.deserialize(jsonElement, implementationClass);
        }
    }

    static class PoiInterfaceSerializer implements JsonSerializer<IPoi>, JsonDeserializer<IPoi> {

        @Override
        public JsonElement serialize(final IPoi value, final Type type, final JsonSerializationContext context) {
            final Type targetType = value != null
                ? value.getClass()
                : type; // if not, then delegate further
            return context.serialize(value, targetType);
        }

        @Override
        public IPoi deserialize(final JsonElement jsonElement, final Type typeOfT, final JsonDeserializationContext context) {
            if (jsonElement.getAsJsonObject().get("osmModel") != null) {
                return context.deserialize(jsonElement, OsmPoiAdapter.class);
            }
            if (jsonElement.getAsJsonObject().get("googlePoi") != null) {
                return context.deserialize(jsonElement, GooglePoiAdapter.class);
            }
            return context.deserialize(jsonElement, Poi.class);
        }
    }
}
