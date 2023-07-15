package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.TrajectoryGeneratorException;

//zwracanie błędów występujących podczas wytyczania tras
public class RoutingException extends TrajectoryGeneratorException {

    private static final long serialVersionUID = 1L;

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
