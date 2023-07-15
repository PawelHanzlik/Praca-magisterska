package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import java.util.Collection;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

//interfejs z metodą obsługującą wyjścia z aplikacji
public interface IResultHandler {

    void handleResult(Collection<ProfileResult> result);
}
