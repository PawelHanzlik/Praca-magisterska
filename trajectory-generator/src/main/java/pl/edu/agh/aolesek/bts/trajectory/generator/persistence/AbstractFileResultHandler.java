package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public abstract class AbstractFileResultHandler extends AbstractResultHandler {

	public AbstractFileResultHandler(Config config) {
		super(config);
	}

	@Override
	protected void saveResultsToOneFile(Collection<ProfileResult> results, String outputFolder) {

		try (PrintWriter writer = new PrintWriter(new File(filename(outputFolder)), StandardCharsets.UTF_8)) {
			writeHeader(writer);
			results.forEach(result -> writeResultToFile(result, writer));
		} catch (IOException e) {
			throw new RuntimeException("Unable to save output to one file!", e);
		}
	}

	@Override
	protected void saveResultsToSeparateFiles(Collection<ProfileResult> results, String outputFolder) {
		results.forEach(result -> {

			try (PrintWriter writer = new PrintWriter(new File(filename(outputFolder, result.getTrajectory().getTrajectoryId())),
					StandardCharsets.UTF_8)) {
				writeHeader(writer);
				writeResultToFile(result, writer);
			} catch (IOException e) {
				throw new RuntimeException("Unable to create trajectory file for " + result.getProfile().getFullName(), e);
			}
		});
	}

	protected abstract void writeHeader(PrintWriter writer);

	protected abstract void writeResultToFile(ProfileResult result, PrintWriter writer);
}
