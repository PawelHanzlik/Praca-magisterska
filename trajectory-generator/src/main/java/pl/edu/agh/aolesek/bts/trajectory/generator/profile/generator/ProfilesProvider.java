package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import pl.edu.agh.aolesek.bts.trajectory.analysis.ResultReader.InterfaceSerializer;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.Profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters.NUMBER_OF_GENERATED_PROFILES;

//przy wykorzystaniu interfejsu IProfilesProvider dostarcza profile utworzone przez użytkownika (domyślnie z folderu profiles)
// i zleca wygenerowanie określonej w konfiguracji liczby losowych profili
@Log4j2
public class ProfilesProvider implements IProfilesProvider {

    private final IProfileGenerator profileGenerator;

    private final Config config;

    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(IProfile.class, InterfaceSerializer.interfaceSerializer(Profile.class))
        .create();

    @Inject
    public ProfilesProvider(IProfileGenerator profileGenerator, Config generatorParameters) {
        this.profileGenerator = profileGenerator;
        this.config = generatorParameters;
    }

    @Override
    public Collection<IProfile> provideProfiles() {
        return Stream.concat(parseProfiles().stream(), generateProfiles().stream()).collect(Collectors.toSet());
    }

    private Collection<IProfile> parseProfiles() {
        final String inputProfilesDirectory = config.get(Parameters.INPUT_PROFILES_DIRECTORY);
        final Path profilesDirectoryPath = Paths.get(inputProfilesDirectory);

        final Set<IProfile> profiles = new HashSet<>();
        final Iterator<File> filesIterator = FileUtils.iterateFiles(profilesDirectoryPath.toFile(), null, false);
        while (filesIterator.hasNext()) {
            final File nextFile = filesIterator.next();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(nextFile)))) {
                log.info("Parsing profile " + nextFile.getName());
                IProfile profile = gson.fromJson(reader, IProfile.class);
                System.out.println(profile.getFullName());
//                if (profile.getFullName().equals("Teenager2")) {
//                    profiles.add(profile);
//                }
                if (profile.getFullName().equals("Student2")) {
                   profiles.add(profile);
                }

                } catch (Exception e) {
                log.error("Unable to parse profile " + nextFile.getName(), e);
            }
        }

        return profiles;
    }

    //przetwarza liczbę profili do wygenerowania
    private Collection<IProfile> generateProfiles() {

        final int numberOfProfilesToGenerate = config.getInt(NUMBER_OF_GENERATED_PROFILES);
        return numberOfProfilesToGenerate >= 0
            ? profileGenerator.generateProfiles(numberOfProfilesToGenerate)
            : Collections.emptySet();
    }
}
