package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router;

import com.google.common.collect.ImmutableMap;
import mil.nga.sf.geojson.LineString;
import mil.nga.sf.geojson.Position;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.Trajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

//zawiera algorytm wytyczający dokładny przebieg trasy i funkcje pomocnicze konwertujące typy danych oraz sprawdzające występowanie błędów
public abstract class AbstractRouter implements IPoiRouter {

    protected final Config config;

    public AbstractRouter(Config config) {
        this.config = config;
    }

    @Override
    public ITrajectory route(IProfile profile, IRoutePlan routePlan) {
        requireNotEmptyPlan(routePlan); //check if plan isn't empty

        //time of way beginning
        final AtomicReference<LocalDateTime> timePointer = new AtomicReference<LocalDateTime>(routePlan.getStartTime());
        //create new trajectory
        final Trajectory trajectory = new Trajectory(new LinkedList<>(), new LinkedList<>(), profile);

        addPlaceOfDepartureToTrajectory(routePlan, timePointer, trajectory); //add departure place
        routePlan.getParts().forEach(part -> routePart(profile, part, timePointer, trajectory));
        return trajectory;
    }

    private void requireNotEmptyPlan(IRoutePlan routePlan) { //check if plan isn't empty
        if (routePlan.getParts().isEmpty()) {
            throw new IllegalStateException("Route plan cannot be empty to perform generating trajectory!");
        }
    }

    private void addPlaceOfDepartureToTrajectory(IRoutePlan routePlan, AtomicReference<LocalDateTime> timePointer, Trajectory trajectory) {
        trajectory.getPoisWithTimestamps()
            .add(Pair.create(timePointer.get(), routePlan.getParts().iterator().next().getSource().getPoi()));
    }

    private void routePart(IProfile profile, IRoutePart routePart, AtomicReference<LocalDateTime> timePointer, ITrajectory trajectory) {
        final RoutingForPart routingForPart = resolveRoutingForPart(profile, routePart);

        final Pair<LinkedList<Position>, Pair<Double, Double>> routingAddDistanceAddTime = supplementRoutingLine(routePart,
            routingForPart);
        final Iterator<Position> pointsIterator = routingAddDistanceAddTime.getFirst().iterator();

        final Pair<Double, Double> additionalDistanceAndTime = routingAddDistanceAddTime.getSecond();
        // Adds time spent and distance traveled by traveling initial and final fragment of route to total time.
        final double totalDistance = routingForPart.getDistance() + additionalDistanceAndTime.getFirst();
        final double totalDuration = modifyDuration(profile, routePart.getTransportMode(), routingForPart.getDuration())
            + additionalDistanceAndTime.getSecond();

        final List<Pair<LocalDateTime, Point>> trajectoryPoints = trajectory.getPointsWithTimestamps();
        final Position firstPoint = pointsIterator.next();
        trajectoryPoints.add(Pair.create(timePointer.get(), new Point(firstPoint.getY(), firstPoint.getX())));

        Position previousPoint = null;
        Position currentPoint = firstPoint;

        while (pointsIterator.hasNext()) {
            previousPoint = currentPoint;
            currentPoint = pointsIterator.next();

            final Double lon = currentPoint.getX();
            final Double lat = currentPoint.getY();

            final double distanceFromPreviousPoint = GeoUtils.distance(previousPoint.getY(), previousPoint.getX(), lat, lon);
            final double partOfTotalDistance = distanceFromPreviousPoint / totalDistance;
            final long nanosSinceLastPoint = (long)(1000000000 * (partOfTotalDistance < 1 ? partOfTotalDistance : 1) * totalDuration);
            final LocalDateTime timeAtPoint = timePointer.get().plusNanos(nanosSinceLastPoint);
            timePointer.set(timeAtPoint);

            trajectoryPoints.add(Pair.create(timePointer.get(), new Point(lat, lon)));
        }

        trajectory.getPoisWithTimestamps().add(Pair.create(timePointer.get(), routePart.getDestination().getPoi()));

        // increment time by amount of time spent when visiting POI
        timePointer.set(timePointer.get().plusSeconds(routePart.getSecondsSpendAtDestination()));
    }

