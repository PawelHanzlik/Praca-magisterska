package pl.edu.agh.aolesek.bts.trajectory.generator.app.modules;

import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.ErrorHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;

import java.util.Collections;
import java.util.Set;

//moduły zajmują się zagnieżdżaniem interfejsów w klasach w singletonie
@Log4j2
public class TrajectoryGeneratorModulesFactory {

    public static final String OSM_MODULE = "OpenRouteServiceOverpass";

    public static final String OSM_RECOMMENDER_MODULE = "OpenRouteServiceOverpassRecommender";

    public static final String GOOGLE_MODULE = "GoogleMaps";

    //konfiguracje dodane na prośbę profesora

    public static final String OSM_GOOGLE = "OverpassGoogle";

    public static final String GOOGLE_OSM = "GoogleOpenRouteService";

    public Set<AbstractTrajectoryGeneratorModule> getModules(Config parameters, ErrorHandler errorHandler) {
        final String m = parameters.get(Parameters.MODULE);

        log.info("Selected modules: " + m); //new functionality shows selected modules
        //klasy przydzielające zagnieżdżenia dziedziczenia wywoływane są na podstawie danych z konfiguracji
        switch (m) {
            case OSM_MODULE:
                return orsOsm(parameters, errorHandler);
            case OSM_RECOMMENDER_MODULE:
                return orsOsmRecommender(parameters, errorHandler);
            case GOOGLE_MODULE:
                return google(parameters, errorHandler);
            case OSM_GOOGLE:
                return osmGoogle(parameters, errorHandler);
            case GOOGLE_OSM:
                return googleOsm(parameters, errorHandler);
            default:
                throw new IllegalArgumentException(getInvalidModuleMessage(m));
        }
    }

    private Set<AbstractTrajectoryGeneratorModule> orsOsm(Config parameters, ErrorHandler errorHandler) {
        return Collections.singleton(new OrsOsmTrajectoryGeneratorModule(parameters, errorHandler));
    }

    private Set<AbstractTrajectoryGeneratorModule> orsOsmRecommender(Config parameters, ErrorHandler errorHandler) {
        return Collections.singleton(new OrsOsmRecommenderModule(parameters, errorHandler));
    }

    private Set<AbstractTrajectoryGeneratorModule> google(Config parameters, ErrorHandler errorHandler) {
        return Collections.singleton(new GoogleTrajectoryGeneratorModule(parameters, errorHandler));
    }

    private Set<AbstractTrajectoryGeneratorModule> googleOsm(Config parameters, ErrorHandler errorHandler) {
        return Collections.singleton(new GoogleOsmTrajectoryGeneratorModule(parameters, errorHandler));
    }

    private Set<AbstractTrajectoryGeneratorModule> osmGoogle(Config parameters, ErrorHandler errorHandler) {
        return Collections.singleton(new OsmGoogleTrajectoryGeneratorModule(parameters, errorHandler));
    }

    private String getInvalidModuleMessage(String m) {
        return String.format("Selected module %s is not a valid generator module!", m);
    }
}
