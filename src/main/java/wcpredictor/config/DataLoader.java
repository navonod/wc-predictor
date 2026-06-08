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
        put('B', new String[]{"Canada", "Bosnia and Herzegovina", "Qatar", "Switzerland"});
        put('C', new String[]{"Brazil", "Morocco", "Haiti", "Scotland"});
        put('D', new String[]{"United States", "Paraguay", "Australia", "Turkey"});
        put('E', new String[]{"Germany", "Curaçao", "Ivory Coast", "Ecuador"});
        put('F', new String[]{"Netherlands", "Japan", "Sweden", "Tunisia"});
        put('G', new String[]{"Belgium", "Egypt", "Iran", "New Zealand"});
        put('H', new String[]{"Spain", "Cape Verde", "Saudi Arabia", "Uruguay"});
        put('I', new String[]{"France", "Senegal", "Iraq", "Norway"});
        put('J', new String[]{"Argentina", "Algeria", "Austria", "Jordan"});
        put('K', new String[]{"Portugal", "DR Congo", "Uzbekistan", "Colombia"});
        put('L', new String[]{"England", "Croatia", "Ghana", "Panama"});
    }};

    private static final String[] FIFA_CODES = {
        "MEX", "RSA", "KOR", "CZE", "CAN", "BIH", "QAT", "SUI",
        "BRA", "MAR", "HAI", "SCO", "USA", "PAR", "AUS", "TUR",
        "GER", "CUW", "CIV", "ECU", "NED", "JPN", "SWE", "TUN",
        "BEL", "EGY", "IRN", "NZL", "ESP", "CPV", "KSA", "URU",
        "FRA", "SEN", "IRQ", "NOR", "ARG", "ALG", "AUT", "JOR",
        "POR", "COD", "UZB", "COL", "ENG", "CRO", "GHA", "PAN"
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
                                       PasswordEncoder passwordEncoder, PoolRepository poolRepo,
                                       TournamentRepository tournamentRepo,
                                       GroupAdvancementPredictionRepository gapRepo) {
        return args -> {
            Tournament tournament = tournamentRepo.findAll().stream().findFirst().orElse(null);
            if (tournament == null) {
                tournament = new Tournament();
                tournament.setName("FIFA World Cup 2026");
                tournament.setDescription("Canada, Mexico, United States");
                tournament.setCreatedAt(java.time.Instant.now());
                tournament = tournamentRepo.save(tournament);
                log.info("Created default tournament: {}", tournament.getName());
            }

            if (teamRepo.count() > 0) {
                log.info("Teams already loaded, migrating group assignments.");
                int orderIdx = 0;
                for (var entry : GROUP_TEAMS.entrySet()) {
                    for (String teamName : entry.getValue()) {
                        for (Team team : teamRepo.findAll()) {
                            if (team.getName().equals(teamName)) {
                                if (!String.valueOf(entry.getKey()).equals(team.getGroupLetter())) {
                                    log.info("Moving {} from group {} to {}",
                                            team.getName(), team.getGroupLetter(), entry.getKey());
                                    team.setGroupLetter(String.valueOf(entry.getKey()));
                                }
                                if (team.getSortOrder() != orderIdx) {
                                    team.setSortOrder(orderIdx);
                                }
                                teamRepo.save(team);
                                break;
                            }
                        }
                        orderIdx++;
                    }
                }
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

                int idx = 0;
                for (var entry : GROUP_TEAMS.entrySet()) {
                    char group = entry.getKey();
                    for (String teamName : entry.getValue()) {
                        Team team = new Team();
                        team.setName(teamName);
                        team.setGroupLetter(String.valueOf(group));
                        team.setFifaCode(FIFACODE_MAP.get(teamName));
                        team.setSortOrder(idx);
                        team.setTournament(tournament);
                        teamMap.put(teamName, teamRepo.save(team));
                        idx++;
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

            fixOverstuffedGroupPredictions(gapRepo, teamRepo, userRepo);

            for (Match match : matchRepo.findAll()) {
                boolean changed = false;
                if (match.getTournament() == null) {
                    match.setTournament(tournament);
                    changed = true;
                }
                if (match.getPredictionsLockTime() == null && match.getMatchDate() != null) {
                    changed = true;
                }
                if (match.getVenue() != null && !match.getVenue().isEmpty()) {
                    for (Object[] m : GROUP_MATCHES) {
                        if ((int) m[0] == match.getMatchNumber()) {
                            String rawDate = (String) m[4];
                            LocalDateTime seedDate = LocalDateTime.parse(rawDate);
                            if (match.getMatchDate().equals(seedDate)) {
                                match.setMatchDate(toUtc(seedDate, match.getVenue()));
                                changed = true;
                            }
                            break;
                        }
                    }
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

            importScheduleCsvs(tournamentRepo, matchRepo, teamRepo);

            for (Match match : matchRepo.findAll()) {
                var round = match.getRound();
                if (round != null && !match.isPredictionsLocked()) {
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

            if (poolRepo.count() == 0) {
                log.info("Creating default pool...");
                Pool pool = new Pool();
                pool.setName("Default Pool");
                pool.setDescription("The main predictor competition pool");
                pool.setCreatedAt(java.time.Instant.now());
                var adminUser = userRepo.findByEmailAddress("wcpredictor@thatcher.africa");
                adminUser.ifPresent(u -> pool.getUsers().add(u));
                poolRepo.save(pool);
                log.info("Default pool created with admin user.");
            }
        };
    }

    private String getCorrectGroup(String teamName) {
        for (var entry : GROUP_TEAMS.entrySet()) {
            for (String name : entry.getValue()) {
                if (name.equals(teamName)) return String.valueOf(entry.getKey());
            }
        }
        return null;
    }

    private int importScheduleCsvs(TournamentRepository tournamentRepo, MatchRepository matchRepo,
                                     TeamRepository teamRepo) {
        int imported = 0;
        try {
            var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:data/*-schedule.csv");
            for (var resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String tournamentName = filename.replace("-schedule.csv", "").replace("_", " ");
                Map<Integer, Match> existingByNumber = new HashMap<>();
                for (Match m : matchRepo.findAll()) {
                    existingByNumber.put(m.getMatchNumber(), m);
                }
                if (existingByNumber.containsKey(104)) {
                    fixGroupMatchPairings(resource, matchRepo, teamRepo);
                    log.info("Schedule already imported, fixed group match pairings only.");
                    continue;
                }
                Tournament t = new Tournament();
                t.setName(tournamentName);
                t.setCreatedAt(java.time.Instant.now());
                t = tournamentRepo.save(t);
                log.info("Created tournament: {}", tournamentName);

                for (Match m : matchRepo.findAll()) {
                    existingByNumber.put(m.getMatchNumber(), m);
                }
                Map<String, Team> teamByName = new HashMap<>();
                Map<String, String> csvAliases = Map.of(
                    "Czechia", "Czech Republic",
                    "Bosnia & Herzegovina", "Bosnia and Herzegovina",
                    "Cabo Verde", "Cape Verde",
                    "T\u00fcrkiye", "Turkey",
                    "Congo DR", "DR Congo",
                    "USA", "United States",
                    "IR Iran", "Iran"
                );
                for (Team team : teamRepo.findAll()) {
                    teamByName.put(team.getName(), team);
                    for (var alias : csvAliases.entrySet()) {
                        if (team.getName().equals(alias.getValue())) {
                            teamByName.put(alias.getKey(), team);
                        }
                    }
                }
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(resource.getInputStream(),
                                java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    boolean headerSkipped = false;
                    int lastMatchNum = 0;
                    while ((line = reader.readLine()) != null) {
                        if (!headerSkipped) { headerSkipped = true; continue; }
                        String[] cols = line.split(",", -1);
                        if (cols.length < 7) continue;
                        int matchNum = Integer.parseInt(cols[0].trim());
                        String utcDateStr = cols[1].trim();
                        boolean estimated = "TRUE".equalsIgnoreCase(cols[2].trim());
                        String csvTeam1 = cols[3].trim();
                        String csvTeam2 = cols[4].trim();
                        String venue = cols[5].trim();
                        LocalDateTime utcDate = LocalDateTime.parse(utcDateStr.replace(" ", "T"));
                        lastMatchNum = matchNum;

                        Match match = existingByNumber.get(matchNum);
                        if (match != null) {
                            if (match.getTournament() == null) match.setTournament(t);
                            match.setMatchDate(utcDate);
                            match.setMatchDateEstimated(estimated);
                            match.setVenue(venue);
                            // Update group-stage team pairings from CSV
                            if (matchNum <= 72) {
                                Team t1 = teamByName.get(csvTeam1);
                                Team t2 = teamByName.get(csvTeam2);
                                if (t1 != null) {
                                    match.setTeam1(t1);
                                    match.setGroupLetter(t1.getGroupLetter());
                                }
                                if (t2 != null) match.setTeam2(t2);
                            }
                            matchRepo.save(match);
                        } else {
                            RoundType round;
                            if (matchNum <= 88) round = RoundType.ROUND_OF_32;
                            else if (matchNum <= 96) round = RoundType.ROUND_OF_16;
                            else if (matchNum <= 100) round = RoundType.QUARTER_FINAL;
                            else if (matchNum <= 102) round = RoundType.SEMI_FINAL;
                            else if (matchNum == 103) round = RoundType.THIRD_PLACE;
                            else round = RoundType.FINAL;

                            Match newMatch = new Match();
                            newMatch.setMatchNumber(matchNum);
                            newMatch.setRound(round);
                            newMatch.setMatchDate(utcDate);
                            newMatch.setMatchDateEstimated(estimated);
                            newMatch.setVenue(venue);
                            newMatch.setTournament(t);
                            newMatch.setPredictionsLockTime(utcDate);
                            newMatch.setPredictionsLocked(true);
                            matchRepo.save(newMatch);
                        }
                    }
                    log.info("Imported {} matches for tournament '{}'", lastMatchNum, tournamentName);
                }
                imported++;
            }
        } catch (Exception e) {
            log.error("Failed to import schedule files: {}", e.getMessage());
        }
        return imported;
    }

    private void fixGroupMatchPairings(org.springframework.core.io.Resource resource,
                                        MatchRepository matchRepo, TeamRepository teamRepo) {
        try {
            Map<String, Team> teamByName = new HashMap<>();
            Map<String, String> csvAliases = Map.of(
                "Czechia", "Czech Republic",
                "Bosnia & Herzegovina", "Bosnia and Herzegovina",
                "Cabo Verde", "Cape Verde",
                "T\u00fcrkiye", "Turkey",
                "Congo DR", "DR Congo",
                "USA", "United States",
                "IR Iran", "Iran"
            );
            for (Team team : teamRepo.findAll()) {
                teamByName.put(team.getName(), team);
                for (var alias : csvAliases.entrySet()) {
                    if (team.getName().equals(alias.getValue())) {
                        teamByName.put(alias.getKey(), team);
                    }
                }
            }

            Map<Integer, Match> existingByNumber = new HashMap<>();
            for (Match m : matchRepo.findAll()) {
                existingByNumber.put(m.getMatchNumber(), m);
            }

            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(resource.getInputStream(),
                            java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                boolean headerSkipped = false;
                int fixed = 0;
                while ((line = reader.readLine()) != null) {
                    if (!headerSkipped) { headerSkipped = true; continue; }
                    String[] cols = line.split(",", -1);
                    if (cols.length < 7) continue;
                    int matchNum = Integer.parseInt(cols[0].trim());
                    if (matchNum > 72) break;

                    String csvTeam1 = cols[3].trim();
                    String csvTeam2 = cols[4].trim();
                    Match match = existingByNumber.get(matchNum);
                    if (match == null) continue;

                    Team t1 = teamByName.get(csvTeam1);
                    Team t2 = teamByName.get(csvTeam2);
                    if (t1 != null && t2 != null) {
                        UUID existing1 = match.getTeam1() != null ? match.getTeam1().getId() : null;
                        UUID existing2 = match.getTeam2() != null ? match.getTeam2().getId() : null;
                        if (!t1.getId().equals(existing1) || !t2.getId().equals(existing2)
                                || !t1.getGroupLetter().equals(match.getGroupLetter())) {
                            match.setTeam1(t1);
                            match.setTeam2(t2);
                            match.setGroupLetter(t1.getGroupLetter());
                            matchRepo.save(match);
                            fixed++;
                        }
                    }
                }
                log.info("Fixed {} group match pairings from CSV", fixed);
            }
        } catch (Exception e) {
            log.error("Failed to fix group match pairings: {}", e.getMessage());
        }
    }

    private void fixOverstuffedGroupPredictions(GroupAdvancementPredictionRepository gapRepo,
                                                  TeamRepository teamRepo, UserRepository userRepo) {
        Map<UUID, List<GroupAdvancementPrediction>> byUser = new HashMap<>();
        for (var gap : gapRepo.findAll()) {
            byUser.computeIfAbsent(gap.getUser().getId(), k -> new ArrayList<>()).add(gap);
        }

        int usersFixed = 0;
        for (var entry : byUser.entrySet()) {
            UUID userId = entry.getKey();
            List<GroupAdvancementPrediction> preds = entry.getValue();
            Set<UUID> allTeamIds = new HashSet<>();
            for (var p : preds) {
                allTeamIds.add(p.getTeam().getId());
            }

            var teams = teamRepo.findAllById(allTeamIds);
            Map<Character, List<Team>> byGroup = new LinkedHashMap<>();
            for (char g = 'A'; g <= 'L'; g++) byGroup.put(g, new ArrayList<>());
            for (Team t : teams) {
                if (t.getGroupLetter() != null) {
                    byGroup.get(t.getGroupLetter().charAt(0)).add(t);
                }
            }

            boolean needsFix = false;
            for (var gEntry : byGroup.entrySet()) {
                if (gEntry.getValue().size() > 3) needsFix = true;
            }

            if (!needsFix) continue;

            // Remove excess: trim any group over 3 down to 3
            Set<UUID> keep = new HashSet<>();
            long groupsWith3 = 0;
            for (var gEntry : byGroup.entrySet()) {
                List<Team> groupTeams = gEntry.getValue();
                if (groupTeams.size() > 3) {
                    keep.addAll(groupTeams.subList(0, 3).stream().map(Team::getId).toList());
                } else {
                    keep.addAll(groupTeams.stream().map(Team::getId).toList());
                }
                if (Math.min(groupTeams.size(), 3) == 3) groupsWith3++;
            }

            // If more than 8 groups have 3, reduce some to 2
            if (groupsWith3 > 8) {
                long toReduce = groupsWith3 - 8;
                for (var gEntry : byGroup.entrySet()) {
                    if (toReduce <= 0) break;
                    List<Team> groupTeams = gEntry.getValue();
                    if (groupTeams.size() >= 3) {
                        UUID removeId = groupTeams.get(2).getId();
                        keep.remove(removeId);
                        toReduce--;
                    }
                }
            }

            int removed = 0;
            for (var p : preds) {
                if (!keep.contains(p.getTeam().getId())) {
                    gapRepo.delete(p);
                    removed++;
                }
            }
            if (removed > 0) {
                var u = userRepo.findById(userId);
                String email = u.map(User::getEmailAddress).orElse("unknown");
                log.info("Cleaned {} overstuffed predictions for {} ({}) — removed {} teams",
                        preds.size(), email, userId, removed);
                usersFixed++;
            }
        }
        if (usersFixed > 0) {
            log.info("Fixed group predictions for {} users after group reassignment", usersFixed);
        }
    }
}
