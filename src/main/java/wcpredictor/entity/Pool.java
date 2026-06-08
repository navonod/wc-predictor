package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "pools")
@Getter
@Setter
public class Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
        name = "pool_users",
        joinColumns = @JoinColumn(name = "pool_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users = new ArrayList<>();
}
