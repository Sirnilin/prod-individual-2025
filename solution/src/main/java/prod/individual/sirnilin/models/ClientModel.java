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
    private UUID clientId;

    @NotBlank
    @Size(max = 255)
    private String login;

    @Min(0)
    private int age;

    @NotBlank
    @Size(max = 1000)
    private String location;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Gender gender;
}
