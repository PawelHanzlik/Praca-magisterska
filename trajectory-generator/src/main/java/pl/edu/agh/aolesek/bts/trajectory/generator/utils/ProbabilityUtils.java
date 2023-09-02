package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;

import java.util.Map;
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

    public static double calculateStickToDailyRoutine(int length){
        return 1 - Math.pow(0.7 * length,-0.19) ;
    }

    public static int calculateSizeOfMap(Map<String, Pair<Set<String>, Set<String>>> orderOfPois){
        int length = 0;
        for (String s : orderOfPois.keySet()){
            length += orderOfPois.get(s).getFirst().toArray().length;
            length += orderOfPois.get(s).getSecond().toArray().length;
        }
        return length / 2;
    }

    public static boolean probabilityOfStickToDailyRoutine(Map<String, Pair<Set<String>, Set<String>>> orderOfPois){
        double random = Math.random() * 100 + 1;
        int length = calculateSizeOfMap(orderOfPois);
        if (length > 10) {
            return random < ((calculateStickToDailyRoutine(length) * 100) + 0.2);
        }
        else if (length < 3){
            return false;
        }
        else {
            return random < (calculateStickToDailyRoutine(length) * 100);
        }
    }
}