    /*
     * Adds initial fragment and final fragment to the routing line. Connects end of received routing to POIs as the routings from extrnal
     * providers usually end at address, not at POI. Connects place of departure to beginning of first routing (the rest source pois are
     * connected because those are also destinations for previous step). If transport mode is other than walking and distance from POI to
     * endpoint of routing is too high, this method will try to query provider for routing between this two points for walking mode.
     */
    private Pair<LinkedList<Position>, Pair<Double, Double>> supplementRoutingLine(IRoutePart routePart, RoutingForPart routingForPart) {
        final LinkedList<Position> routingLinePoints = new LinkedList<>(routingForPart.getLine().getCoordinates());
        double additionalDistance = 0;
        double additionalTime = 0;

        final IPoi sourcePoi = routePart.getSource().getPoi();
        final Pair<Double, Double> initialPart = fillInitialPart(sourcePoi, routingLinePoints, routePart.getTransportMode());
        additionalDistance += initialPart.getFirst();
        additionalTime += initialPart.getSecond();

        final IPoi destinationPoi = routePart.getDestination().getPoi();
        final Pair<Double, Double> finalPart = fillFinalPart(destinationPoi, routingLinePoints, routePart.getTransportMode());
        additionalDistance += finalPart.getFirst();
        additionalTime += finalPart.getSecond();

        return Pair.create(routingLinePoints, Pair.create(additionalDistance, additionalTime));
    }

    private Pair<Double, Double> fillInitialPart(IPoi sourcePoi, LinkedList<Position> routingLinePoints, String transportMode) {
        final Position first = routingLinePoints.getFirst();
        final double distanceFromSource = GeoUtils.distance(sourcePoi.getLat(), sourcePoi.getLon(), first.getY(), first.getX());
        final double timeRequiredToWalkFromSource = distanceFromSource / config.getDouble(Parameters.AVERAGE_WALKING_SPEED);

        if (distanceFromSource <= 1) {
            return Pair.create(0d, 0d);
        }

        if (Objects.equals(defaultMode(), transportMode) || distanceFromSource <= config.getLong(Parameters.MAX_DISTANCE_TO_SHORTCUT)) {
            // Already default(walking) transport mode or too near to route, so we can only shortcut between existing routing and poi
            final Position poiPosition = new Position(sourcePoi.getLon(), sourcePoi.getLat());
            routingLinePoints.push(poiPosition);
            return Pair.create(distanceFromSource, timeRequiredToWalkFromSource);
        } else {
            final Position poiPosition = new Position(sourcePoi.getLon(), sourcePoi.getLat());
            routingLinePoints.push(poiPosition);
            return Pair.create(distanceFromSource, timeRequiredToWalkFromSource);
        }
    }

    private Pair<Double, Double> fillFinalPart(IPoi destinationPoi, LinkedList<Position> routingLinePoints, String transportMode) {
        final Position last = routingLinePoints.getLast();
        final double distanceToDestination = GeoUtils.distance(destinationPoi.getLat(), destinationPoi.getLon(), last.getY(), last.getX());
        final double timeRequiredToWalkToDestination = distanceToDestination / config.getDouble(Parameters.AVERAGE_WALKING_SPEED);

        if (distanceToDestination <= 1) {
            return Pair.create(0d, 0d);
        }

        if (Objects.equals(defaultMode(), transportMode) || distanceToDestination <= config.getLong(Parameters.MAX_DISTANCE_TO_SHORTCUT)) {
            final Position poiPosition = new Position(destinationPoi.getLon(), destinationPoi.getLat());
            routingLinePoints.add(poiPosition);
            return Pair.create(distanceToDestination, timeRequiredToWalkToDestination);
        } else {
            final Position poiPosition = new Position(destinationPoi.getLon(), destinationPoi.getLat());
            routingLinePoints.add(poiPosition);
            return Pair.create(distanceToDestination, timeRequiredToWalkToDestination);
        }
    }

