package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tournament_predictions")
@Data
public class TournamentPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private User user;

    private String goldenBoot;
    private String goldenBall;
    private String goldenGlove;
    private String youngPlayer;
    private String fairPlay;
    private String mostEntertaining;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalist1_id")
    private Team finalist1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalist2_id")
    private Team finalist2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id")
    private Team champion;

    @Column(nullable = false)
    private Instant timestamp;
}
