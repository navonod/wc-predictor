package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
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

    @ManyToMany(mappedBy = "users")
    private List<Pool> pools = new ArrayList<>();

    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private Instant createdAt;
}
