package pl.edu.agh.aolesek.bts.trajectory.generator.app.modules;

import com.google.inject.AbstractModule;
import lombok.AllArgsConstructor;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.ErrorHandler;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;

//struktura zawierająca poniższe informacje
@AllArgsConstructor
public abstract class AbstractTrajectoryGeneratorModule extends AbstractModule {

    //konfiguracja generatora
    protected final Config generatorParameters;

    //informacje o błędach
    protected final ErrorHandler errorHandler;
}
