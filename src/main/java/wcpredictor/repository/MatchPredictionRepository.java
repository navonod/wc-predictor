package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.MatchPrediction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchPredictionRepository extends JpaRepository<MatchPrediction, UUID> {
    Optional<MatchPrediction> findByUserIdAndMatchId(UUID userId, UUID matchId);
    List<MatchPrediction> findByUserId(UUID userId);
    List<MatchPrediction> findByMatchId(UUID matchId);
    List<MatchPrediction> findByUserIdAndMatchRoundOrderByMatchMatchDateAsc(UUID userId, wcpredictor.entity.RoundType round);
}
