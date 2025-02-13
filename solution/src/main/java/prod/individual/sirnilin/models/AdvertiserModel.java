package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "advertisers")
public class AdvertiserModel {

    @Id
    @JsonProperty("advertiser_id")
    @NotNull
    private UUID advertiserId;

    @NotBlank
    private String name;

}
