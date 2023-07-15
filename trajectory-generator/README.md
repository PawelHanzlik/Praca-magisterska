# Generator trajektorii
Aplikacja pozwala na generowanie trajektorii turystów. Generowanie trajektorie mają możliwie najwierniej oddawać ludzkie zachowanie i być alternatywą dla trajektorii pochodzących z sieci telefonii komórkowej.

## Uruchamianie
Główną klasą generatora trajektorii jest `pl.edu.agh.aolesek.bts.trajectory.generator.app.Application`. W chwili obecnej projekt należy uruchamiać przez wybrane IDE. Jako argument należy podać lokalizacje pliku konfiguracyjnego, w przypadku braku argumentu aplikacja korzysta z dostarczonego pliku konfiguracyjnego. Opis poszczególnych wartości w pliku konfiguracyjnym znajduje się w jednym z kolejnych rozdziałów.

## Moduły
Funkcje aplikacji mogą być dodawane bądź usuwane poprzez tworzenie modułów, w których budowany jest kontekst aplikacji, a więc wszystkie usługi, które są następnie wykorzystywane przez generator. Odpowiedni moduł można wybrać zmieniając odpowiednią wartość w pliku konfiguracyjnym. Aplikacja posiada 3 wbudowane moduły: moduł OSM + ORS, moduł OSM + ORS + System rekomendacyjny, moduł Google Maps API. Należy zapoznać się z zawartością pakietu `pl.edu.agh.aolesek.bts.trajectory.generator.app.modules` w celu modyfikacji/dodawania nowych funkcjonalności.

## Konfiguracja - parametry generatora
Opis parametrów możliwych do ustawienia w konfiguracji znajduje się również w typie wyliczeniowym *Parameters*:
```
public enum Parameters {
    /**
     * URL of Overpass API. An example of free Overpass API is https://lz4.overpass-api.de/api/.
     */
    OVERPASS_URL,

    /**
     * API Key for Overpass API. Required only for some API instances.
     */
    OVERPASS_KEY,

    /**
     * URL of OpenRouteService API instance. An example of free ORS API is https://api.openrouteservice.org/v2/
     */
    ORS_URL,

    /**
     * API Key for ORS API. Required only for some API instances.
     */
    ORS_KEY,

    /**
     * URL of Google Maps API. e. g. https://maps.googleapis.com/maps/api/
     */
    GOOGLE_API_URL,

    /**
     * API Key for Google Maps API.
     */
    GOOGLE_API_KEY,

    /**
     * Number of generated profiles. The total number of profiles is sum of profiles from INPUT_PROFILES_DIRECTORY and generated profiles.
     */
    NUMBER_OF_GENERATED_PROFILES,

    /**
     * Number of trajectories per one (generated or provided) profile.
     */
    NUMBER_OF_TRAJECTORIES_PER_PROFILE,

    /**
     * Factor used to simulate preference of one of available transport modes.
     */
    PREF_TRANSPORT_MODE_FACTOR,

    /**
     * Base number that will be used to determine number of POIs to visit in one trajectory.
     */
    BASE_NUMBER_OF_VISITED_POIS,
    BASE_NUMBER_OF_VISITED_POIS_DEVIATION,

    /**
     * Numbers used to generate walking speed for trajectories.
     */
    WALKING_SPEED_MODIFIER_BASE,
    WALKING_SPEED_MODIFIER_BASE_DEVIATION,
    AVERAGE_WALKING_SPEED,

    /**
     * Numbers used to determine max distance from start point for trajectory.
     */
    MAX_DISTANCE_FROM_START_BASE,
    MAX_DISTANCE_FROM_START_BASE_DEVIATION,

    /**
     * Numbers used to provide random time of visit if such data is not available from the provider.
     */
    GENERATED_SPENT_SECONDS_BASE,
    GENERATED_SPENT_SECONDS_MAX,
    GENERATED_SPENT_SECONDS_BASE_PERCENTAGE_DEVIATION,

    /**
     * Max number of recommendations for one trajectory. Works only if proper POIRecommender is set.
     */
    MAX_NUMBER_OF_RECOMMENDED_POIS_FOR_TRAJECTORY,

    /**
     * Below this distance, generator will shortcut some points of routing. In case of transport methods other than foot walking, routing
     * received from external provider may not be complete, e.g. when driving a car, it is sometimes impossible to reach target point
     * directly by car. In such a cases, generator will query external provider for routing from SOURCE point to ROUTE START POINT (e. g.
     * from office to parking), but querying external provider will be performed only if distance from SOURCE point to ROUTE START POINT is
     * greater than this variable.
     */
    MAX_DISTANCE_TO_SHORTCUT,

    /**
     * Should execute validators for generted trajectories.
     */
    SHOULD_VALIDATE,

    /**
     * Constants used to configure preferences regarding multiple visits in POI of same category.
     */
    CATEGORIES_TO_AVOID_MULTIPLE_VISITS,
    MAX_POIS_WITH_SAME_CATEGORY,
    MAX_POIS_WITH_SAME_CATEGORY_WHEN_AVOIDING,

    /**
     * Every profile has its preferred number of visited POIs. This number is used to generate random number based on mentioned variable.
     */
    AVERAGE_NUMBER_OF_VISITED_POIS_DEVIATION,

    /**
     * Modules set used for building context for validator. Possible values available in {@link TrajectoryGeneratorModulesFactory}:
     * OpenRouteServiceAndOverpass, OpenRouteServiceOverpassRecommender, GoogleMapsAPI
     */
    MODULE,

    /**
     * Should simulated tourist go to home after visiting all POIs.
     */
    SHOULD_GO_BACK_AT_THE_END,

    /**
     * Directory with profiles to use while generating. Directory to save results.
     */
    INPUT_PROFILES_DIRECTORY,
    OUTPUT_DIRECTORY,

    /**
     * Determines whether output files will be generates one file per profile/one file per result handler.
     */
    OUTPUT_IN_ONE_FILE,

    /**
     * Numbers used while generating random profiles.
     */
    BASE_NUMBER_OF_INTERESTS,
    BASE_NUMBER_OF_INTERESTS_DEVIATION,

    /**
     * ALS Poi recommender requires to build model for providing recommendations. The model will be built every BUILD_MODEL_EVERY_N_RATINGS
     * ratings added to recommender.
     */
    BUILD_MODEL_EVERY_N_RATINGS,
}
```

