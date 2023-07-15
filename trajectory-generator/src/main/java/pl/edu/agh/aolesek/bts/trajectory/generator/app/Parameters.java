package pl.edu.agh.aolesek.bts.trajectory.generator.app;

public enum Parameters {
    /**
     * URL of Overpass API. An example of free Overpass API is https://lz4.overpass-api.de/api/.
     */
    OVERPASS_URL,

    /**
     * API Key for Overpass API. Required only for some API instances.
     */
    OVERPASS_KEY,

    /**
     * URL of OpenRouteService API instance. An example of free ORS API is https://api.openrouteservice.org/v2/
     */
    ORS_URL,

    /**
     * API Key for ORS API. Required only for some API instances.
     */
    ORS_KEY,

    /**
     * URL of Google Maps API. e. g. https://maps.googleapis.com/maps/api/
     */
    GOOGLE_API_URL,

    /**
     * API Key for Google Maps API.
     */
    GOOGLE_API_KEY,

    /**
     * Number of generated profiles. The total number of profiles is sum of profiles from INPUT_PROFILES_DIRECTORY and generated profiles.
     */
    NUMBER_OF_GENERATED_PROFILES,

    /**
     * Number of trajectories per one (generated or provided) profile.
     */
    NUMBER_OF_TRAJECTORIES_PER_PROFILE,

    /**
     * Factor used to simulate preference of one of available transport modes.
     */
    PREF_TRANSPORT_MODE_FACTOR,

    /**
     * Base number that will be used to determine number of POIs to visit in one trajectory.
     */
    BASE_NUMBER_OF_VISITED_POIS,
    BASE_NUMBER_OF_VISITED_POIS_DEVIATION,

    /**
     * Numbers used to generate walking speed for trajectories.
     */
    WALKING_SPEED_MODIFIER_BASE,
    WALKING_SPEED_MODIFIER_BASE_DEVIATION,
    AVERAGE_WALKING_SPEED,

    /**
     * Numbers used to determine max distance from start point for trajectory.
     */
    MAX_DISTANCE_FROM_START_BASE,
    MAX_DISTANCE_FROM_START_BASE_DEVIATION,

    /**
     * Numbers used to provide random time of visit if such data is not available from the provider.
     */
    GENERATED_SPENT_SECONDS_BASE,
    GENERATED_SPENT_SECONDS_MAX,
    GENERATED_SPENT_SECONDS_BASE_PERCENTAGE_DEVIATION,

    /**
     * Max number of recommendations for one trajectory. Works only if proper POIRecommender is set.
     */
    MAX_NUMBER_OF_RECOMMENDED_POIS_FOR_TRAJECTORY,

    /**
     * Below this distance, generator will shortcut some points of routing. In case of transport methods other than foot walking, routing
     * received from external provider may not be complete, e.g. when driving a car, it is sometimes impossible to reach target point
     * directly by car. In such a cases, generator will query external provider for routing from SOURCE point to ROUTE START POINT (e. g.
     * from office to parking), but querying external provider will be performed only if distance from SOURCE point to ROUTE START POINT is
     * greater than this variable.
     */
    MAX_DISTANCE_TO_SHORTCUT,

    /**
     * Should execute validators for generted trajectories.
     */
    SHOULD_VALIDATE,

    /**
     * Constants used to configure preferences regarding multiple visits in POI of same category.
     */
    CATEGORIES_TO_AVOID_MULTIPLE_VISITS,
    MAX_POIS_WITH_SAME_CATEGORY,
    MAX_POIS_WITH_SAME_CATEGORY_WHEN_AVOIDING,

    /**
     * Every profile has its preferred number of visited POIs. This number is used to generate random number based on mentioned variable.
     */
    AVERAGE_NUMBER_OF_VISITED_POIS_DEVIATION,

    /**
     * Modules set used for building context for validator. Possible values available in {@link TrajectoryGeneratorModulesFactory}:
     * OpenRouteServiceAndOverpass, OpenRouteServiceOverpassRecommender, GoogleMapsAPI
     */
    MODULE,

    /**
     * Should simulated tourist go to home after visiting all POIs.
     */
    SHOULD_GO_BACK_AT_THE_END,

    /**
     * Directory with profiles to use while generating. Directory to save results.
     */
    INPUT_PROFILES_DIRECTORY,
    OUTPUT_DIRECTORY,

    /**
     * Determines whether output files will be generated one file per profile/one file per result handler.
     */
    OUTPUT_IN_ONE_FILE,

    /**
     * Numbers used while generating random profiles.
     */
    BASE_NUMBER_OF_INTERESTS,
    BASE_NUMBER_OF_INTERESTS_DEVIATION,

    /**
     * ALS Poi recommender requires to build model for providing recommendations. The model will be built every BUILD_MODEL_EVERY_N_RATINGS
     * ratings added to recommender.
     */
    BUILD_MODEL_EVERY_N_RATINGS,
}
