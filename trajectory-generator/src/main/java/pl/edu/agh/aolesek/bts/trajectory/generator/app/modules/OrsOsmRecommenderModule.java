package pl.edu.agh.aolesek.bts.trajectory.generator.app.modules;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.ErrorHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.*;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.*;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.OsmPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IPoiPlanner;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.osm.OsmOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.osm.OsmPoiPlanner;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.ALSPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.IPoiRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.ors.OpenRouteServicePoiRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.IProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.IProfilesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.ProfilesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.osm.OsmBasicProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.validator.ITrajectoryValidator;
import pl.edu.agh.aolesek.bts.trajectory.generator.validator.TrajectoryTimeValidator;

//zagnieżdżanie hierarchii dziedziczenia klas i interfejsów przy wyborze usług opartych o OSM
public class OrsOsmRecommenderModule extends AbstractTrajectoryGeneratorModule {

    public OrsOsmRecommenderModule(Config generatorParameters, ErrorHandler errorHandler) {
        super(generatorParameters, errorHandler);
    }

    protected void configure() {
        bind(ErrorHandler.class).toInstance(errorHandler);
        bind(Config.class).toInstance(generatorParameters);
        bind(IProfileGenerator.class).to(OsmBasicProfileGenerator.class).in(Singleton.class);
        bind(new TypeLiteral<IPoiGenerator<? extends IPoi>>() {}).to(OsmPoiGenerator.class).in(Singleton.class);
        bind(new TypeLiteral<IPoiPlanner<? extends IPoi>>() {}).to(OsmPoiPlanner.class).in(Singleton.class);
        bind(IPoiRouter.class).to(OpenRouteServicePoiRouter.class).in(Singleton.class);
        bind(IProfilesProvider.class).to(ProfilesProvider.class).in(Singleton.class);
        bind(ISpentTimeProvider.class).to(RandomSpentTimeProvider.class).in(Singleton.class);
        bind(IOpeningHoursSupplier.class).to(OsmOpeningHoursSupplier.class).in(Singleton.class);
        bind(IPricesProvider.class).to(RandomPricesProvider.class).in(Singleton.class);

        //różnica między klasami OrsOsmRecommenderModule i OrsOsmTrajectoryGeneratorModule
        bind(IPoiRecommender.class).to(ALSPoiRecommender.class).in(Singleton.class);

        Multibinder<IResultHandler> outputBinder = Multibinder.newSetBinder(binder(), IResultHandler.class);

        // Choose desired output handlers.
        outputBinder.addBinding().to(JSONResultHandler.class);
        outputBinder.addBinding().to(StoryResultHandler.class);
        outputBinder.addBinding().to(OneGeoJSONResultHandler.class);
        outputBinder.addBinding().to(GeoJSONResultHandler.class);
        outputBinder.addBinding().to(CsvTrajectoryResultHandler.class);

        Multibinder<ITrajectoryValidator> validatorBinder = Multibinder.newSetBinder(binder(), ITrajectoryValidator.class);
        validatorBinder.addBinding().to(TrajectoryTimeValidator.class);
    }
}
