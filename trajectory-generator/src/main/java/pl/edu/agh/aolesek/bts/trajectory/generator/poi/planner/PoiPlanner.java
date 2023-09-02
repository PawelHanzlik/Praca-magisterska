package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import com.google.inject.Inject;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.TrajectoryHistoryOfPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Preferences.ActivityTime;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.Poi;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.ProbabilityUtils;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//klasa abstrakcyjna do planowania trasy korzystająca z interfejsu IPoPlanner i instancji interfejsu IOpeningHoursSupplier zwracającego informację
// o tym czy obiekt jest w danym momencie otwarty. W jej ramach losowana jest godzina rozpoczęcia podróży dla wskazanej w konfiguracji pory dnia.
// Dla oczekiwanego średniego czasu trwania wizyty wyznaczana jest wartość losowa oscylująca wokół niej. Wpływa to na realizm, bo nikt nie przebywa
// w danym Poi tyle samo czasu. Wybierany jest środek transportu z uwzględnieniem wskazanych w parametrach profilu prawdopodobieństw.
// Wyznaczane są kolejne Poi tworzące trasę. W skład klasy wchodzą również pomocnicze funkcje porównujące długość odcinków z danymi współrzędnymi końców,
// sprawdzające czy Poi są czynne, wyznaczające czas jaki zajmuje przebycie odcinka trasy, zwracające punkt początkowy całej wędrówki.
// Jeśli w konfiguracji jest zlecony powrót po zakończeniu wędrówki, metoda resolveReturningTransportMode wyznacza wykorzystany w tym celu środek transportu.
public abstract class PoiPlanner<T extends IPoi> implements IPoiPlanner<T> {

    protected final Config config;

    //interfejs zwracający informacje o tym czy obiekt jest otwarty
    private final IOpeningHoursSupplier openingHoursSupplier;

    private List<String> historyOfTransportModes = new ArrayList<>();
    //konstruktor
    @Inject
    public PoiPlanner(Config parameters, IOpeningHoursSupplier openingHoursSupplier) {
        this.config = parameters;
        this.openingHoursSupplier = openingHoursSupplier;
    }

    //planowanie trasy
    @Override
    public RoutePlan planRoute(Collection<PoiHolder<T>> poisForProfile, TrajectoryHistoryOfPoi<T> historyOfPoi) {
        if (poisForProfile.isEmpty()) {
            throw new IllegalArgumentException("You have to supply at least 1 poi for planner!");
        }

        final IProfile profile = poisForProfile.iterator().next().getProfile();
        final LocalDateTime startTime = resolveStartTime(profile);
        final AtomicReference<LocalDateTime> estimatedTimePointer = new AtomicReference<LocalDateTime>(startTime);

        final HashSet<PoiHolder<? extends IPoi>> poisToPlan = new HashSet<>(poisForProfile);
        final PoiHolder<? extends IPoi> placeOfDeparture = resolvePlaceOfDeparture(profile);
        PoiHolder<? extends IPoi> currentPoint = placeOfDeparture;
        final List<IRoutePart> routeParts = new LinkedList<>();

        while (!poisToPlan.isEmpty()) {
            final String transportMode = resolveTransportMode(profile);
            historyOfTransportModes.add(transportMode);
            final PoiHolder<? extends IPoi> bestPoi = getBestNextPoi(currentPoint, poisToPlan, estimatedTimePointer, transportMode, historyOfPoi);
            historyOfPoi.addToOrderOfPoi(bestPoi.getPoi().getCategory(), poisToPlan);
            final long secondsSpent = resolveVisitTime(bestPoi);
            long secondsSpendAtDestination = (long)(secondsSpent * profile.getPreferences().getSpendTimeModifier());
            secondsSpendAtDestination = checkIfSpentTimeIsProbable(secondsSpendAtDestination, currentPoint, profile, estimatedTimePointer);
            estimatedTimePointer.set(estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination));

            final RoutePart routePart = new RoutePart(currentPoint, bestPoi, transportMode, secondsSpendAtDestination);
            routeParts.add(routePart);
            currentPoint = bestPoi;
        }

