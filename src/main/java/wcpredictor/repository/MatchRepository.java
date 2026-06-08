package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import wcpredictor.entity.Match;
import wcpredictor.entity.RoundType;
import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByRoundOrderByMatchDateAsc(RoundType round);
    List<Match> findByRoundInOrderByMatchDateAsc(List<RoundType> rounds);
    List<Match> findAllByOrderByMatchDateAsc();
    List<Match> findByGroupLetterOrderByMatchDateAsc(String groupLetter);

    @Query("SELECT m.round FROM Match m WHERE m.predictionsLocked = false GROUP BY m.round ORDER BY MIN(m.matchDate) ASC")
    List<RoundType> findOpenRounds();

    List<Match> findByTournamentId(UUID tournamentId);
}
