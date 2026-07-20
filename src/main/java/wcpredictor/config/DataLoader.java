package wcpredictor.config;

import wcpredictor.entity.*;
import wcpredictor.repository.*;
import wcpredictor.service.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private static final String[][] FIFA_COUNTRIES = {
        {"AFG", "Afghanistan"}, {"ALB", "Albania"}, {"ALG", "Algeria"}, {"ASA", "American Samoa"},
        {"AND", "Andorra"}, {"ANG", "Angola"}, {"AIA", "Anguilla"}, {"ATG", "Antigua and Barbuda"},
        {"ARG", "Argentina"}, {"ARM", "Armenia"}, {"ARU", "Aruba"}, {"AUS", "Australia"},
        {"AUT", "Austria"}, {"AZE", "Azerbaijan"}, {"BAH", "Bahamas"}, {"BHR", "Bahrain"},
        {"BAN", "Bangladesh"}, {"BRB", "Barbados"}, {"BLR", "Belarus"}, {"BEL", "Belgium"},
        {"BLZ", "Belize"}, {"BEN", "Benin"}, {"BER", "Bermuda"}, {"BHU", "Bhutan"},
        {"BOL", "Bolivia"}, {"BIH", "Bosnia and Herzegovina"}, {"BOT", "Botswana"}, {"BRA", "Brazil"},
        {"VGB", "British Virgin Islands"}, {"BRU", "Brunei"}, {"BUL", "Bulgaria"}, {"BFA", "Burkina Faso"},
        {"BDI", "Burundi"}, {"CAM", "Cambodia"}, {"CMR", "Cameroon"}, {"CAN", "Canada"},
        {"CPV", "Cape Verde"}, {"CAY", "Cayman Islands"}, {"CTA", "Central African Republic"}, {"CHA", "Chad"},
        {"CHI", "Chile"}, {"CHN", "China"}, {"TPE", "Chinese Taipei"}, {"COL", "Colombia"},
        {"COM", "Comoros"}, {"CGO", "Congo"}, {"COK", "Cook Islands"}, {"CRC", "Costa Rica"},
        {"CRO", "Croatia"}, {"CUB", "Cuba"}, {"CUW", "Curaçao"}, {"CYP", "Cyprus"},
        {"CZE", "Czech Republic"}, {"DEN", "Denmark"}, {"DJI", "Djibouti"}, {"DMA", "Dominica"},
        {"DOM", "Dominican Republic"}, {"COD", "DR Congo"}, {"ECU", "Ecuador"}, {"EGY", "Egypt"},
        {"SLV", "El Salvador"}, {"ENG", "England"}, {"EQG", "Equatorial Guinea"}, {"ERI", "Eritrea"},
        {"EST", "Estonia"}, {"SWZ", "Eswatini"}, {"ETH", "Ethiopia"}, {"FRO", "Faroe Islands"},
        {"FIJ", "Fiji"}, {"FIN", "Finland"}, {"FRA", "France"}, {"GAB", "Gabon"},
        {"GAM", "Gambia"}, {"GEO", "Georgia"}, {"GER", "Germany"}, {"GHA", "Ghana"},
        {"GIB", "Gibraltar"}, {"GRE", "Greece"}, {"GRN", "Grenada"}, {"GUM", "Guam"},
        {"GUA", "Guatemala"}, {"GUI", "Guinea"}, {"GNB", "Guinea-Bissau"}, {"GUY", "Guyana"},
        {"HAI", "Haiti"}, {"HON", "Honduras"}, {"HKG", "Hong Kong"}, {"HUN", "Hungary"},
        {"ISL", "Iceland"}, {"IND", "India"}, {"IDN", "Indonesia"}, {"IRN", "Iran"},
        {"IRQ", "Iraq"}, {"ISR", "Israel"}, {"ITA", "Italy"}, {"CIV", "Ivory Coast"},
        {"JAM", "Jamaica"}, {"JPN", "Japan"}, {"JOR", "Jordan"}, {"KAZ", "Kazakhstan"},
        {"KEN", "Kenya"}, {"KOS", "Kosovo"}, {"KUW", "Kuwait"}, {"KGZ", "Kyrgyzstan"},
        {"LAO", "Laos"}, {"LVA", "Latvia"}, {"LBN", "Lebanon"}, {"LES", "Lesotho"},
        {"LBR", "Liberia"}, {"LBY", "Libya"}, {"LIE", "Liechtenstein"}, {"LTU", "Lithuania"},
        {"LUX", "Luxembourg"}, {"MAC", "Macau"}, {"MAD", "Madagascar"}, {"MWI", "Malawi"},
        {"MAS", "Malaysia"}, {"MDV", "Maldives"}, {"MLI", "Mali"}, {"MLT", "Malta"},
        {"MTN", "Mauritania"}, {"MRI", "Mauritius"}, {"MEX", "Mexico"}, {"MDA", "Moldova"},
        {"MNG", "Mongolia"}, {"MNE", "Montenegro"}, {"MSR", "Montserrat"}, {"MAR", "Morocco"},
        {"MOZ", "Mozambique"}, {"MYA", "Myanmar"}, {"NAM", "Namibia"}, {"NEP", "Nepal"},
        {"NED", "Netherlands"}, {"NCL", "New Caledonia"}, {"NZL", "New Zealand"}, {"NCA", "Nicaragua"},
        {"NIG", "Niger"}, {"NGA", "Nigeria"}, {"PRK", "North Korea"}, {"MKD", "North Macedonia"},
        {"NIR", "Northern Ireland"}, {"NOR", "Norway"}, {"OMA", "Oman"}, {"PAK", "Pakistan"},
        {"PLE", "Palestine"}, {"PAN", "Panama"}, {"PNG", "Papua New Guinea"}, {"PAR", "Paraguay"},
        {"PER", "Peru"}, {"PHI", "Philippines"}, {"POL", "Poland"}, {"POR", "Portugal"},
        {"PUR", "Puerto Rico"}, {"QAT", "Qatar"}, {"IRL", "Republic of Ireland"}, {"ROU", "Romania"},
        {"RUS", "Russia"}, {"RWA", "Rwanda"}, {"SKN", "Saint Kitts and Nevis"}, {"LCA", "Saint Lucia"},
        {"VIN", "Saint Vincent and the Grenadines"}, {"SAM", "Samoa"}, {"SMR", "San Marino"},
        {"STP", "São Tomé and Príncipe"}, {"KSA", "Saudi Arabia"}, {"SCO", "Scotland"}, {"SEN", "Senegal"},
        {"SRB", "Serbia"}, {"SEY", "Seychelles"}, {"SLE", "Sierra Leone"}, {"SGP", "Singapore"},
        {"SVK", "Slovakia"}, {"SVN", "Slovenia"}, {"SOL", "Solomon Islands"}, {"SOM", "Somalia"},
        {"RSA", "South Africa"}, {"KOR", "South Korea"}, {"SSD", "South Sudan"}, {"ESP", "Spain"},
        {"SRI", "Sri Lanka"}, {"SDN", "Sudan"}, {"SUR", "Suriname"}, {"SWE", "Sweden"},
        {"SUI", "Switzerland"}, {"SYR", "Syria"}, {"TAH", "Tahiti"}, {"TJK", "Tajikistan"},
        {"TAN", "Tanzania"}, {"THA", "Thailand"}, {"TLS", "Timor-Leste"}, {"TOG", "Togo"},
        {"TGA", "Tonga"}, {"TRI", "Trinidad and Tobago"}, {"TUN", "Tunisia"}, {"TUR", "Turkey"},
        {"TKM", "Turkmenistan"}, {"TCA", "Turks and Caicos Islands"}, {"UGA", "Uganda"},
        {"UKR", "Ukraine"}, {"UAE", "United Arab Emirates"}, {"USA", "United States"}, {"URU", "Uruguay"},
        {"VIR", "U.S. Virgin Islands"}, {"UZB", "Uzbekistan"}, {"VAN", "Vanuatu"}, {"VEN", "Venezuela"},
        {"VIE", "Vietnam"}, {"WAL", "Wales"}, {"YEM", "Yemen"}, {"ZAM", "Zambia"}, {"ZIM", "Zimbabwe"},
    };

    @Bean
    public CommandLineRunner loadData(TeamRepository teamRepo, MatchRepository matchRepo,
                                       SettingRepository settingRepo, UserRepository userRepo,
                                       PasswordEncoder passwordEncoder, PoolRepository poolRepo,
                                       TournamentRepository tournamentRepo,
                                       GroupAdvancementPredictionRepository gapRepo,
                                       TournamentTeamRepository ttRepo,
                                       EntityManager entityManager,
                                       PredictionService predictionService) {
        return args -> {
            seedAllCountries(teamRepo);

            Tournament tournament = tournamentRepo.findAll().stream().findFirst().orElse(null);
            if (tournament == null) {
                tournament = new Tournament();
                tournament.setName("FIFA World Cup 2026");
                tournament.setDescription("Canada, Mexico, United States");
                tournament.setCreatedAt(Instant.now());
                tournament = tournamentRepo.save(tournament);
                log.info("Created default tournament: {}", tournament.getName());
            }

            migrateTournamentTeams(ttRepo, tournament, entityManager);

            fixOverstuffedGroupPredictions(gapRepo, teamRepo, userRepo, ttRepo, tournamentRepo);

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

            importScheduleCsvs(tournamentRepo, matchRepo, teamRepo, ttRepo);

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
                seedSettings(settingRepo);
            }

            if (userRepo.count() == 0) {
                User admin = new User();
                admin.setEmailAddress("wcpredictor@thatcher.africa");
                admin.setFirstName("Admin");
                admin.setLastName("");
                admin.setNickname("Admin");
                admin.setEncryptedPassword(passwordEncoder.encode("password"));
                admin.setAdmin(true);
                admin.setConfirmed(true);
                admin.setCreatedAt(Instant.now());
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
                pool.setCreatedAt(Instant.now());
                var adminUser = userRepo.findByEmailAddress("wcpredictor@thatcher.africa");
                adminUser.ifPresent(u -> pool.getUsers().add(u));
                poolRepo.save(pool);
                log.info("Default pool created with admin user.");
            }

            predictionService.trimAllPredictions();
        };
    }

    private void seedAllCountries(TeamRepository teamRepo) {
        Map<String, Team> existingByCode = new HashMap<>();
        Map<String, Team> existingByName = new HashMap<>();
        for (Team t : teamRepo.findAll()) {
            if (t.getCountryCode() != null) existingByCode.put(t.getCountryCode(), t);
            existingByName.put(t.getName(), t);
        }
        int created = 0, updated = 0;
        for (String[] row : FIFA_COUNTRIES) {
            Team team = existingByCode.get(row[0]);
            if (team == null) {
                team = existingByName.get(row[1]);
            }
            if (team == null) {
                team = new Team();
                team.setCountryCode(row[0]);
                team.setName(row[1]);
                teamRepo.save(team);
                created++;
            } else if (team.getCountryCode() == null) {
                team.setCountryCode(row[0]);
                teamRepo.save(team);
                updated++;
            }
        }
        if (created > 0 || updated > 0) {
            log.info("FIFA countries: {} created, {} updated with country codes", created, updated);
        } else {
            log.info("FIFA countries already seeded");
        }
    }

    private void migrateTournamentTeams(TournamentTeamRepository ttRepo, Tournament fallbackTournament,
                                         EntityManager em) {
        if (ttRepo.count() > 0) return;
        log.info("Migrating teams to TournamentTeam join table...");

        try {
            int rows = em.createNativeQuery(
                "INSERT INTO tournament_teams (id, tournament_id, team_id, group_letter, sort_order) " +
                "SELECT hex(randomblob(16)), tournament_id, id, group_letter, sort_order FROM teams " +
                "WHERE group_letter IS NOT NULL").executeUpdate();
            if (rows > 0) {
                log.info("SQL migration: migrated {} tournament-team rows", rows);
                em.createNativeQuery("ALTER TABLE teams DROP COLUMN group_letter").executeUpdate();
                em.createNativeQuery("ALTER TABLE teams DROP COLUMN sort_order").executeUpdate();
                em.createNativeQuery("ALTER TABLE teams DROP COLUMN tournament_id").executeUpdate();
                try { em.createNativeQuery("ALTER TABLE teams DROP COLUMN fifa_code").executeUpdate(); } catch (Exception ignored) {}
                log.info("Dropped orphan columns from teams table");
                return;
            }
        } catch (Exception e) {
            log.warn("SQL migration failed (column may not exist yet): {}", e.getMessage());
        }

        log.info("Assigning GROUP_TEAMS to default tournament...");
        int idx = 0;
        for (var entry : GROUP_TEAMS.entrySet()) {
            for (String teamName : entry.getValue()) {
                Team team = null;
                for (Team t : findAllTeams(em)) {
                    if (t.getName().equals(teamName)) { team = t; break; }
                }
                if (team != null) {
                    TournamentTeam tt = new TournamentTeam();
                    tt.setTournament(fallbackTournament);
                    tt.setTeam(team);
                    tt.setGroupLetter(String.valueOf(entry.getKey()));
                    tt.setSortOrder(idx);
                    ttRepo.save(tt);
                }
                idx++;
            }
        }
        log.info("Assigned GROUP_TEAMS to default tournament");
    }

    @SuppressWarnings("unchecked")
    private List<Team> findAllTeams(EntityManager em) {
        return em.createQuery("SELECT t FROM Team t").getResultList();
    }

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

    private void fixOverstuffedGroupPredictions(GroupAdvancementPredictionRepository gapRepo,
                                                  TeamRepository teamRepo, UserRepository userRepo,
                                                  TournamentTeamRepository ttRepo,
                                                  TournamentRepository tournamentRepo) {
        Tournament tournament = tournamentRepo.findAll().stream().findFirst().orElse(null);
        if (tournament == null) return;

        Map<UUID, Character> teamGroupMap = new HashMap<>();
        for (var tt : ttRepo.findByTournamentId(tournament.getId())) {
            if (tt.getGroupLetter() != null && tt.getGroupLetter().length() == 1) {
                teamGroupMap.put(tt.getTeam().getId(), tt.getGroupLetter().charAt(0));
            }
        }

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
                Character g = teamGroupMap.get(t.getId());
                if (g != null) byGroup.get(g).add(t);
            }

            boolean needsFix = false;
            for (var gEntry : byGroup.entrySet()) {
                if (gEntry.getValue().size() > 3) needsFix = true;
            }
            if (!needsFix) continue;

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

    private void importScheduleCsvs(TournamentRepository tournamentRepo, MatchRepository matchRepo,
                                      TeamRepository teamRepo, TournamentTeamRepository ttRepo) {
        try {
            var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:data/*-schedule.csv");
            for (var resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String tournamentName = filename.replace("-schedule.csv", "").replace("_", " ");

                if (tournamentRepo.findAll().stream().anyMatch(t -> t.getName().equals(tournamentName))) {
                    fixGroupMatchPairings(resource, matchRepo, teamRepo);
                    assignKnockoutTournamentIds(matchRepo, tournamentRepo, tournamentName);
                    log.info("Tournament '{}' already exists, fixed group match pairings.", tournamentName);
                    continue;
                }

                Tournament t = new Tournament();
                t.setName(tournamentName);
                t.setCreatedAt(Instant.now());
                t = tournamentRepo.save(t);
                log.info("Created tournament: {}", tournamentName);

                assignTournamentTeamsFromSchedule(resource, t, teamRepo, ttRepo);

                Map<Integer, Match> existingByNumber = new HashMap<>();
                for (Match m : matchRepo.findAll()) {
                    existingByNumber.put(m.getMatchNumber(), m);
                }
                Map<String, Team> teamByName = buildTeamNameMap(teamRepo);
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(resource.getInputStream(),
                                java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    boolean headerSkipped = false;
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

                        Match match = existingByNumber.get(matchNum);
                        if (match != null) {
                            if (match.getTournament() == null) match.setTournament(t);
                            match.setMatchDate(utcDate);
                            match.setMatchDateEstimated(estimated);
                            match.setVenue(venue);
                            if (matchNum <= 72) {
                                Team team1 = teamByName.get(csvTeam1);
                                Team team2 = teamByName.get(csvTeam2);
                                if (team1 != null) {
                                    match.setTeam1(team1);
                                    match.setGroupLetter(getGroupForTeam(team1, t, ttRepo));
                                }
                                if (team2 != null) match.setTeam2(team2);
                            }
                            matchRepo.save(match);
                        } else {
                            RoundType round = roundForMatch(matchNum);
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
                }
            }
        } catch (Exception e) {
            log.error("Failed to import schedule files: {}", e.getMessage());
        }
    }

    private void assignTournamentTeamsFromSchedule(org.springframework.core.io.Resource resource,
                                                     Tournament tournament, TeamRepository teamRepo,
                                                     TournamentTeamRepository ttRepo) {
        Map<String, Team> teamByName = buildTeamNameMap(teamRepo);
        Map<Character, Integer> groupOrder = new HashMap<>();
        int idx = 0;
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(resource.getInputStream(),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                String[] cols = line.split(",", -1);
                if (cols.length < 7) continue;
                int matchNum = Integer.parseInt(cols[0].trim());
                if (matchNum > 72) break;
                String csvTeam1 = cols[3].trim();
                String csvTeam2 = cols[4].trim();
                Team t1 = teamByName.get(csvTeam1);
                Team t2 = teamByName.get(csvTeam2);
                char group = csvTeam1.charAt(0); // approximate, overridden below
                for (Team t : new Team[]{t1, t2}) {
                    if (t == null) continue;
                    if (ttRepo.findByTournamentIdAndTeamId(tournament.getId(), t.getId()).isPresent()) continue;
                    TournamentTeam tt = new TournamentTeam();
                    tt.setTournament(tournament);
                    tt.setTeam(t);
                    tt.setGroupLetter(String.valueOf(group));
                    tt.setSortOrder(groupOrder.getOrDefault(group, 0));
                    ttRepo.save(tt);
                    groupOrder.put(group, groupOrder.getOrDefault(group, 0) + 1);
                }
            }
        } catch (Exception ignored) {}
        log.info("Assigned tournament teams from schedule CSV");
    }

    private String getGroupForTeam(Team team, Tournament tournament, TournamentTeamRepository ttRepo) {
        var tt = ttRepo.findByTournamentIdAndTeamId(tournament.getId(), team.getId());
        return tt.map(TournamentTeam::getGroupLetter).orElse(null);
    }

    private RoundType roundForMatch(int matchNum) {
        if (matchNum <= 88) return RoundType.ROUND_OF_32;
        if (matchNum <= 96) return RoundType.ROUND_OF_16;
        if (matchNum <= 100) return RoundType.QUARTER_FINAL;
        if (matchNum <= 102) return RoundType.SEMI_FINAL;
        if (matchNum == 103) return RoundType.THIRD_PLACE;
        return RoundType.FINAL;
    }

    private Map<String, Team> buildTeamNameMap(TeamRepository teamRepo) {
        Map<String, String> csvAliases = Map.of(
            "Czechia", "Czech Republic", "Bosnia & Herzegovina", "Bosnia and Herzegovina",
            "Cabo Verde", "Cape Verde", "T\u00fcrkiye", "Turkey", "Congo DR", "DR Congo",
            "USA", "United States", "IR Iran", "Iran"
        );
        Map<String, Team> map = new HashMap<>();
        for (Team team : teamRepo.findAll()) {
            map.put(team.getName(), team);
            for (var alias : csvAliases.entrySet()) {
                if (team.getName().equals(alias.getValue())) map.put(alias.getKey(), team);
            }
        }
        return map;
    }

    private void assignKnockoutTournamentIds(MatchRepository matchRepo,
                                               TournamentRepository tournamentRepo,
                                               String tournamentName) {
        Tournament t = tournamentRepo.findAll().stream()
                .filter(tr -> tr.getName().equals(tournamentName)).findFirst().orElse(null);
        if (t == null) return;
        int fixed = 0;
        for (Match m : matchRepo.findAll()) {
            if (m.getMatchNumber() >= 73 && m.getTournament() == null) {
                m.setTournament(t);
                matchRepo.save(m);
                fixed++;
            }
        }
        if (fixed > 0) log.info("Assigned tournament_id to {} knockout matches", fixed);
    }

    private void fixGroupMatchPairings(org.springframework.core.io.Resource resource,
                                        MatchRepository matchRepo, TeamRepository teamRepo) {
        Map<String, Team> teamByName = buildTeamNameMap(teamRepo);
        Map<Integer, Match> byNum = new HashMap<>();
        for (Match m : matchRepo.findAll()) { byNum.put(m.getMatchNumber(), m); }
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            int fixed = 0;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                String[] cols = line.split(",", -1);
                if (cols.length < 7) continue;
                int matchNum = Integer.parseInt(cols[0].trim());
                if (matchNum > 72) break;
                Match match = byNum.get(matchNum);
                if (match == null) continue;
                Team t1 = teamByName.get(cols[3].trim());
                Team t2 = teamByName.get(cols[4].trim());
                if (t1 != null && t2 != null) {
                    UUID e1 = match.getTeam1() != null ? match.getTeam1().getId() : null;
                    UUID e2 = match.getTeam2() != null ? match.getTeam2().getId() : null;
                    if (!t1.getId().equals(e1) || !t2.getId().equals(e2)) {
                        match.setTeam1(t1);
                        match.setTeam2(t2);
                        matchRepo.save(match);
                        fixed++;
                    }
                }
            }
            log.info("Fixed {} group match pairings from CSV", fixed);
        } catch (Exception e) {
            log.error("Failed to fix group match pairings: {}", e.getMessage());
        }
    }

    private void seedSettings(SettingRepository settingRepo) {
        log.info("Seeding default settings...");
        String[][] defaults = {
            {"points_correct_score", "3", "INTEGER"}, {"points_correct_margin", "2", "INTEGER"},
            {"points_correct_result", "1", "INTEGER"}, {"points_draw_correct", "3", "DOUBLE"},
            {"points_draw_diff", "1.5", "DOUBLE"}, {"points_golden_boot", "5", "INTEGER"},
            {"points_golden_ball", "5", "INTEGER"}, {"points_golden_glove", "5", "INTEGER"},
            {"points_young_player", "5", "INTEGER"}, {"points_fair_play", "5", "INTEGER"},
            {"points_entertaining", "5", "INTEGER"}, {"points_finalist", "5", "INTEGER"},
            {"points_champion", "20", "INTEGER"}, {"points_group_qualifier", "2", "INTEGER"},
        };
        for (String[] d : defaults) {
            Setting s = new Setting(); s.setName(d[0]); s.setValue(d[1]); s.setValueType(d[2]);
            settingRepo.save(s);
        }
        log.info("Seeded {} default settings.", defaults.length);
    }
}
