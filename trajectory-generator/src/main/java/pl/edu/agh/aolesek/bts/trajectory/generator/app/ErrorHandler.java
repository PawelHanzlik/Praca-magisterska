package pl.edu.agh.aolesek.bts.trajectory.generator.app;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.TrajectoryGeneratorException;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.RoutingException;
//obsługa błędów
@Log4j2
public class ErrorHandler {

	//zgłaszanie błędów
	public void handleError(Throwable e) {
		if (e instanceof TrajectoryGeneratorException) {
			handleError(e.getCause());
		} else if (e instanceof RoutingException) {
			handleError(e.getCause());
		} else if (e instanceof HttpException) {
			final HttpException httpException = (HttpException) e;
			log.error("Http exception, response: " + httpException.response(), httpException);
		} else if (e instanceof RuntimeException && e.getCause() instanceof HttpException) {
			handleError(e.getCause());
		} else {
			log.error("An error occured!", e);
		}
	}

	public void handleError(Throwable e, IProfile profile) {
		final ProfileLogger profileLogger = profile.getLogger();
		if (e instanceof TrajectoryGeneratorException) {
			handleError(e.getCause(), profile);
		} else if (e instanceof RoutingException) {
			handleError(e.getCause(), profile);
		} else if (e instanceof HttpException) {
			final HttpException httpException = (HttpException) e;
			profileLogger.error("Http exception, response: " + httpException.response(), httpException);
		} else if (e instanceof RuntimeException && e.getCause() instanceof HttpException) {
			handleError(e.getCause(), profile);
		} else {
			profileLogger.error("An error occured!", e);
		}
	}
}
