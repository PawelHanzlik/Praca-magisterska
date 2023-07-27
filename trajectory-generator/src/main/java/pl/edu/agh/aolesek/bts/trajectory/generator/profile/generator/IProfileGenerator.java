package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

import java.util.Collection;

//interfejs z funkcją generującą zadaną liczbę profili. Generuje dla nich losow kolejne parametry
public interface IProfileGenerator {

    Collection<IProfile> generateProfiles(int numberOfProfiles);
}
