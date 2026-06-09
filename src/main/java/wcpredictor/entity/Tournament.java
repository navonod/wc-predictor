package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "tournament")
    private List<TournamentTeam> tournamentTeams = new ArrayList<>();
}
