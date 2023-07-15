package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.AbstractPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;

//generowanie pliku story
public class StoryResultHandler extends AbstractFileResultHandler {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    public StoryResultHandler(Config config) {
        super(config);
    }

    @Override
    protected String getFormat() {
        return "story";
    }

    @Override
    protected void writeHeader(PrintWriter writer) {
        // do nothing - no header for story file
    }

    @Override
    protected void writeResultToFile(ProfileResult result, PrintWriter printWriter) {
        final IProfile profile = result.getProfile();
        if (result.getTrajectory() == null) {
            profile.getLogger().warn("Unable to write story for " + result.getProfile().getFullName() + " - trajectory is empty!");
            return;
        }

        final String name = profile.getFullName();
        final List<Pair<LocalDateTime, IPoi>> visitedPois = result.getTrajectory().getPoisWithTimestamps();

        printInvocation(printWriter, profile, name);
        printInterests(printWriter, profile, name);
        printNumberOfPois(result, printWriter, name);
        printRoute(result, printWriter, profile, name, visitedPois);
        printSummary(result, printWriter, name, visitedPois);
    }

    private void printInvocation(PrintWriter printWriter, final IProfile profile, final String name) {
        printWriter.println(String.format(Locale.ROOT,
            "%s average number of visited places is %d, preffered activity time is %s, preffered modes are %s. Material status is %s.",
            name,
            profile.getPreferences().getAverageNumberOfPOIs(),
            profile.getPreferences().getActivityTime().name(),
            transport(profile),
            profile.getMaterialStatus().name()));
    }

    private String transport(IProfile profile) {
        return profile.getPrefferedTransportModes().stream()
            .map(mode -> String.format(Locale.ROOT, "%s(%.2f)", mode.getFirst(), mode.getSecond()))
            .collect(Collectors.joining(", "));
    }

    private void printInterests(PrintWriter printWriter, final IProfile profile, final String name) {
        printWriter.println(String.format(Locale.ROOT, "%s interests are %s.", name, interests(profile)));
    }

