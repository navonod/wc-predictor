package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Data
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int matchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundType round;

    @Column(nullable = false)
    private LocalDateTime matchDate;

    private String venue;

    @Column(length = 1)
    private String groupLetter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    private Team team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    private Team team2;

    private Integer team1Score;
    private Integer team2Score;

    @Column(nullable = false)
    private boolean predictionsLocked = false;

    private LocalDateTime predictionsLockTime;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean matchDateEstimated = false;

    public boolean isLocked() {
        if (predictionsLocked) return true;
        if (predictionsLockTime != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(predictionsLockTime)) return true;
        return false;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;
}
