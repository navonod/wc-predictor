package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.Tournament;
import java.util.UUID;

public interface TournamentRepository extends JpaRepository<Tournament, UUID> {
}
