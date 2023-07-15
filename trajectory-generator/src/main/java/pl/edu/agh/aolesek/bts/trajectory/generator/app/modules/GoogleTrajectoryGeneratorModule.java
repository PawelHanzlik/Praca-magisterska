package pl.edu.agh.aolesek.bts.trajectory.generator.app.modules;

import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.ErrorHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider.GoogleAdditionalDataProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.CsvTrajectoryResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.GeoJSONResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.IResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.JSONResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.OneGeoJSONResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.StoryResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPricesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.ISpentTimeProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.RandomSpentTimeProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.google.GooglePoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IPoiPlanner;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.google.GooglePoiPlanner;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.DummyRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.IPoiRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.google.DirectionsRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.IProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.IProfilesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.ProfilesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.google.GoogleBasicProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.validator.ITrajectoryValidator;
import pl.edu.agh.aolesek.bts.trajectory.generator.validator.TrajectoryTimeValidator;

//zagnieżdżanie hierarchii dziedziczenia klas i interfejsów przy wyborze usług Google
public class GoogleTrajectoryGeneratorModule extends AbstractTrajectoryGeneratorModule {

    public GoogleTrajectoryGeneratorModule(Config generatorParameters, ErrorHandler errorHandler) {
        super(generatorParameters, errorHandler);
    }

    protected void configure() {
        bind(ErrorHandler.class).toInstance(errorHandler);
        bind(Config.class).toInstance(generatorParameters);
        bind(IProfileGenerator.class).to(GoogleBasicProfileGenerator.class);
        bind(new TypeLiteral<IPoiGenerator<? extends IPoi>>() {}).to(GooglePoiGenerator.class);
        bind(new TypeLiteral<IPoiPlanner<? extends IPoi>>() {}).to(GooglePoiPlanner.class);
        bind(IPoiRouter.class).to(DirectionsRouter.class);
        bind(IProfilesProvider.class).to(ProfilesProvider.class);
        bind(ISpentTimeProvider.class).to(RandomSpentTimeProvider.class);
        bind(IOpeningHoursSupplier.class).to(GoogleAdditionalDataProvider.class);
        bind(IPricesProvider.class).to(GoogleAdditionalDataProvider.class);
        bind(IPoiRecommender.class).to(DummyRecommender.class).in(Singleton.class);

        Multibinder<IResultHandler> outputBinder = Multibinder.newSetBinder(binder(), IResultHandler.class);
        outputBinder.addBinding().to(JSONResultHandler.class);
        outputBinder.addBinding().to(GeoJSONResultHandler.class);
        outputBinder.addBinding().to(CsvTrajectoryResultHandler.class);
        outputBinder.addBinding().to(StoryResultHandler.class);
        outputBinder.addBinding().to(OneGeoJSONResultHandler.class);

        Multibinder<ITrajectoryValidator> validatorBinder = Multibinder.newSetBinder(binder(), ITrajectoryValidator.class);
        validatorBinder.addBinding().to(TrajectoryTimeValidator.class);
    }
}
