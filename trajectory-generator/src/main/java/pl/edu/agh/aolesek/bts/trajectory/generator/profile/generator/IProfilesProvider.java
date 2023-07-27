package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

import java.util.Collection;

//interfejs z funkcją dostarczającą profile
public interface IProfilesProvider {

    Collection<IProfile> provideProfiles();
}