    private String interests(IProfile profile) {
        return profile.getInterests().stream()
            .map(interest -> String.format(Locale.ROOT, "%s(%.2f)", interest.getFirst(), interest.getSecond()))
            .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    private void printNumberOfPois(ProfileResult result, PrintWriter printWriter, final String name) {
        List<PoiHolder<? extends IPoi>> allAvailablePois = (List<PoiHolder<? extends IPoi>>)result.getProfile().getLogger()
            .getParameter(AbstractPoiGenerator.ALL_POIS);
        String line = String.format(Locale.ROOT, "Found %d POIS for %s. ", allAvailablePois.size(), name);
        String categories = allAvailablePois.stream().map(PoiHolder::getPoi).map(IPoi::getCategory).distinct()
            .collect(Collectors.joining(", "));
        printWriter.println(line + "Categories of found POIs are " + categories);
    }

    private void printSummary(ProfileResult result, PrintWriter printWriter, String name, List<Pair<LocalDateTime, IPoi>> visitedPois) {
        final LinkedList<Pair<LocalDateTime, IPoi>> poisInLinkedList = new LinkedList<>(visitedPois);
        final String returnedHome = Objects.equals(poisInLinkedList.getFirst().getSecond(), poisInLinkedList.getLast().getSecond())
            ? "then returned home."
            : "then did not return home.";
        final String message = String.format(Locale.ROOT, "%s visited %d pois between %s and %s, %s",
            name,
            visitedPois.size() - 1, // minus place of departure
            poisInLinkedList.getFirst().getFirst().format(formatter),
            poisInLinkedList.getLast().getFirst().format(formatter),
            returnedHome);
        printWriter.println(message);

        final Map<String, Throwable> errorLog = result.getProfile().getLogger().getErrorLog();
        if (!errorLog.isEmpty()) {
            printWriter.println("Following errors occured: ");
            errorLog.forEach((errorMessage, error) -> printWriter.println(String.format("%s -> %s", errorMessage, error)));
        }
        printWriter.println();
    }

    private void printRoute(ProfileResult result, PrintWriter printWriter, IProfile profile, String name,
        List<Pair<LocalDateTime, IPoi>> visitedPois) {
        printWriter.println(String.format(Locale.ROOT, "%s departed from %s at %s.", name, point(profile.getPlaceOfDeparture()),
            departureTime(visitedPois)));

        printPois(visitedPois, result, printWriter);
    }

    private String point(Point point) {
        return String.format(Locale.ROOT, "(%.3f, %.3f)", point.getLat(), point.getLon());
    }

    private String departureTime(List<Pair<LocalDateTime, IPoi>> trajectoryPointsWithTimestamps) {
        return trajectoryPointsWithTimestamps.isEmpty() ? "?"
            : trajectoryPointsWithTimestamps.iterator().next().getFirst().format(formatter);
    }

    private void printPois(List<Pair<LocalDateTime, IPoi>> visitedPois, ProfileResult result, PrintWriter writer) {
        if (visitedPois.size() > 1) {
            final Iterator<Pair<LocalDateTime, IPoi>> poisIterator = visitedPois.iterator();
            Pair<LocalDateTime, IPoi> source = null;
            Pair<LocalDateTime, IPoi> destination = poisIterator.next();
            final AtomicReference<LocalDateTime> previousLeaveTime = new AtomicReference<>(destination.getFirst());

            do {
                source = destination;
                destination = poisIterator.next();
                writer.println(lineForRoutePart(source, destination, result, previousLeaveTime));
            } while (poisIterator.hasNext());
        }
    }

    private String lineForRoutePart(Pair<LocalDateTime, IPoi> previousPlace, Pair<LocalDateTime, IPoi> thisPlace, ProfileResult result,
        AtomicReference<LocalDateTime> previousPlaceLeaveTime) {
        Optional<IRoutePart> correspondingPart = resolveCorrespondingRoutePart(previousPlace, thisPlace, result);
        if (!correspondingPart.isPresent()) {
            return "Unable to resolve routePart for this part of destination!";
        }

        final String fullName = result.getProfile().getFullName();
        final String poiName = createPoiName(thisPlace);
        final double travelDuration = resolveTravelDuration(thisPlace, previousPlaceLeaveTime);
        final double kmDistance = resolveDistanceInKm(previousPlace, thisPlace);
        final String format = thisPlace.getFirst().format(formatter);
        final String transportMode = correspondingPart.get().getTransportMode();
        final double minutesAtDestination = (double)correspondingPart.get().getSecondsSpendAtDestination() / 60;

        previousPlaceLeaveTime.set(thisPlace.getFirst().plusSeconds(correspondingPart.get().getSecondsSpendAtDestination()));
        final String line = String.format(
            "%s visited %s at %s. (Traveled %.1f km SL in %5.1f minutes, %12.12s).",
            fullName,
            poiName,
            format,
            kmDistance,
            travelDuration,
            transportMode);
        final String spentTime = String.format(" Then spent %5.1f minutes there and left at %s.", minutesAtDestination,
            previousPlaceLeaveTime.get().format(formatter));
        return IRoutePart.PLACE_OF_DEPARTURE.equals(thisPlace.getSecond().getId()) ? line : line + spentTime;
    }

    private Optional<IRoutePart> resolveCorrespondingRoutePart(Pair<LocalDateTime, IPoi> previousPlace,
        Pair<LocalDateTime, IPoi> thisPlace,
        ProfileResult result) {
        Optional<IRoutePart> correspondingPart = result.getRoutePlan().getParts().stream()
            .filter(part -> Objects.equals(part.getSource().getPoi(), previousPlace.getSecond())
                && Objects.equals(part.getDestination().getPoi(), thisPlace.getSecond()))
            .findFirst();
        return correspondingPart;
    }

    private String createPoiName(Pair<LocalDateTime, IPoi> poi) {
        return String.format(Locale.ROOT, "%50.50s (%27.27s:%.3f,%.3f:%10.10s)",
            poi.getSecond().getName(),
            poi.getSecond().getId(), poi.getSecond().getLat(), poi.getSecond().getLon(),
            poi.getSecond().getCategory());
    }

    private double resolveTravelDuration(Pair<LocalDateTime, IPoi> nextPlace, AtomicReference<LocalDateTime> currentPlaceLeaveTime) {
        return ((double)Duration.between(currentPlaceLeaveTime.get(), nextPlace.getFirst()).getSeconds()) / 60;
    }

    private double resolveDistanceInKm(Pair<LocalDateTime, IPoi> currentPlace, Pair<LocalDateTime, IPoi> nextPlace) {
        return GeoUtils.distance(nextPlace.getSecond().getLat(), nextPlace.getSecond().getLon(),
            currentPlace.getSecond().getLat(), currentPlace.getSecond().getLon()) / 1000.0;
    }
}
