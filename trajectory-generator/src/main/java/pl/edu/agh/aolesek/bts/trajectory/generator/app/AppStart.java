package pl.edu.agh.aolesek.bts.trajectory.generator.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.modules.AbstractTrajectoryGeneratorModule;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.modules.TrajectoryGeneratorModulesFactory;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

import java.time.Duration;
import java.util.Set;

@Log4j2
public class AppStart {
    public AppStart()
    {
        main(null);
    }
    public static void main(String[] args) {
        if (CliHelpHandler.handleUserNeedsAssistance(args)) {
            return;
        }
        String[] AppArgs={"config.cfg"};
        Config config = new Config(AppArgs); //przetwarza plik konfiguracyjny
        performTrajectoryGeneration(args, config);
    }
    private static void performTrajectoryGeneration(String[] args, Config config) {
        final ErrorHandler errorHandler = new ErrorHandler();
        final long start = System.currentTimeMillis();
        try {
            //stworzenie obiektu klasy Application z użyciem konstruktora od Guice
            final Injector injector = Guice.createInjector(createContext(config, errorHandler));
            final Application app = injector.getInstance(Application.class);
            app.start(); //uruchamia aplikację widoczną w konsoli
        } catch (RuntimeException e) {
            errorHandler.handleError(e); //zgłaszanie błędów
        } finally {
            log.info("Finished after " + (Duration.ofMillis(System.currentTimeMillis() - start))); //log wyświetlany w konsoli
            RetrofitClientFactory.shutdown();
        }
    }
    private static Module[] createContext(Config generatorParameters, ErrorHandler errorHandler) {
        final Set<AbstractTrajectoryGeneratorModule> modules = new TrajectoryGeneratorModulesFactory().getModules(generatorParameters,
                errorHandler); //tworzenie modułu zagnieżdżającego klasy
        return modules.toArray(new Module[0]);
    }
}
