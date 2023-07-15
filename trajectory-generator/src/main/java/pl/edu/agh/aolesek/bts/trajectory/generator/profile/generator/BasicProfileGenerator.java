package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import static pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters.BASE_NUMBER_OF_VISITED_POIS;
import static pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters.PREF_TRANSPORT_MODE_FACTOR;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Preferences;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Preferences.ActivityTime;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Profile;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils;

//klasa abstrakcyjna generująca zdefiniowane profile przy użyciu interfejsu IProfileGenerator
public abstract class BasicProfileGenerator implements IProfileGenerator {

    protected static final Random RND = new Random();

    private final Config config;

    public BasicProfileGenerator(Config config) {
        this.config = config;
    }

    @Override
    public Collection<IProfile> generateProfiles(int numberOfProfiles) {
        return IntStream.range(0, numberOfProfiles)
            .mapToObj(i -> generateProfile())
            .collect(Collectors.toSet());
    }

    private Profile generateProfile() {
        final Collection<Pair<String, Double>> interests = RandomUtils.normalize(generateInterests());
        final Collection<Pair<String, Double>> prefferedTransportModes = RandomUtils.normalize(generatePreferredTransportModes());

        return Profile.builder().interests(interests)
            .prefferedTransportModes(prefferedTransportModes)
            .materialStatus(generateMaterialStatus())
            .preferences(randomPreferences())
            .placeOfDeparture(randomPlaceOfDeparture())
            .maxRange(generateMaxDistance())
            .build();
    }

    private Collection<Pair<String, Double>> generateInterests() {
        final int numberOfInterests = RandomUtils.deviated(config.getInt(Parameters.BASE_NUMBER_OF_INTERESTS),
            config.getInt(Parameters.BASE_NUMBER_OF_INTERESTS_DEVIATION));
        final Set<String> usedAmenities = new HashSet<>();
        return IntStream.range(0, numberOfInterests > getAmenities().size() ? getAmenities().size() : numberOfInterests)
            .mapToObj(interestNumber -> generateInterest(usedAmenities))
            .collect(Collectors.toSet());
    }

    private Pair<String, Double> generateInterest(final Set<String> usedAmenities) {
        String amenity = getAmenities().get(RND.nextInt(getAmenities().size()));
        while (usedAmenities.contains(amenity) && usedAmenities.size() < getAmenities().size()) {
            amenity = getAmenities().get(RND.nextInt(getAmenities().size()));
        }
        usedAmenities.add(amenity);
        final double level = RND.nextDouble();
        return Pair.create(amenity, level);
    }

    public abstract List<String> getAmenities();

    private Collection<Pair<String, Double>> generatePreferredTransportModes() {
        final Set<Pair<String, Double>> prefferedTransportModes = new HashSet<>();
        prefferedTransportModes
            .add(Pair.create(getPrefferedTransportMode(), RND.nextDouble() + config.getDouble(PREF_TRANSPORT_MODE_FACTOR)));

        if (getTransportModes().size() > 1) {
            int numberOfAdditionalTransportModes = RND.nextInt(2 + getTransportModes().size() - 1);
            IntStream.range(0, numberOfAdditionalTransportModes)
                .mapToObj(modeNumber -> {
                    final String mode = getTransportModes().get(RND.nextInt(getTransportModes().size()));
                    final double level = RND.nextDouble();
                    return Pair.create(mode, level);
                }).forEach(generatedMode -> {
                    final boolean notAlreadyInCollection = prefferedTransportModes.stream()
                        .noneMatch(mode -> Objects.equals(mode.getFirst(), generatedMode.getFirst()));
                    if (notAlreadyInCollection) {
                        prefferedTransportModes.add(generatedMode);
                    }
                });
        }
        return RandomUtils.normalize(prefferedTransportModes);
    }

    protected abstract String getPrefferedTransportMode();

    protected abstract List<String> getTransportModes();

    private Level generateMaterialStatus() {
        final Level[] levels = Level.values();
        int levelNumber = RND.nextInt(levels.length);
        return levels[levelNumber];
    }

    public Preferences randomPreferences() {
        final ActivityTime[] levels = ActivityTime.values();
        final int levelNumber = RND.nextInt(levels.length);
        final ActivityTime activityTime = levels[levelNumber];
        final int pois = RandomUtils.deviated(config.getInt(BASE_NUMBER_OF_VISITED_POIS),
            config.getInt(Parameters.BASE_NUMBER_OF_VISITED_POIS_DEVIATION));

        final double speed = RandomUtils.deviatedByPercentage(config.getDouble(Parameters.WALKING_SPEED_MODIFIER_BASE),
            config.getDouble(Parameters.WALKING_SPEED_MODIFIER_BASE_DEVIATION));
        final List<Double> distributedValues = RandomUtils.distributedValues(3);
        return new Preferences(activityTime, pois, 0.8 + 0.4 * RND.nextDouble(), speed, distributedValues.get(0), distributedValues.get(1),
            distributedValues.get(2));
    }

    public abstract Point randomPlaceOfDeparture();

    private double generateMaxDistance() {
        return RandomUtils.deviatedPositive(config.getDouble(Parameters.MAX_DISTANCE_FROM_START_BASE),
            config.getDouble(Parameters.MAX_DISTANCE_FROM_START_BASE_DEVIATION));
    }

}
