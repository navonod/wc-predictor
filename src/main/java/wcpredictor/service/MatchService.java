package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.MatchRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateAsc();
    }

    public List<Match> getMatchesByRound(RoundType round) {
        return matchRepository.findByRoundOrderByMatchDateAsc(round);
    }

    public List<Match> getMatchesByGroup(String groupLetter) {
        return matchRepository.findByGroupLetterOrderByMatchDateAsc(groupLetter);
    }

    public Optional<Match> findById(UUID id) {
        return matchRepository.findById(id);
    }

    @Transactional
    public Match save(Match match) {
        return matchRepository.save(match);
    }

    public List<RoundType> getOpenRounds() {
        return matchRepository.findOpenRounds();
    }

    public List<RoundType> getAllRoundTypes() {
        return Arrays.asList(RoundType.values());
    }

    public Map<RoundType, List<Match>> getAllMatchesGroupedByRound() {
        return getAllMatches().stream()
                .collect(Collectors.groupingBy(Match::getRound, LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional
    public void lockRound(RoundType round) {
        List<Match> matches = matchRepository.findByRoundOrderByMatchDateAsc(round);
        for (Match m : matches) {
            m.setPredictionsLocked(true);
        }
        matchRepository.saveAll(matches);
    }

    @Transactional
    public void unlockRound(RoundType round) {
        List<Match> matches = matchRepository.findByRoundOrderByMatchDateAsc(round);
        for (Match m : matches) {
            m.setPredictionsLocked(false);
        }
        matchRepository.saveAll(matches);
    }

    public Match getNextMatchToStart() {
        return getAllMatches().stream()
                .filter(m -> m.getTeam1Score() == null && m.getMatchDate() != null)
                .findFirst().orElse(null);
    }
}
