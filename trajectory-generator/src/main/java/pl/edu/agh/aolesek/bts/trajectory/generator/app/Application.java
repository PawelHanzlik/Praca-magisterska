package pl.edu.agh.aolesek.bts.trajectory.generator.app;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.TrajectoryGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.persistence.IResultHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.validator.ITrajectoryValidator;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Log4j2
public class Application {

    private final TrajectoryGenerator<? extends IPoi> trajectoryGenerator;

    private final Set<ITrajectoryValidator> validators;

    private final Set<IResultHandler> resultHandlers;

    private final Config config;

    private final IPoiRecommender recommender;

    //tu za pomocą interfejsów z przedrostkiem I tworzone są instancje obiektów aplikacji
    //Poi->Poi to wiadomo obiekt na mapie
    //ITrejectoryGenerator->TrajectoryGenerator generowanie trajektorii
    //IResultHandler jest związany z kilkoma ResultHandlerami dla plików wyjściowych w różnych formatach (moduł persistence)
    //ITrajectoryValidator->TrajectoryTimeValidator sprawdza błędy w trajektoriach
    //config obiekt zawierający dane z pliku konfiguracyjnego
    //IPoiRecommender->DummyRecommender,ALSPoiRecommender rekomendacje (moduł recommender)
    @Inject
    public Application(TrajectoryGenerator<? extends IPoi> trajectoryGenerator, Set<IResultHandler> resultHandlers,
        Set<ITrajectoryValidator> validators, Config config, IPoiRecommender recommender) {
        this.trajectoryGenerator = trajectoryGenerator;
        this.resultHandlers = resultHandlers;
        this.validators = validators;
        this.config = config;
        this.recommender = recommender;
    }

    //uruchamia aplikację widoczną w konsoli
    public void start() {
        try {
            long start = System.currentTimeMillis();
            //obsługa Log4j2 - log widoczny w konsoli
            log.info("Generating trajectories...");
            //uruchomienie właściwej części aplikacji
            final Collection<ProfileResult> results = Collections.unmodifiableCollection(trajectoryGenerator.generateTrajectories());
            log.info(String.format("Finished generating trajectories after %d ms.", System.currentTimeMillis() - start));

            if (config.getBoolean(Parameters.SHOULD_VALIDATE)) {
                start = System.currentTimeMillis();
                log.info("Validating trajectories...");
                results.forEach(result -> {
                    log.info("Validating trajectory for " + result.getProfile().getFullName() + ".");
                    validators.forEach(validator -> validator.validate(result)); //walidacja
                });
                log.info(String.format("Finished validating trajectories after %d ms.", System.currentTimeMillis() - start));
            }

            start = System.currentTimeMillis();
            log.info("Handling generated trajectories...");
            resultHandlers.forEach(handler -> {
                try {
                    handler.handleResult(results);
                } catch (Exception e) {
                    log.error("Unable to handler result for " + handler.getClass().getSimpleName() + ". An exception occurred: " + e, e);
                }
            });
            log.info(String.format("Finished handling generated trajectories after %d ms. %d handlers found.",
                System.currentTimeMillis() - start, resultHandlers.size()));
        } finally {
            recommender.stopContext();
        }
    }
}
