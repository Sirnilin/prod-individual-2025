package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "clients")
public class ClientModel {

    @Id
    @JsonProperty("client_id")
    @Column(unique = true)
    private UUID clientId;

    @NotBlank
    private String login;

    @Min(0)
    private int age;

    @NotBlank
    private String location;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Gender gender;
}
