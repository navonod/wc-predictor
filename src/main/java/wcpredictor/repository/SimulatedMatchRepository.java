package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.Match;
import wcpredictor.entity.SimulatedMatch;
import wcpredictor.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulatedMatchRepository extends JpaRepository<SimulatedMatch, UUID> {
    List<SimulatedMatch> findByUser(User user);
    List<SimulatedMatch> findByUserId(UUID userId);
    Optional<SimulatedMatch> findByUserAndMatch(User user, Match match);
    List<SimulatedMatch> findByMatchId(UUID matchId);
    void deleteByUser(User user);
}
