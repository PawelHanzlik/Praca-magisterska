package pl.edu.agh.aolesek.bts.trajectory.generator.core;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;

import java.util.*;
import java.util.stream.Collectors;

public class TrajectoryHistoryOfPoi <T extends IPoi> {

    Set<PoiHolder<T>> poiHistory;
    Set<String> poiNames;

    Set<Pair<String, String>> poiNamesAndCategories;

    List<Pair<PoiHolder<T>, Integer>> numberOfOccurrences;

    Map<String, Pair<Set<String>, Set<String>>> orderOfPois;

    public TrajectoryHistoryOfPoi(Set<PoiHolder<T>> poiHistory, Set<String> poiNames, Set<Pair<String, String>> poiNamesAndCategories,
                                  List<Pair<PoiHolder<T>, Integer>> numberOfOccurrences, Map<String, Pair<Set<String>, Set<String>>> orderOfPois) {
        this.poiHistory = poiHistory;
        this.poiNames = poiNames;
        this.poiNamesAndCategories = poiNamesAndCategories;
        this.numberOfOccurrences = numberOfOccurrences;
        this.orderOfPois = orderOfPois;
    }

    private void addPoiToHistory(PoiHolder<T> poiToAdd){
        if (poiToAdd != null && !poiAlreadyExists(poiToAdd)){
            poiHistory.add(poiToAdd);
        }
    }

    public void addPoiToHistory(Collection<PoiHolder<T>> poisForProfile){
        for (PoiHolder<T> p : poisForProfile){
            addPoiToHistory(p);
        }
    }

    public void addOccurrences(Collection<PoiHolder<T>> poisForProfile){
        for (PoiHolder<T> p : poisForProfile){
            int index = exists(p);
            if (index != -1){
                numberOfOccurrences.set(index, Pair.create(numberOfOccurrences.get(index).getFirst(), numberOfOccurrences.get(index).getSecond() + 1));
            } else {
                numberOfOccurrences.add(Pair.create(p,1));
            }
        }
    }

    public String toString(){
        StringBuilder history = new StringBuilder();
        for (String name : poiNames){
            history.append(name).append(" ");
        }
        return history.toString();
    }

    private Boolean poiAlreadyExists(PoiHolder<T> poiToAdd){
        if (poiToAdd != null && poiToAdd.getPoi() != null && poiToAdd.getPoi().getName() != null) {
            if (poiNames.contains(poiToAdd.getPoi().getName())) {
                if (poiNamesAndCategories.contains(Pair.create(poiToAdd.getPoi().getName(), poiToAdd.getPoi().getCategory()))){
                    return true;
                }else {
                    poiNamesAndCategories.add(Pair.create(poiToAdd.getPoi().getName(), poiToAdd.getPoi().getCategory()));
                    return false;
                }
            }
            else {
                poiNames.add(poiToAdd.getPoi().getName());
                poiNamesAndCategories.add(Pair.create(poiToAdd.getPoi().getName(), poiToAdd.getPoi().getCategory()));
                return false;
            }
        }
        return true;
    }

    public Collection<PoiHolder<T>> getPoisByCategory(String category){
        Collection<PoiHolder<T>> selectedPois = new ArrayList<>();
        for (PoiHolder<T> poi : poiHistory){
            if (poi.getPoi().getCategory().equals(category)){
                selectedPois.add(poi);
            }
        }
        return selectedPois;
    }

    public PoiHolder<T> returnBestPoiToReturn(PoiHolder<T> p, Collection<PoiHolder<T>> selectedPois) {
        PoiHolder<T> bestPoi = null;
        double maxValue = Double.MIN_VALUE;
        for (PoiHolder<T> poi : selectedPois){
            if(maxValue < getOccurrences(poi) /
                    GeoUtils.distance(poi.getPoi().getLat(), poi.getPoi().getLon(), p.getPoi().getLat(), p.getPoi().getLon())){
                maxValue = getOccurrences(poi) /
                        GeoUtils.distance(poi.getPoi().getLat(), poi.getPoi().getLon(), p.getPoi().getLat(), p.getPoi().getLon());
                bestPoi = poi;
            } else {
                bestPoi = p;
            }
        }
        return bestPoi;
    }

    private Integer getOccurrences(PoiHolder<T> poi){
        if (poi != null) {
            for (Pair<PoiHolder<T>, Integer> numberOfOccurrence : numberOfOccurrences) {
                if (numberOfOccurrence.getFirst().equals(poi))
                    return numberOfOccurrence.getSecond();
            }
        }
        return 0;
    }

    private Integer exists(PoiHolder<T> poi){
        if (poi != null) {
            for (int i = 0; i < numberOfOccurrences.size(); i++) {
                if (numberOfOccurrences.get(i).getFirst().equals(poi))
                    return i;
            }
        }
        return -1;
    }

    public String occurencesToString(){
        StringBuilder sb = new StringBuilder();
        for (Pair<PoiHolder<T>, Integer> numberOfOccurrence : numberOfOccurrences){
            sb.append(numberOfOccurrence.getFirst().getPoi().getName()).append(" ").append(numberOfOccurrence.getSecond()).append(" ");
        }
        return sb.toString();
    }

    public void addToOrderOfPoi(String category, HashSet<PoiHolder<? extends IPoi>> poisLeft){
        if (this.orderOfPois.containsKey(category)){
            for (PoiHolder<? extends IPoi> poi : poisLeft) {
                this.orderOfPois.get(category).getSecond().add(poi.getPoi().getCategory());
            }
        }
        else{
            Set<String> emptySet = new HashSet<>();
            this.orderOfPois.put(category, Pair.create(emptySet, poisLeft.stream().map(PoiHolder::getPoi).map(IPoi::getCategory).collect(Collectors.toSet())));
        }
    }

    public void printOrderOfPoi(){
        for (Map.Entry<String, Pair<Set<String>, Set<String>>> entry : this.orderOfPois.entrySet()){
            System.out.println(" Order of poi " + entry.getKey() + " przed " + Arrays.toString(entry.getValue().getFirst().toArray()) + " po" +
                    Arrays.toString(entry.getValue().getSecond().toArray()));
        }
    }

    public void addReverseToOrderOfPoi(){
        for (Map.Entry<String, Pair<Set<String>, Set<String>>> entry : this.orderOfPois.entrySet()){
            for (String category : entry.getValue().getSecond()){
                if (this.orderOfPois.containsKey(category)){
                    this.orderOfPois.get(category).getFirst().add(entry.getKey());
                } else {
                    Set<String> emptySet = new HashSet<>();
                    this.orderOfPois.put(category, Pair.create(new HashSet<>(){
                        {
                            add(entry.getKey());
                        }
                    }, emptySet));
                }
            }
        }
    }

    public Map<String, Pair<Set<String>, Set<String>>> getOrderOfPois() {
        return orderOfPois;
    }
}