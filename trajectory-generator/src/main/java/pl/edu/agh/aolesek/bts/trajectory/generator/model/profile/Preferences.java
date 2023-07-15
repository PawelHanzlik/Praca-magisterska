package pl.edu.agh.aolesek.bts.trajectory.generator.model.profile;

import lombok.Data;

//preferencje profilu
@Data
public class Preferences {

	private final ActivityTime activityTime;

	private final int averageNumberOfPOIs;

	private final double spendTimeModifier;

	private final double walkingSpeedModifier;

	private final double interestFactor;

	private final double priceFactor;

	private final double distanceFactor;

	public enum ActivityTime {
		MORNING,
		MIDDAY,
		EVENING
	}
}
