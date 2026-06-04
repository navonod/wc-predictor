package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulated_matches",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id"}))
@Data
public class SimulatedMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    private Integer team1Score;
    private Integer team2Score;

    @Column(nullable = false)
    private Instant timestamp;
}
