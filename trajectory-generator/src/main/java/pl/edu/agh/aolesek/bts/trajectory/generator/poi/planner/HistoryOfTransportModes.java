package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import java.util.List;
public class HistoryOfTransportModes {

    List<String> history;

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }


    public HistoryOfTransportModes(List<String> history) {
        this.history = history;
    }
}
