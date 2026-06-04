package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.TournamentPrediction;
import java.util.Optional;
import java.util.UUID;

public interface TournamentPredictionRepository extends JpaRepository<TournamentPrediction, UUID> {
    Optional<TournamentPrediction> findByUserId(UUID userId);
}
