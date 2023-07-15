package pl.edu.agh.aolesek.bts.trajectory.generator.model;

import lombok.Data;

//własna implementacja typu danych para z użyciem szablonu
@Data
public class Pair<T1, T2> {

	private final T1 first;

	private final T2 second;

	public static <T1, T2> Pair<T1, T2> create(T1 first, T2 second) {
		return new Pair<T1, T2>(first, second);
	}
}
