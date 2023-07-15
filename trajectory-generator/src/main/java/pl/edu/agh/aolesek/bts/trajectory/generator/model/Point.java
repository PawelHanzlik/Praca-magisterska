package pl.edu.agh.aolesek.bts.trajectory.generator.model;

import lombok.Data;


//strutura danych do przechowywania par współrzędnych geograficznych
@Data
public class Point {

	private final Double lat;

	private final Double lon;
}
