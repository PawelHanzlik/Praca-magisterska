package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

//interfejs zawierający funkcję informującą o czasie spędzonym w obiekcie Poi
public interface ISpentTimeProvider {

	long resolveSecondsSpent(IPoi poi);
}
