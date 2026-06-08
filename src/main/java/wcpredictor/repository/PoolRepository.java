package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.Pool;
import java.util.UUID;

public interface PoolRepository extends JpaRepository<Pool, UUID> {
}
