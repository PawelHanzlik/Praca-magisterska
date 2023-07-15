package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

//struktura zawierająca listę odcinków tworzących trasę. Oprócz listy zawiera czas, w którym rozpoczęto trasę
@Data
@AllArgsConstructor
public class RoutePlan implements IRoutePlan {

	private final List<IRoutePart> parts; //odcinki trasy

	private final LocalDateTime startTime; //godzina rozpoczęcia wędrówki
}