## Dostawcy danych
W celu uzyskania punktów POI dla każdego profilu, jak również w celu znalezienia drogi pomiędzy poszczególnymi punktami, generator korzysta z zewnętrznych dostawców danych. Dwa główne źródła danych to OpenStreetMaps oraz OpenRouteService.
Dodatkowo zaimplementowano również odpowiednie mechanizmy, które pozwalają na współprace z Google Maps API. W celu zmiany dostawcy na Google Maps API należy w pliku konfiguracyjnym zmienić wartość parametru 'MODULE' oraz uzupełnić parametry z adresem Google Maps API oraz kluczem do API.
Podczas rozwijania aplikacji wykorzystywano głównie dostawców OpenStreetMaps oraz OpenRouteService. W klasie `Parameters` znajdują się przykładowe adresy publicznie dostępnych instancji ORS API i OSM API, które jednak posiadają pewne ograniczenia (można wysłać tylko określoną liczbę zapytań). W pliku konfiguracyjnym dostarczonym z aplikacją ustawione są instancje znajdujące się pod adresem http://164.132.104.73 . Serwer został uruchomiony specjalnie dla opisywanej aplikacji i nie ma żadnych ograniczeń w liczbie zapytań (jedynie ilość dostępnych zasobów), jednak nie gwarantuję że będzie on dostępny przez 100% czasu.
Możliwe jest uruchomienie własnych instancji API (np. lokalnie, dla zwiększenia szybkości działania generatora, lub w przypadku problemów z serwerem z konfiguracji). Szczegóły dostępne są pod następującymi adresami:
https://wiki.openstreetmap.org/wiki/Overpass_API/Installation
https://github.com/GIScience/openrouteservice
Łatwym sposobem uruchomienia własnych instancji jest skorzystanie z gotowych obrazów Dockera:
`wiktorn/overpass-api:latest` (szczegóły https://github.com/wiktorn/Overpass-API)
`openrouteservice/openrouteservice:latest` (szczegółowy opis uruchamiania znajduje się pod adresem https://github.com/GIScience/openrouteservice/blob/master/docker/README.md)
Jako że podczas prac wykorzystywano głównie ORS i OSM, możliwe że wykorzystanie Google Maps API może być problematyczne i będzie wymagać jeszcze pewnych nakładów pracy. Nalezy się upewnić, że ustawiony klucz API Google na pewno jest poprawny i ma dostęp do koniecznych usług - w przypadku niektóych błędów generator nie zgłosi błędu na konsoli, tylko zwróci pustą listę POI, co spowoduje niepoprawne działanie.


## System rekomendacyjny
W jednym z modułów zaimplementowany jest mechanizm rekomendacji punktów POI na podstawie wcześniejszych ocen. Mechanizm korzysta z biblioteki Spark oraz metody ALS (alternating least squares). Parametr `BUILD_MODEL_EVERY_N_RATINGS` pozwala określić co jaką liczbę ocen budowany będzie model rekomendacji.
W celu poprawnego działania Spark wymaga zainstalowania w systemie `Hadoop`, lub przynajmniej dostarczenia aplikacji `\winutils.exe`. W przypadku jej braku, działanie aplikacji zakończy się wyjątkiem. W moim przypadku wystarczyło pobranie i skopiowanie `winutils.exe` we wskazane przez Sparka miejsce, szczegóły dostępne tutaj:https://stackoverflow.com/questions/45613687/spark-ml-without-hadoop-installed. 
W celu wyłączenia rekomendacji (np. w celu zwiększenia szybkości działania), należy wybrać odpowiedni moduł bądź zdefiniować w module `DummyRecommender` jako dostawcę rekomendacji.
Rekomendacje działają najlepiej w przypadku dużych zbiorów - w przypadku małych zbiorów, przy generowaniu początkowych trajektorii liczba ocen może być za niska do zbudowania modelu, więc nie otrzymamy żadnych rekomendacji. Można zmienić tę liczbę zgodnie z informacjami z początku tego rozdziału.

## Generowanie profilil
Domyślnie dostarczone profile znajdują się w katalogu `profiles/`. Są tam dwa przykładowe profile w formacie .json. Otwierając pliki edytorem tekstu można zapoznać się ze składnią.
Poza dostarczaniem profili z zewnątrz, możliwe jest generowanie profili. Należy skorzystać z parametru `NUMBER_OF_GENERATED_PROFILES` w pliku konfiguracyjnym. Generatory profili losują zainteresowania z puli oraz wybierają punkty początkowe ze zdefiniowanej wcześniej listy. Ograniczona liczba punktów początkowych może jednak wpływać na wyniki, więc może być konieczne generowanie tych punktów lub przynajmniej zwiększenie liczby wbudowanych. 

## Obszar generowania trajektorii
Aplikacja była tworzona z wykorzystaniem dostawcy danych, który posiada jedynie dane OSM dla małopolski. Możliwe jest uruchomienie generatora dla dowolnego innego obszaru, należy jednak mieć świadomość, że:
* Dostawca danych znajdujący się pod adresem w konfiguracji obsługuje jedynie małopolskę, więc należy zmienić dostawcę na Google / jedną z publicznie dostępnych instancji API ORS i OSM.
* Generatory profili mają wbudowane listy punktów startowych znajdujacych się na obszarze Krakowa. Przed uruchomieniem generatora profili na innym obszarze, należy zmienić te punkty bądź zmienić mechanizm w taki sposób, aby losował punkty początkowe w wybranym obszarze.

## Dane wyjściowe
Większość procedur obsługi wyjscia obsługuje 2 tryby - w jednym pliku, bądź jeden plik na trajektorię. Szczegóły w opisie parametrów.

* Plik `.story` zawiera słowny opis trajektorii. Na początku każdego wpisu znajduje się krótkie podsumowanie profilu, następnie lista odwiedzonych miejsc wraz z danymi o czasie i miejscu, na końcu znajduje się krótkie podsumowanie trajektorii.
* Plik `.csv` zawiera trajektorię, która można zaimportować do arkusza kalkulacyjnego. W pliku znajdują się dane pozwalające na identyfikację trajektorii, jej właściciela, a także stempel czasowy i współrzędne oraz to, czy punkt jest tylko elemetem drogi, czy jest to POI.
* Plik `.json` zawiera zapis całej struktury wygenerowanej przez generator. Sa tam wszelkie informacje o profilu, punktach POI, planie trajektorii oraz o samej trajektorii.
* Plik `.log.json` zawiera log, w którym przechowywane są szczegóły dotyczące działania generatora. Można tam znaleźć informacje na temat podjętych decyzji, problemów, dostępności danych...
* Plik `.geojson` zawiera trajektorie wraz z punktami POI. Wspólne punkty POI dla profili nie są agregowane, więc nachodzą na siebie jeżeli istnieją.
* Plik `.one.geojson` zawiera trajektorie wraz z zagregowanymi punktami POI. Brak tutaj informacji o przynalezności POI do poszczególnych trajektorii, gdyż mogą należeć do więcej niż jednej kategorii. Ten format nie jest obsługiwany w przypadku zapisu do pojedynczych plików.


## Pozostałe uwagi
Wszystkie zalezności są pobierane przy pomocy mavena. Projekt korzysta z Lomboka, więc przed otwarciem projektu w IDE konieczna będzie jego instalacja.
Identyfikator trajektorii ma następującą postać:
ImieNazwisko@ID_TRAJEKTORII [profileId ID_PROFILU]

# Statystyki

W celu wygenerowania statystyk należy uruchomić klasę `pl.edu.agh.aolesek.bts.trajectory.analysis.stats.StatsGeneratorApp` z parametrem wskazującym na plik wynikowy `json`, który ma być poddany analizie. w przypadku niewskazania żadnego argumentu, statystyki zostaną wygenerowane dla przykładowego pliku.
W aktualnej wersji statystyki są wypisywane na konsolę, działają tylko wtedy, gdy zapisujemy wyniki symulacji do jednego pliku (1 plik na 1 handler, patrz: opis parametrów generatora).
