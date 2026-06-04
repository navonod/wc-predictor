package wcpredictor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "settings")
@Data
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "setting_value", nullable = false)
    private String value;

    @Column(nullable = false)
    private String valueType;
}
