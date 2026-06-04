package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.Team;
import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByGroupLetterOrderByNameAsc(String groupLetter);
    List<Team> findByGroupLetterIsNotNullOrderByGroupLetterAscNameAsc();
    List<Team> findAllByOrderByNameAsc();
}
