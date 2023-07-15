package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import java.util.Collection;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

//interfejs z funkcją dostarczającą profile
public interface IProfilesProvider {

    Collection<IProfile> provideProfiles();
}
