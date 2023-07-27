package pl.edu.agh.aolesek.bts.trajectory.generator.model.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

//tworzenie w oparciu o Log4j2 loga opisującego działanie profilu
@Log4j2
public class ProfileLogger {

    @JsonIgnore
    private transient final Map<String, Object> ongoingLog = new LinkedHashMap<>();

    private final Map<String, Object> globalParameters = new LinkedHashMap<>();

    private final Map<String, Object> profileLog = new LinkedHashMap<>();

    private final Map<String, Throwable> errorLog = new LinkedHashMap<>();

    private final Map<String, Object> debugLog = new LinkedHashMap<>();

    private final IProfile profile;

    public ProfileLogger(Profile profile) {
        this.profile = profile;
    }

    public void info(String message) {
        info(message, null);
    }

    public void info(String message, Map<String, Object> properties) {
        final String timestampMessage = String.format("[%s] %s", LocalDateTime.now(), message);
        profileLog.put(timestampMessage, new LinkedHashMap<>(properties == null ? new HashMap<String, Object>() : properties));
        log.debug(profile.getFullName() + ": " + message);
    }

    public void warn(String message) {
        warn(message, null);

    }

    public void warn(String message, Throwable error) {
        final String timestampMessage = String.format("[%s] %s", LocalDateTime.now(), message);
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("throwable", error);
        profileLog.put(timestampMessage, parameters);
        log.warn(profile.getFullName() + ": " + message);
    }

    public void error(String message) {
        error(message, null);
    }

    public void error(String message, Throwable error) {
        final String timestampMessage = String.format("[%s] %s", LocalDateTime.now(), message);
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("throwable", error);
        profileLog.put(timestampMessage, parameters);
        getErrorLog().put(timestampMessage, error);
        log.error(profile.getFullName() + ": " + message, error);
    }

    public void debug(String message) {
        debug(message, null, null);
    }

    public void debug(String message, Map<String, Object> parameters) {
        debug(message, null, parameters);
    }

    public void debug(String message, Throwable t, Map<String, Object> parameters) {
        final String timestampMessage = String.format("[%s] %s", LocalDateTime.now(), message);
        final HashMap<String, Object> params = new HashMap<>(parameters == null ? new HashMap<>() : parameters);
        params.put("throwable", t);
        debugLog.put(timestampMessage, params);
        log.debug(profile.getFullName() + ": " + message);
    }

    public synchronized void appendOngoingLog(String message, Map<String, Object> eventProperties) {
        ongoingLog.put(message, eventProperties);
    }

    public synchronized void commitOngoingPLog(String commitMessage) {
        info(commitMessage, new LinkedHashMap<>(ongoingLog));
        ongoingLog.clear();
    }

    public Map<String, Object> getLog() {
        return Collections.unmodifiableMap(profileLog);
    }

    public void logParameter(String key, Object parameter) {
        globalParameters.put(key, parameter);
    }

    public Object getParameter(String key) {
        return globalParameters.get(key);
    }

    public Map<String, Throwable> getErrorLog() {
        return errorLog;
    }
}
