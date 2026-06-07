package wcpredictor.config;

import wcpredictor.entity.*;
import wcpredictor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private static final Map<Character, String[]> GROUP_TEAMS = new LinkedHashMap<>() {{
        put('A', new String[]{"Mexico", "South Africa", "South Korea", "Czech Republic"});
        put('B', new String[]{"Canada", "Iran", "Scotland", "Sweden"});
        put('C', new String[]{"Argentina", "Croatia", "Australia", "Turkey"});
        put('D', new String[]{"United States", "Colombia", "Egypt", "Bosnia and Herzegovina"});
        put('E', new String[]{"Spain", "Uruguay", "Saudi Arabia", "Haiti"});
        put('F', new String[]{"France", "Paraguay", "Ghana", "Iraq"});
        put('G', new String[]{"Brazil", "Netherlands", "Qatar", "Curaçao"});
        put('H', new String[]{"Germany", "Senegal", "Uzbekistan", "Panama"});
        put('I', new String[]{"England", "Morocco", "Tunisia", "Jordan"});
        put('J', new String[]{"Portugal", "Ecuador", "Japan", "Cape Verde"});
        put('K', new String[]{"Belgium", "Switzerland", "Algeria", "New Zealand"});
        put('L', new String[]{"Norway", "Austria", "Ivory Coast", "DR Congo"});
    }};

    private static final String[] FIFA_CODES = {
        "MEX", "RSA", "KOR", "CZE", "CAN", "IRN", "SCO", "SWE",
        "ARG", "CRO", "AUS", "TUR", "USA", "COL", "EGY", "BIH",
        "ESP", "URU", "KSA", "HAI", "FRA", "PAR", "GHA", "IRQ",
        "BRA", "NED", "QAT", "CUW", "GER", "SEN", "UZB", "PAN",
        "ENG", "MAR", "TUN", "JOR", "POR", "ECU", "JPN", "CPV",
        "BEL", "SUI", "ALG", "NZL", "NOR", "AUT", "CIV", "COD"
    };

    private static final Map<String, String> FIFACODE_MAP = new LinkedHashMap<>();
    static {
        int idx = 0;
        for (var entry : GROUP_TEAMS.entrySet()) {
            for (String team : entry.getValue()) {
                FIFACODE_MAP.put(team, FIFA_CODES[idx++]);
            }
        }
    }

    // Match schedule data: matchNumber, groupLetter, team1, team2, date, venue
    private static final Object[][] GROUP_MATCHES = {
        // Matchday 1
        {1, 'A', "Mexico", "South Africa", "2026-06-11T13:00", "Estadio Azteca, Mexico City"},
        {2, 'A', "South Korea", "Czech Republic", "2026-06-11T20:00", "Estadio Akron, Zapopan"},
        {3, 'B', "Canada", "Sweden", "2026-06-12T13:00", "BMO Field, Toronto"},
        {4, 'B', "Iran", "Scotland", "2026-06-12T16:00", "BC Place, Vancouver"},
        {5, 'C', "Argentina", "Turkey", "2026-06-13T13:00", "Mercedes-Benz Stadium, Atlanta"},
        {6, 'C', "Croatia", "Australia", "2026-06-13T20:00", "Levi's Stadium, Santa Clara"},
        {7, 'D', "United States", "Bosnia and Herzegovina", "2026-06-12T20:00", "SoFi Stadium, Los Angeles"},
        {8, 'D', "Colombia", "Egypt", "2026-06-13T16:00", "NRG Stadium, Houston"},
        {9, 'E', "Spain", "Haiti", "2026-06-14T13:00", "Lincoln Financial Field, Philadelphia"},
        {10, 'E', "Uruguay", "Saudi Arabia", "2026-06-14T20:00", "Arrowhead Stadium, Kansas City"},
        {11, 'F', "France", "Iraq", "2026-06-15T13:00", "AT&T Stadium, Dallas"},
        {12, 'F', "Paraguay", "Ghana", "2026-06-15T16:00", "Gillette Stadium, Boston"},
        {13, 'G', "Brazil", "Curaçao", "2026-06-16T13:00", "Lumen Field, Seattle"},
        {14, 'G', "Netherlands", "Qatar", "2026-06-16T20:00", "MetLife Stadium, East Rutherford"},
        {15, 'H', "Germany", "Panama", "2026-06-17T13:00", "Hard Rock Stadium, Miami"},
        {16, 'H', "Senegal", "Uzbekistan", "2026-06-17T16:00", "Mercedes-Benz Stadium, Atlanta"},
        {17, 'I', "England", "Jordan", "2026-06-14T16:00", "AT&T Stadium, Dallas"},
        {18, 'I', "Morocco", "Tunisia", "2026-06-15T20:00", "SoFi Stadium, Los Angeles"},
        {19, 'J', "Portugal", "Cape Verde", "2026-06-16T16:00", "Gillette Stadium, Boston"},
        {20, 'J', "Ecuador", "Japan", "2026-06-17T20:00", "Levi's Stadium, Santa Clara"},
        {21, 'K', "Belgium", "New Zealand", "2026-06-18T13:00", "BC Place, Vancouver"},
        {22, 'K', "Switzerland", "Algeria", "2026-06-18T16:00", "BMO Field, Toronto"},
        {23, 'L', "Norway", "DR Congo", "2026-06-19T13:00", "Lincoln Financial Field, Philadelphia"},
        {24, 'L', "Austria", "Ivory Coast", "2026-06-19T20:00", "Arrowhead Stadium, Kansas City"},

        // Matchday 2
        {25, 'A', "Czech Republic", "South Africa", "2026-06-18T12:00", "Mercedes-Benz Stadium, Atlanta"},
        {28, 'A', "Mexico", "South Korea", "2026-06-18T19:00", "Estadio Akron, Zapopan"},
        {26, 'B', "Canada", "Scotland", "2026-06-19T16:00", "BC Place, Vancouver"},
        {27, 'B', "Iran", "Sweden", "2026-06-19T19:00", "BMO Field, Toronto"},
        {29, 'C', "Argentina", "Australia", "2026-06-20T13:00", "Hard Rock Stadium, Miami"},
        {30, 'C', "Croatia", "Turkey", "2026-06-20T16:00", "NRG Stadium, Houston"},
        {31, 'D', "United States", "Egypt", "2026-06-19T19:00", "SoFi Stadium, Los Angeles"},
        {32, 'D', "Colombia", "Bosnia and Herzegovina", "2026-06-20T20:00", "Levi's Stadium, Santa Clara"},
        {33, 'E', "Spain", "Saudi Arabia", "2026-06-21T13:00", "MetLife Stadium, East Rutherford"},
        {34, 'E', "Uruguay", "Haiti", "2026-06-21T16:00", "Lumen Field, Seattle"},
        {35, 'F', "France", "Ghana", "2026-06-22T13:00", "Gillette Stadium, Boston"},
        {36, 'F', "Paraguay", "Iraq", "2026-06-22T16:00", "AT&T Stadium, Dallas"},
        {37, 'G', "Brazil", "Qatar", "2026-06-23T13:00", "Lincoln Financial Field, Philadelphia"},
        {38, 'G', "Netherlands", "Curaçao", "2026-06-23T20:00", "Arrowhead Stadium, Kansas City"},
        {39, 'H', "Germany", "Uzbekistan", "2026-06-24T13:00", "Hard Rock Stadium, Miami"},
        {40, 'H', "Senegal", "Panama", "2026-06-24T16:00", "Mercedes-Benz Stadium, Atlanta"},
        {41, 'I', "England", "Tunisia", "2026-06-21T20:00", "SoFi Stadium, Los Angeles"},
        {42, 'I', "Morocco", "Jordan", "2026-06-22T20:00", "NRG Stadium, Houston"},
        {43, 'J', "Portugal", "Japan", "2026-06-23T16:00", "MetLife Stadium, East Rutherford"},
        {44, 'J', "Ecuador", "Cape Verde", "2026-06-24T20:00", "Levi's Stadium, Santa Clara"},
        {45, 'K', "Belgium", "Algeria", "2026-06-25T13:00", "BC Place, Vancouver"},
        {46, 'K', "Switzerland", "New Zealand", "2026-06-25T16:00", "BMO Field, Toronto"},
        {47, 'L', "Norway", "Ivory Coast", "2026-06-26T13:00", "Lumen Field, Seattle"},
        {48, 'L', "Austria", "DR Congo", "2026-06-26T20:00", "AT&T Stadium, Dallas"},

        // Matchday 3
        {53, 'A', "Czech Republic", "Mexico", "2026-06-24T19:00", "Estadio Azteca, Mexico City"},
        {54, 'A', "South Africa", "South Korea", "2026-06-24T19:00", "Estadio BBVA, Guadalupe"},
        {49, 'B', "Canada", "Iran", "2026-06-26T16:00", "BC Place, Vancouver"},
        {50, 'B', "Scotland", "Sweden", "2026-06-26T16:00", "BMO Field, Toronto"},
        {51, 'C', "Argentina", "Croatia", "2026-06-27T20:00", "MetLife Stadium, East Rutherford"},
        {52, 'C', "Australia", "Turkey", "2026-06-27T20:00", "NRG Stadium, Houston"},
        {55, 'D', "United States", "Colombia", "2026-06-27T13:00", "SoFi Stadium, Los Angeles"},
        {56, 'D', "Egypt", "Bosnia and Herzegovina", "2026-06-27T13:00", "Levi's Stadium, Santa Clara"},
        {57, 'E', "Spain", "Uruguay", "2026-06-28T20:00", "Hard Rock Stadium, Miami"},
        {58, 'E', "Saudi Arabia", "Haiti", "2026-06-28T20:00", "Arrowhead Stadium, Kansas City"},
        {59, 'F', "France", "Paraguay", "2026-06-29T20:00", "AT&T Stadium, Dallas"},
        {60, 'F', "Ghana", "Iraq", "2026-06-29T20:00", "Gillette Stadium, Boston"},
        {61, 'G', "Brazil", "Netherlands", "2026-06-30T20:00", "MetLife Stadium, East Rutherford"},
        {62, 'G', "Qatar", "Curaçao", "2026-06-30T20:00", "Lincoln Financial Field, Philadelphia"},
        {63, 'H', "Germany", "Senegal", "2026-07-01T20:00", "SoFi Stadium, Los Angeles"},
        {64, 'H', "Uzbekistan", "Panama", "2026-07-01T20:00", "Levi's Stadium, Santa Clara"},
        {65, 'I', "England", "Morocco", "2026-06-29T13:00", "Lumen Field, Seattle"},
        {66, 'I', "Tunisia", "Jordan", "2026-06-29T13:00", "BC Place, Vancouver"},
        {67, 'J', "Portugal", "Ecuador", "2026-07-01T13:00", "Hard Rock Stadium, Miami"},
        {68, 'J', "Japan", "Cape Verde", "2026-07-01T13:00", "Mercedes-Benz Stadium, Atlanta"},
        {69, 'K', "Belgium", "Switzerland", "2026-07-02T20:00", "BMO Field, Toronto"},
        {70, 'K', "Algeria", "New Zealand", "2026-07-02T20:00", "BC Place, Vancouver"},
        {71, 'L', "Norway", "Austria", "2026-07-03T20:00", "NRG Stadium, Houston"},
        {72, 'L', "Ivory Coast", "DR Congo", "2026-07-03T20:00", "AT&T Stadium, Dallas"},
    };

    private static final Map<String, String> VENUE_TIMEZONE = Map.ofEntries(
        Map.entry("Mexico City", "America/Mexico_City"),
        Map.entry("Zapopan", "America/Mexico_City"),
        Map.entry("Guadalupe", "America/Mexico_City"),
        Map.entry("Toronto", "America/Toronto"),
        Map.entry("Vancouver", "America/Vancouver"),
        Map.entry("Atlanta", "America/New_York"),
        Map.entry("Santa Clara", "America/Los_Angeles"),
        Map.entry("Los Angeles", "America/Los_Angeles"),
        Map.entry("Houston", "America/Chicago"),
        Map.entry("Philadelphia", "America/New_York"),
        Map.entry("Kansas City", "America/Chicago"),
        Map.entry("Dallas", "America/Chicago"),
        Map.entry("Boston", "America/New_York"),
        Map.entry("Seattle", "America/Los_Angeles"),
        Map.entry("East Rutherford", "America/New_York"),
        Map.entry("Miami", "America/New_York")
    );

    private static LocalDateTime toUtc(LocalDateTime localTime, String venue) {
        for (var entry : VENUE_TIMEZONE.entrySet()) {
            if (venue.contains(entry.getKey())) {
                return ZonedDateTime.of(localTime, ZoneId.of(entry.getValue()))
                        .withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
            }
        }
        return localTime;
    }

    @Bean
    public CommandLineRunner loadData(TeamRepository teamRepo, MatchRepository matchRepo,
                                       SettingRepository settingRepo, UserRepository userRepo,
                                       PasswordEncoder passwordEncoder, GameRepository gameRepo,
                                       TournamentRepository tournamentRepo) {
        return args -> {
            Tournament tournament = tournamentRepo.findAll().stream().findFirst().orElse(null);
            if (tournament == null) {
                tournament = new Tournament();
                tournament.setName("2026 FIFA World Cup");
                tournament.setDescription("Canada, Mexico, United States");
                tournament.setCreatedAt(java.time.Instant.now());
                tournament = tournamentRepo.save(tournament);
                log.info("Created default tournament: {}", tournament.getName());
            }

            if (teamRepo.count() > 0) {
                log.info("Teams already loaded, skipping seed.");
                for (Team team : teamRepo.findAll()) {
                    if (team.getTournament() == null) {
                        team.setTournament(tournament);
                        teamRepo.save(team);
                        log.info("Assigned team {} to tournament {}", team.getName(), tournament.getName());
                    }
                }
            } else {
                log.info("Seeding teams and matches...");
                Map<String, Team> teamMap = new HashMap<>();

                for (var entry : GROUP_TEAMS.entrySet()) {
                    char group = entry.getKey();
                    for (String teamName : entry.getValue()) {
                        Team team = new Team();
                        team.setName(teamName);
                        team.setGroupLetter(String.valueOf(group));
                        team.setFifaCode(FIFACODE_MAP.get(teamName));
                        team.setTournament(tournament);
                        teamMap.put(teamName, teamRepo.save(team));
                    }
                }

                int md1Count = 0, md2Count = 0, md3Count = 0;
                Map<RoundType, LocalDateTime> earliestKickoff = new HashMap<>();
                List<Match> seededMatches = new ArrayList<>();

                for (Object[] m : GROUP_MATCHES) {
                    int matchNum = (int) m[0];
                    char group = (char) m[1];
                    String t1Name = (String) m[2];
                    String t2Name = (String) m[3];
                    String dateStr = (String) m[4];
                    String venue = (String) m[5];

                    RoundType round;
                    if (matchNum <= 24) { round = RoundType.GROUP_MD1; md1Count++; }
                    else if (matchNum <= 48) { round = RoundType.GROUP_MD2; md2Count++; }
                    else { round = RoundType.GROUP_MD3; md3Count++; }

                    LocalDateTime kickoff = toUtc(LocalDateTime.parse(dateStr), venue);
                    if (!earliestKickoff.containsKey(round) || kickoff.isBefore(earliestKickoff.get(round))) {
                        earliestKickoff.put(round, kickoff);
                    }

                    Match match = new Match();
                    match.setMatchNumber(matchNum);
                    match.setRound(round);
                    match.setGroupLetter(String.valueOf(group));
                    match.setTeam1(teamMap.get(t1Name));
                    match.setTeam2(teamMap.get(t2Name));
                    match.setMatchDate(kickoff);
                    match.setVenue(venue);
                    match.setTournament(tournament);
                    seededMatches.add(match);
                }

                for (Match match : seededMatches) {
                    match.setPredictionsLockTime(earliestKickoff.get(match.getRound()));
                    matchRepo.save(match);
                }
                log.info("Seeded 48 teams, {} group matches (MD1: {}, MD2: {}, MD3: {})", GROUP_MATCHES.length, md1Count, md2Count, md3Count);
            }

            for (Match match : matchRepo.findAll()) {
                boolean changed = false;
                if (match.getTournament() == null) {
                    match.setTournament(tournament);
                    changed = true;
                }
                if (match.getPredictionsLockTime() == null && match.getMatchDate() != null) {
                    changed = true;
                }
                if (changed) matchRepo.save(match);
            }

            for (Match match : matchRepo.findAll()) {
                var round = match.getRound();
                if (round != null) {
                    var earliest = matchRepo.findByRoundOrderByMatchDateAsc(round).stream()
                            .map(Match::getMatchDate)
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    if (earliest != null && !earliest.equals(match.getPredictionsLockTime())) {
                        match.setPredictionsLockTime(earliest);
                        matchRepo.save(match);
                    }
                }
            }

            if (settingRepo.count() == 0) {
                log.info("Seeding default settings...");
                Setting s;
                s = new Setting(); s.setName("points_correct_score"); s.setValue("3"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_correct_margin"); s.setValue("2"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_correct_result"); s.setValue("1"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_draw_correct"); s.setValue("3"); s.setValueType("DOUBLE"); settingRepo.save(s);
                s = new Setting(); s.setName("points_draw_diff"); s.setValue("1.5"); s.setValueType("DOUBLE"); settingRepo.save(s);
                s = new Setting(); s.setName("points_golden_boot"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_golden_ball"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_golden_glove"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_young_player"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_fair_play"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_entertaining"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_finalist"); s.setValue("5"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_champion"); s.setValue("20"); s.setValueType("INTEGER"); settingRepo.save(s);
                s = new Setting(); s.setName("points_group_qualifier"); s.setValue("2"); s.setValueType("INTEGER"); settingRepo.save(s);
                log.info("Seeded 14 default settings.");
            }

            if (userRepo.count() == 0) {
                log.info("Creating default admin user: wcpredictor@thatcher.africa");
                User admin = new User();
                admin.setEmailAddress("wcpredictor@thatcher.africa");
                admin.setFirstName("Admin");
                admin.setLastName("");
                admin.setNickname("Admin");
                admin.setEncryptedPassword(passwordEncoder.encode("password"));
                admin.setAdmin(true);
                admin.setConfirmed(true);
                userRepo.save(admin);
                log.info("Admin user created.");
            } else {
                for (User user : userRepo.findAll()) {
                    if (user.getConfirmed() == null) {
                        user.setConfirmed(true);
                        userRepo.save(user);
                        log.info("Marked existing user {} as confirmed", user.getEmailAddress());
                    }
                }
            }

            if (gameRepo.count() == 0) {
                log.info("Creating default game...");
                Game game = new Game();
                game.setName("Default Game");
                game.setDescription("The main 2026 World Cup predictor competition");
                game.setCreatedAt(java.time.Instant.now());
                var adminUser = userRepo.findByEmailAddress("wcpredictor@thatcher.africa");
                adminUser.ifPresent(u -> game.getUsers().add(u));
                gameRepo.save(game);
                log.info("Default game created with admin user.");
            }
        };
    }
}
