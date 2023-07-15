package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import java.util.Collection;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

//interfejs z funkcją generującą zadaną liczbę profili. Generuje dla nich losow kolejne parametry
public interface IProfileGenerator {

    Collection<IProfile> generateProfiles(int numberOfProfiles);
}
