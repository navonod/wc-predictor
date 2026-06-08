package wcpredictor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TournamentService {

    private static final Logger log = LoggerFactory.getLogger(TournamentService.class);

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                              MatchRepository matchRepository) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id);
    }

    @Transactional
    public Tournament create(String name, String description) {
        Tournament t = new Tournament();
        t.setName(name);
        t.setDescription(description);
        t.setCreatedAt(Instant.now());
        return tournamentRepository.save(t);
    }

    @Transactional
    public void delete(UUID id) {
        tournamentRepository.deleteById(id);
    }

    @Transactional
    public int importSchedule(UUID tournamentId, java.io.InputStream csvStream) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow();

        Map<Integer, Match> existingByNumber = new HashMap<>();
        for (Match m : matchRepository.findAll()) {
            existingByNumber.put(m.getMatchNumber(), m);
        }

        int updated = 0;
        try (var reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                String[] cols = line.split(",", -1);
                if (cols.length < 7) continue;

                int matchNum = Integer.parseInt(cols[0].trim());
                String utcDateStr = cols[1].trim();
                boolean estimated = "TRUE".equalsIgnoreCase(cols[2].trim());
                String venue = cols[5].trim();
                LocalDateTime utcDate = LocalDateTime.parse(utcDateStr.replace(" ", "T"));

                Match match = existingByNumber.get(matchNum);
                if (match != null) {
                    match.setTournament(tournament);
                    match.setMatchDate(utcDate);
                    match.setMatchDateEstimated(estimated);
                    match.setVenue(venue);
                    matchRepository.save(match);
                    updated++;
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
                    newMatch.setTournament(tournament);
                    newMatch.setPredictionsLockTime(utcDate);
                    newMatch.setPredictionsLocked(true);
                    matchRepository.save(newMatch);
                    updated++;
                }
            }
            log.info("Imported {} matches for tournament '{}'", updated, tournament.getName());
        } catch (Exception e) {
            log.error("Failed to import schedule: {}", e.getMessage());
            throw new RuntimeException("Failed to import schedule: " + e.getMessage());
        }
        return updated;
    }
}
