package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "tournament_actuals")
@Data
public class TournamentActual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", unique = true, nullable = false)
    private Tournament tournament;

    private String goldenBoot;

    private String goldenBall;

    private String goldenGlove;

    private String youngPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fair_play_id")
    private Team fairPlay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entertaining_id")
    private Team mostEntertaining;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalist1_id")
    private Team finalist1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalist2_id")
    private Team finalist2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id")
    private Team championTeam;
}
