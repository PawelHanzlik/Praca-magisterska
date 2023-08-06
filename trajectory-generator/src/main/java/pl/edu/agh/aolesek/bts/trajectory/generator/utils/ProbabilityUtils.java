package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

import java.util.Set;

public class ProbabilityUtils {


    public static double calculateReturn(Set<String> setOfPois){
        return 1 - calculateExplore(setOfPois);
    }

    public static  double calculateExplore(Set<String> setOfPois){
        return Math.pow(0.6 * setOfPois.size(),-0.21) ;
    }

    public static boolean probabilityOfExploreCalculate(Set<String> setOfPois){
        double random = Math.random() * 100 + 1;
        return random < (calculateExplore(setOfPois) * 100);
    }
}
