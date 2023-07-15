package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;

//interfejs udostępniająca informację o poziomie cenowym z przedziału A-E (obiekt klasy Level)
public interface IPricesProvider {

	Level resolvePriceLevel(IPoi element);
}
