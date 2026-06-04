package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.GroupAdvancementPrediction;
import java.util.List;
import java.util.UUID;

public interface GroupAdvancementPredictionRepository extends JpaRepository<GroupAdvancementPrediction, UUID> {
    List<GroupAdvancementPrediction> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
