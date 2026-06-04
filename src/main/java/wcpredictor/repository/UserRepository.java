package wcpredictor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wcpredictor.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAddress(String emailAddress);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
}
