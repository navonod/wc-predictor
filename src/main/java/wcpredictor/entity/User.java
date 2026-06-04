package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firstName;
    private String lastName;
    private String nickname;

    @Column(nullable = false, unique = true)
    private String emailAddress;

    private boolean isAdmin;

    private Boolean confirmed = false;

    private String encryptedPassword;

    private String passwordResetToken;
    private Instant passwordResetTokenExpiry;
}
