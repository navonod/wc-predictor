package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.Game;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
}
