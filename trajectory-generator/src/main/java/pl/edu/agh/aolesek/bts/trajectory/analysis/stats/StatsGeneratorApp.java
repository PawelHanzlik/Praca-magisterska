package pl.edu.agh.aolesek.bts.trajectory.analysis.stats;

import java.util.Arrays;

public class StatsGeneratorApp {

    public StatsGeneratorApp(String name){
        String[] array = new String[] {name};
        main(array);
    }

    public static final String DEFAULT_PATH = "D:/2023-03/entertainment_one_file/2023-02-16_09-22-29_trajectories.json";

    public static void main(String[] args) {
        final String path = args.length > 0 ? args[0] : DEFAULT_PATH;
        final StatsGenerator statsGenerator = new StatsGenerator(Arrays.asList(path));
        System.out.println("Generating stats for " + path);
        statsGenerator.generateStats();
    }
}
