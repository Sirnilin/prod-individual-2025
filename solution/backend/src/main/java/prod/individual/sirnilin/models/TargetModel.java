package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "targets")
public class TargetModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @JsonProperty("age_from")
    @Min(0)
    private Integer ageFrom;

    @JsonProperty("age_to")
    @Min(0)
    private Integer ageTo;

    private String location;
}
