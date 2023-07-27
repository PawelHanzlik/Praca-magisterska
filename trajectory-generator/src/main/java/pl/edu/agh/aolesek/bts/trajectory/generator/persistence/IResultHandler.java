package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

import java.util.Collection;

//interfejs z metodą obsługującą wyjścia z aplikacji
public interface IResultHandler {

    void handleResult(Collection<ProfileResult> result);
}
