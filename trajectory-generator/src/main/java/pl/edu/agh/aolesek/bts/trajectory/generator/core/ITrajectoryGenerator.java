package pl.edu.agh.aolesek.bts.trajectory.generator.core;

import java.util.Collection;

public interface ITrajectoryGenerator {

    //główna funkcja aplikacji
    Collection<ProfileResult> generateTrajectories();
}
