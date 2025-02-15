package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Entity
@Table(name = "time")
public class TimeModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @JsonProperty("current_date")
    @Column(name = "recorded_date")
    @NotNull(message = "Current date cannot be null")
    @Positive(message = "Current date must be positive")
    private Integer currentDate;
}
