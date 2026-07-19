package wcpredictor.repository;

import wcpredictor.entity.TournamentActual;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface TournamentActualRepository extends JpaRepository<TournamentActual, UUID> {
    Optional<TournamentActual> findByTournamentId(UUID tournamentId);
}
