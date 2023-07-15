package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//zwracanie timestampów
public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static String filenameTimestamp() {
        return filenameTimestamp(LocalDateTime.now());
    }

    public static String filenameTimestamp(LocalDateTime timestamp) {
        return timestamp.format(FORMATTER);
    }
}
