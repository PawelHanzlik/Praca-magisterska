package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import java.util.Map;

//interfejs udostępniający gettery i settery dla obiektów klasy Poi
public interface IPoi {

	String getId();

	String getName();

	String getCategory();

	double getLat();

	double getLon();

	long getAverageSecondsSpent();

	void setAverageSecondsSpent(long seconds);

	Map<String, ? extends Object> getAdditionalProperties();
}
