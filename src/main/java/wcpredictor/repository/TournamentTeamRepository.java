package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.TournamentTeam;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, UUID> {
    List<TournamentTeam> findByTournamentIdAndGroupLetterNotNullOrderByGroupLetterAscSortOrderAsc(UUID tournamentId);
    List<TournamentTeam> findByTournamentIdAndGroupLetterOrderBySortOrderAsc(UUID tournamentId, String groupLetter);
    Optional<TournamentTeam> findByTournamentIdAndTeamId(UUID tournamentId, UUID teamId);
    List<TournamentTeam> findByTournamentId(UUID tournamentId);
}
