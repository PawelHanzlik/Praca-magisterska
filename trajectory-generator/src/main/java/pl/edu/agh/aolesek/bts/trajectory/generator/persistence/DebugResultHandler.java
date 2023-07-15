package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import java.util.Collection;

import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

//generowanie wyjścia do loggera
@Log4j2
public class DebugResultHandler implements IResultHandler {

    @Override
    public void handleResult(Collection<ProfileResult> result) {
        result.forEach(res -> log.debug(res));
    }
}