        if (config.getBoolean(Parameters.SHOULD_GO_BACK_AT_THE_END)) { //sprawdzanie czy na końcu trajektorii powinien nastąpić powrót
            final RoutePart routePart = new RoutePart(currentPoint, placeOfDeparture, resolveReturningTransportMode(profile), 0);
            routeParts.add(routePart);
        }
        historyOfPoi.addReverseToOrderOfPoi();
        historyOfTransportModes.clear();
        return new RoutePlan(routeParts, startTime);
    }

    private long checkIfSpentTimeIsProbable(long secondsSpendAtDestination, PoiHolder<? extends IPoi> currentPoi, IProfile profile,
                                            AtomicReference<LocalDateTime> estimatedTimePointer) {
        if (profile.getFullName().contains("Teenager")){
            if (currentPoi.getPoi().getCategory().equals("school")){
                if (estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination)
                        .isAfter(LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(3).plusMinutes(30)))){
                    Duration dur = Duration.between(estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination),
                            LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(3).plusMinutes(Math.round(new Random().nextDouble() * 30))));
                    return secondsSpendAtDestination - dur.getSeconds();
                }
            }
        } else if (profile.getFullName().contains("Student")){
            if (currentPoi.getPoi().getCategory().equals("university")){
                if (estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination)
                        .isAfter(LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(4).plusMinutes(30)))){
                    Duration dur = Duration.between(estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination),
                            LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(4).plusMinutes(Math.round(new Random().nextDouble() * 30))));
                    return secondsSpendAtDestination - dur.getSeconds();
                }
            }
        } else if (profile.getFullName().contains("Adult")){
            if (currentPoi.getPoi().getCategory().equals("company")){
                if (estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination)
                        .isAfter(LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(5).plusMinutes(45)))){
                    Duration dur = Duration.between(estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination),
                            LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(5).plusMinutes(Math.round(new Random().nextDouble() * 45))));
                    return secondsSpendAtDestination - dur.getSeconds();
                }
            }
        }
        if (!profile.getFullName().contains("Night")) {
            LocalDateTime maximumTimeOfTrajectories = LocalDateTime.of(estimatedTimePointer.get().toLocalDate(), LocalTime.NOON.plusHours(6).plusMinutes(30));
            if (estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination).isAfter(maximumTimeOfTrajectories)) {
                Duration dur = Duration.between(estimatedTimePointer.get().plusSeconds(secondsSpendAtDestination), maximumTimeOfTrajectories);
                if (dur.compareTo(Duration.ofSeconds(600)) < 0) {
                    if (dur.compareTo(Duration.ofSeconds(300)) < 0)
                        dur = dur.plusSeconds(500);
                    else
                        dur = dur.plusSeconds(300);
                }
                return secondsSpendAtDestination - dur.getSeconds();
            } else
                return secondsSpendAtDestination;
        }
        return secondsSpendAtDestination;
    }

    //wyznaczanie momentu rozpoczęcia podróży - godzina jest losowana dla wybranej w konfiguracji pory dnia
    private LocalDateTime  resolveStartTime(IProfile profile) {
        final LocalDate startDate = config.resolveSimulationStartTime().toLocalDate();
        final ActivityTime activityTime = profile.getPreferences().getActivityTime();
        int rnd = new Random().nextInt(90);
        //sonarqube sugestia
        int weekendDelay = 0;
        if (startDate.getDayOfWeek().equals(DayOfWeek.SATURDAY) || startDate.getDayOfWeek().equals(DayOfWeek.SUNDAY))
            weekendDelay = 2;
        if (ActivityTime.MORNING.equals(activityTime)) {
            return startDate.atStartOfDay().plusHours(6 + weekendDelay).plusMinutes(rnd);
        } else if (ActivityTime.MIDDAY.equals(activityTime)) {
            return startDate.atStartOfDay().plusHours(12 + weekendDelay).plusMinutes(rnd);
        } else if (ActivityTime.EVENING.equals(activityTime)){
            return startDate.atStartOfDay().plusHours(17 + weekendDelay).plusMinutes(rnd);
        } else {
            int random = new Random().nextInt(4);
            switch (random){
                case 1 : return startDate.atStartOfDay().plusHours(6 + weekendDelay).plusMinutes(rnd);
                case 2 : return startDate.atStartOfDay().plusHours(12 + weekendDelay).plusMinutes(rnd);
                case 3 : return startDate.atStartOfDay().plusHours(17 + weekendDelay).plusMinutes(rnd);
                default: return startDate.atStartOfDay().plusHours(10 + weekendDelay).plusMinutes(rnd);
            }
        }
    }

    //wyznaczanie czasy trwania wizyty z uwzględnieniem losowości
    protected long resolveVisitTime(PoiHolder<?> nearestToCurrent) {
        final long averageSecondsSpent = nearestToCurrent.getPoi().getAverageSecondsSpent();
        return RandomUtils.deviatedByPercentage(averageSecondsSpent,
            config.getDouble(Parameters.GENERATED_SPENT_SECONDS_BASE_PERCENTAGE_DEVIATION));
    }

    //wyznaczanie środka transportu
    private String resolveTransportMode(IProfile profile) {
        final Collection<Pair<String, Double>> prefferedTransportModes = profile.getPrefferedTransportModes();
        final String randomValue = RandomUtils.randomValue(profile.getFullName(),prefferedTransportModes, historyOfTransportModes, false);

        return randomValue;
    }

    //wyznaczanie poi
    private PoiHolder<? extends IPoi> getBestNextPoi(PoiHolder<? extends IPoi> ref, Collection<PoiHolder<? extends IPoi>> allPois,
        AtomicReference<LocalDateTime> estimatedTimePointer,
        String transportMode, TrajectoryHistoryOfPoi<T> historyOfPoi) {
        final List<PoiHolder<? extends IPoi>> nearestPois = allPois.stream()
            .sorted((poiA, poiB) -> compareByDistanceToRef(ref, poiA, poiB))
            .collect(Collectors.toList());

        final Optional<PoiHolder<? extends IPoi>> nearestOpenedPoi = nearestPois.stream()
            .filter(poi -> filterOutClosedPoi(ref, poi, estimatedTimePointer, transportMode)).findFirst();
        final Optional<PoiHolder<? extends IPoi>> nearestPoi = nearestPois.stream().findFirst();

        if (ProbabilityUtils.probabilityOfStickToDailyRoutine(historyOfPoi.getOrderOfPois())){
            final PoiHolder<? extends IPoi> poi = getPoisInRoutineOrder(allPois, historyOfPoi);
            allPois.remove(poi);
            final long seconds = estimateTravelTimeSeconds(ref, poi, transportMode);
            estimatedTimePointer.set(estimatedTimePointer.get().plusSeconds(seconds));
            return poi;
        }
        else if (nearestOpenedPoi.isPresent()) {
            final PoiHolder<? extends IPoi> poi = nearestOpenedPoi.get();
            allPois.remove(poi);
            final long seconds = estimateTravelTimeSeconds(ref, poi, transportMode);
            estimatedTimePointer.set(estimatedTimePointer.get().plusSeconds(seconds));
            return poi;
        } else if (nearestPoi.isPresent()) {
            final PoiHolder<? extends IPoi> poi = nearestPoi.get();
            allPois.remove(poi);
            final long seconds = estimateTravelTimeSeconds(ref, poi, transportMode);
            estimatedTimePointer.set(estimatedTimePointer.get().plusSeconds(seconds));
            poi.getProfile().getLogger()
                .info("Unable to find POI that is opened at estimated " + estimatedTimePointer + ". Nearest POI will be used.");
            return poi;
        } else {
            throw new IllegalStateException("Unable to find nearest point of ref in an empty collection!");
        }
    }

    private PoiHolder<? extends IPoi> getPoisInRoutineOrder(Collection<PoiHolder<? extends IPoi>> allPois, TrajectoryHistoryOfPoi<T> historyOfPoi) {
        PoiHolder<? extends IPoi> poi = allPois.iterator().next();
        Set<String> categories = allPois.stream().map(PoiHolder::getPoi).map(IPoi::getCategory).collect(Collectors.toSet());
        Map<String, Pair<Set<String>, Set<String>>> orderOfPois = historyOfPoi.getOrderOfPois();
        for (PoiHolder<? extends IPoi> p : allPois){
            if (orderOfPois.containsKey(p.getPoi().getCategory())){
                if (!orderOfPois.get(p.getPoi().getCategory()).getFirst().isEmpty()){
                    for (String s : orderOfPois.get(p.getPoi().getCategory()).getFirst()){
                        if (categories.contains(s)){
                            poi = getPoiByCategory(allPois, s);
                            categories.remove(p.getPoi().getCategory());
                            break;
                        }
                    }
                }
            }
        }
        return poi;
    }

    private PoiHolder<? extends IPoi> getPoiByCategory(Collection<PoiHolder<? extends IPoi>> allPois, String category){
        for (PoiHolder<? extends IPoi> p : allPois){
            if (p.getPoi().getCategory().equals(category))
                return p;
        }
        return allPois.iterator().next();
    }
    //porównywanie długości odcinków z danymi współrzędnymi ich końców
    private int compareByDistanceToRef(PoiHolder<?> ref, PoiHolder<?> poiA, PoiHolder<?> poiB) {
        final double fromAToRef = GeoUtils.distance(poiA.getPoi().getLat(), poiA.getPoi().getLon(),
            ref.getPoi().getLat(), ref.getPoi().getLon());
        final double fromBToRef = GeoUtils.distance(poiB.getPoi().getLat(), poiB.getPoi().getLon(),
            ref.getPoi().getLat(), ref.getPoi().getLon());

        return fromAToRef > fromBToRef ? 1 : -1;
    }

    //sprawdzanie czy poi jest czynne
    protected boolean filterOutClosedPoi(PoiHolder<?> ref, PoiHolder<?> poiHolder, AtomicReference<LocalDateTime> estimatedTimePointer,
        String transportMode) {
        final long seconds = estimateTravelTimeSeconds(ref, poiHolder, transportMode);
        final LocalDateTime estimatedTimeAtPoi = estimatedTimePointer.get().plusSeconds(seconds);
        final boolean isOpen = openingHoursSupplier.isOpen(poiHolder.getProfile(), poiHolder.getPoi(), estimatedTimeAtPoi);

        return isOpen;
    }

    //wyznaczanie czasu jaki zajmuje przebycie odcinka trasy
    protected long estimateTravelTimeSeconds(PoiHolder<?> from, PoiHolder<?> to, String transportMode) {
        final IPoi aPoi = from.getPoi();
        final IPoi bPoi = to.getPoi();
        final double mpsSpeed = averageMeterPerSecondSpeedForTransportMode(transportMode);
        final double distance = GeoUtils.distance(aPoi.getLat(), aPoi.getLon(), bPoi.getLat(), bPoi.getLon());

        return (long)(distance / mpsSpeed);
    }

    //funkcja pomocnicza zawsze zwraca 1
    protected double averageMeterPerSecondSpeedForTransportMode(String transportMode) {
        return 1;
    }

    private PoiHolder<? extends IPoi> resolvePlaceOfDeparture(IProfile profile) {
        final Point placeOfDeparture = profile.getPlaceOfDeparture();
        return new PoiHolder<Poi>(profile, createDeparturePoi(placeOfDeparture), Collections.emptyMap());
    }

    //funkcja zwraca punkt początkowy wędrówki
    protected Poi createDeparturePoi(Point placeOfDeparture) {
        return new Poi(IRoutePart.PLACE_OF_DEPARTURE, IRoutePart.PLACE_OF_DEPARTURE, "Place of departure for this profile.",
            placeOfDeparture.getLat(),
            placeOfDeparture.getLon(),
            0);
    }

    //wybór środka transportu jeśli na końcu trajektorii według konfiguracji ma nastąpić powrót
    protected String resolveReturningTransportMode(IProfile profile) {
        final Collection<Pair<String, Double>> prefferedTransportModes = profile.getPrefferedTransportModes();
        final Collection<Pair<String, Double>> prefferedTransportModesMultipliedByTransportSpeed = prefferedTransportModes.stream()
            .map(mode -> Pair.create(mode.getFirst(), mode.getSecond() * averageMeterPerSecondSpeedForTransportMode(mode.getFirst())))
            .collect(Collectors.toList());
        final String randomValue = RandomUtils.randomValue(profile.getFullName(),prefferedTransportModesMultipliedByTransportSpeed, historyOfTransportModes, true);
        return randomValue;
    }
}
