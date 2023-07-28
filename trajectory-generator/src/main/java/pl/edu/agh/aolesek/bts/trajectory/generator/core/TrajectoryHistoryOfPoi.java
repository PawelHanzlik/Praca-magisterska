package pl.edu.agh.aolesek.bts.trajectory.generator.core;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.util.Collection;
import java.util.Set;

public class TrajectoryHistoryOfPoi <T extends IPoi> {

    Set<PoiHolder<T>> poiHistory;
    Set<String> poiNames;

    public TrajectoryHistoryOfPoi(Set<PoiHolder<T>> poiHistory, Set<String> poiNames) {
        this.poiHistory = poiHistory;
        this.poiNames = poiNames;
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

    public String toString(){
        StringBuilder history = new StringBuilder();
        for (String name : poiNames){
            history.append(name).append(" ");
        }
        return history.toString();
    }

    private Boolean poiAlreadyExists(PoiHolder<T> poiToAdd){
        if (poiToAdd != null && poiToAdd.getPoi() != null && poiToAdd.getPoi().getName() != null) {
            if (poiNames.contains(poiToAdd.getPoi().getName()))
                return true;
            else {
                poiNames.add(poiToAdd.getPoi().getName());
                return false;
            }
        }
        return true;
    }
}
