package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

import java.util.Collection;

//generowanie wyjścia do konsoli
public class PrintResultHandler implements IResultHandler {

    @Override
    public void handleResult(Collection<ProfileResult> result) {
        System.out.println(result);
    }
}
