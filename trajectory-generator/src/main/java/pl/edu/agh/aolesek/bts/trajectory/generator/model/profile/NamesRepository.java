package pl.edu.agh.aolesek.bts.trajectory.generator.model.profile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.extern.log4j.Log4j2;

import static com.google.common.base.Predicates.not;

//przetwarzanie bazy imion i nazwisk wykorzystywanej przez generator profili
@Log4j2
public class NamesRepository {

    private final Set<String> names = new HashSet<>();

    private final Set<String> firstNames = new HashSet<>();

    private final Set<String> lastNames = new HashSet<>();

    private static final NamesRepository NAMES_REPOSITORY = new NamesRepository();

    private static final Random RND = new Random();

    public NamesRepository() {
        //wczytywanie informacji z bazy
        try (Scanner namesScanner = new Scanner(new File(getClass().getClassLoader().getResource("names.txt").getFile()));
            Scanner surnamesScanner = new Scanner(new File(getClass().getClassLoader().getResource("surnames.txt").getFile()))) {
            while (namesScanner.hasNextLine()) {
                firstNames.add(namesScanner.nextLine().trim());
            }
            while (surnamesScanner.hasNextLine()) {
                lastNames.add(surnamesScanner.nextLine().trim());
            }
        } catch (IOException e) {
            log.warn("Unable to load names from file!", e);
        }
    }

    //generowanie imion i nazwisk
    public synchronized static String createNewName() {
        return NAMES_REPOSITORY.generateUniqueName();
    }

    private String generateUniqueName() {
        return Stream.iterate(generateName(), nextName -> generateName())
            .filter(not(names::contains))
            .limit(1)
            .findFirst()
            .orElseGet(() -> oddDesperateName());
    }

    private String generateName() {
        final String randomName = randomName();
        final String niceWrittenName = randomName.substring(0, 1).toUpperCase() + randomName.substring(1).toLowerCase();

        final String randomSurname = randomSurname();
        final String niceWrittenSurname = randomSurname.substring(0, 1).toUpperCase() + randomSurname.substring(1).toLowerCase();

        return niceWrittenName + niceWrittenSurname;
    }

    private String oddDesperateName() {
        final String randomName = randomName();
        final String niceWrittenName = randomName.substring(0, 1).toUpperCase() + randomName.substring(1).toLowerCase();

        final String oddSurname = UUID.randomUUID().toString().substring(0, 8);
        final String niceWrittenSurname = oddSurname.substring(0, 1).toUpperCase() + oddSurname.substring(1).toLowerCase();

        return niceWrittenName + niceWrittenSurname;
    }

    private String randomName() {
        return firstNames.stream().skip(RND.nextInt(firstNames.size())).findFirst().orElse("Mike");
    }

    private String randomSurname() {
        return lastNames.stream().skip(RND.nextInt(lastNames.size())).findFirst().orElse("November");
    }
}
