package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

import java.io.File;
import java.util.Collection;

import static pl.edu.agh.aolesek.bts.trajectory.generator.utils.TimeUtils.filenameTimestamp;

public abstract class AbstractResultHandler implements IResultHandler {

    private final Config config;

    public AbstractResultHandler(Config config) {
        this.config = config;
    }

    @Override
    public void handleResult(Collection<ProfileResult> results) {
        String outputFolder = resolveOutputFolder();
        if (!new File(outputFolder).exists()) {
            new File(outputFolder).mkdirs();
        }

        if (config.getBoolean(Parameters.OUTPUT_IN_ONE_FILE)) {
            saveResultsToOneFile(results, outputFolder);
        } else {
            saveResultsToSeparateFiles(results, outputFolder);
        }
    }

    private String resolveOutputFolder() {
        return config.getBoolean(Parameters.OUTPUT_IN_ONE_FILE) ? config.get(Parameters.OUTPUT_DIRECTORY)
            : config.get(Parameters.OUTPUT_DIRECTORY) + filenameTimestamp() + "_" + getFormat() + "/";
    }

    protected abstract String getFormat();

    protected abstract void saveResultsToOneFile(Collection<ProfileResult> results, String outputFolder);

    protected abstract void saveResultsToSeparateFiles(Collection<ProfileResult> results, String outputFolder);

    protected String filename(String outputFolder) {
        return outputFolder + filenameTimestamp() + "_trajectories." + getFormat();
    }

    protected String filename(String outputFolder, String profileName) {
        return outputFolder + filenameTimestamp() + "_" + profileName + "_trajectory." + getFormat();
    }
}