    protected abstract String checkTransportMode(String transportMode); //nowa funkcja sprawdzająca środek transportu

    private RoutingForPart resolveRoutingForPart(IProfile profile, IRoutePart routePart) {

        String transportModeExtracted = routePart.getTransportMode(); //defined transport mode
        String transportModeChecked = checkTransportMode(transportModeExtracted); //checked mode with crossing services

        if(!transportModeChecked.equals("correct"))
        {
            transportModeExtracted = transportModeChecked; //assign if condition is fulfilled
        }

        final RoutingForPart routingForPart = resolveRoutingFromExternalProvider(profile, routePart, transportModeExtracted); //API query
        profile.getLogger().debug("Received routing for part " + routePart + " (mode:" + routePart.getTransportMode() + ")",
            ImmutableMap.of("routing", routingForPart));

        if (routingForPart.getLine().getCoordinates().size() < 2) {
            profile.getLogger()
                .warn("Routing resolved from external service is invalid! At least two points should be returned. Routing between "
                    + routePart.getSource() + " and " + routePart.getDestination()
                    + " was requested. Will try to shortcut between source and target to prevent fail.");
            final Position from = new Position(routePart.getSource().getPoi().getLon(), routePart.getSource().getPoi().getLat());
            final Position to = new Position(routePart.getDestination().getPoi().getLon(), routePart.getDestination().getPoi().getLat());
            final LineString shortcutLine = new LineString(Arrays.asList(from, to));
            return new RoutingForPart(routingForPart.getDuration(), routingForPart.getDistance(), shortcutLine);
        }
        return routingForPart;
    }

    //query for API
    private RoutingForPart resolveRoutingFromExternalProvider(IProfile profile, IRoutePart routePart, String transportModeChecked) {
        try {
            try {
                return queryExternalProvider(routePart, transportModeChecked);
            } catch (final RuntimeException e) {
                // Some points may not be reachable with transport modes other than walking (or other default mode), so after error we will
                // try again for default mode
                profile.getLogger().warn("Unable to resolve routing for provided transport mode, using default instead");
                return queryExternalProviderWithDefaultMode(routePart);
            }
        } catch (IOException | RuntimeException e) {
            throw new RoutingException("Exception when contacting routing provider", e);
        }
    }

    //not used because transport mode name has been extracted above
    /*protected RoutingForPart queryExternalProvider(IRoutePart part) throws IOException {
        return queryExternalProvider(part, part.getTransportMode());
    }*/

    protected RoutingForPart queryExternalProviderWithDefaultMode(IRoutePart part) throws IOException {
        return queryExternalProvider(part, defaultMode());
    }

    protected RoutingForPart queryExternalProvider(IRoutePart part, String transportMode) throws IOException {
        final IPoi sourcePoi = part.getSource().getPoi();
        final Point sourcePoint = new Point(sourcePoi.getLat(), sourcePoi.getLon());

        final IPoi destinationPoi = part.getDestination().getPoi();
        final Point destinationPoint = new Point(destinationPoi.getLat(), destinationPoi.getLon());

        return queryExternalProvider(sourcePoint, destinationPoint, transportMode);
    }

    protected abstract RoutingForPart queryExternalProvider(Point from, Point to, String transportMode) throws IOException;

    protected abstract String defaultMode();

    /**
     * Modify travel duration received from external provider using data stored in profile, e.g. in case of walking, modify total duration
     * to match average walking speed.
     * 
     * @param totalDuration
     *            total duration received from external provider
     * @return duration modified for profile
     */
    protected abstract double modifyDuration(IProfile profile, String transportMode, double totalDuration);

    protected String poiToString(Point point) {
        final double lat = point.getLat();
        final double lon = point.getLon();
        return String.format(Locale.ROOT, "%f,%f", lat, lon);
    }
}
