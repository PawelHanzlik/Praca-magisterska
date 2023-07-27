package pl.edu.agh.aolesek.bts.trajectory.generator.core;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.ErrorHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IPoiPlanner;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.IPoiRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.IProfilesProvider;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Log4j2
public class TrajectoryGenerator<T extends IPoi> implements ITrajectoryGenerator {

    private final IPoiGenerator<T> poiGenerator;

    private final IPoiPlanner<T> poiPlanner;

    private final IPoiRouter poiRouter;

    private final IProfilesProvider profilesProvider;

    private final ErrorHandler errorHandler;

    private final AtomicLong generatedTrajectories = new AtomicLong(0);

    private final AtomicLong totalTrajectories = new AtomicLong(0);

    private final Config config;

    private long startTime;

    //przypisanie odziedziczonych klas
    @Inject
    public TrajectoryGenerator(IPoiGenerator<T> poiGenerator, IPoiPlanner<T> poiPlanner, IPoiRouter poiRouter,
        IProfilesProvider profilesProvider, ErrorHandler errorHandler, Config config) {
        this.poiGenerator = poiGenerator;
        this.poiPlanner = poiPlanner;
        this.poiRouter = poiRouter;
        this.profilesProvider = profilesProvider;
        this.errorHandler = errorHandler;
        this.config = config;
    }

    @Override
    public Collection<ProfileResult> generateTrajectories() {
        startTime = System.currentTimeMillis(); //pobranie czasu
        try {
            final Collection<IProfile> profiles =profilesProvider.provideProfiles(); //pobranie profili
            //Sonarqube sugestia
            //totalTrajectories.set(profiles.size()*config.getInt(Parameters.NUMBER_OF_TRAJECTORIES_PER_PROFILE));
            //ustalenie liczby trajektorii do wygenerowania na podstawie konfiguracji
            totalTrajectories.set((long) profiles.size()*config.getInt(Parameters.NUMBER_OF_TRAJECTORIES_PER_PROFILE));
            return profiles.parallelStream()
                .map(this::generateTrajectoriesForSingleProfile)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        } finally {
            //informacje o czasie trwania
            final Duration generatingDuration = Duration.ofMillis(System.currentTimeMillis() - startTime);
            log.info(String.format("Finished after %s.", generatingDuration.toString()));
        }
    }

    private Collection<ProfileResult> generateTrajectoriesForSingleProfile(IProfile profile) {
        final int trajectoriesPerProfile = config.getInt(Parameters.NUMBER_OF_TRAJECTORIES_PER_PROFILE);
        return Collections.nCopies(trajectoriesPerProfile, profile).stream()
            .map(this::generateTrajectoryForSingleProfile)
            .collect(Collectors.toSet());
    }

    private ProfileResult generateTrajectoryForSingleProfile(IProfile profile) {
        final ProfileLogger profileLogger = profile.getLogger();
        Collection<PoiHolder<T>> poisForProfile = null;
        IRoutePlan routePlan = null;
        ITrajectory trajectory = null;


        try {
            long start = System.currentTimeMillis();
            profileLogger.debug("Generating POIs for profile " + profile.getFullName());
            poisForProfile = poiGenerator.generatePois(profile);
            System.out.println("KURWAAAAAAAAAAA");
            System.out.println(poisForProfile);
            profileLogger.debug(String.format("Generated %d POIs in %d ms", poisForProfile.size(), System.currentTimeMillis() - start));
            poiGenerator.logStats();

            start = System.currentTimeMillis();
            profileLogger.debug("Planning route for profile " + profile.getFullName());
            routePlan = poiPlanner.planRoute(poisForProfile);
            profileLogger
                .debug(String.format("Planned route that starts at %s and consists of %d parts after %d ms.", routePlan.getStartTime(),
                    routePlan.getParts().size(), System.currentTimeMillis() - start));

            start = System.currentTimeMillis();
            profileLogger.debug("Generating trajectory for profile " + profile.getFullName());
            trajectory = poiRouter.route(profile, routePlan);
            profileLogger.debug(String.format("Generated %d points trajectory in %d ms.", trajectory.getPointsWithTimestamps().size(),
                System.currentTimeMillis() - start));
            poiRouter.logStats();
        } catch (Exception e) {
            errorHandler.handleError(e, profile);
        }

        final long numberOfGeneratedTrajectories = generatedTrajectories.incrementAndGet();
        log.info(String.format("Generated %d of %d trajectories.", numberOfGeneratedTrajectories, totalTrajectories.get()));
        return new ProfileResult(profile, poisForProfile, routePlan, trajectory);
    }
}
