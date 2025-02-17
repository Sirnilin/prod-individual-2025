package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "ml_score")
public class MlScoreModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    private UUID id;

    @JsonProperty("advertiser_id")
    private UUID advertiserId;

    @JsonProperty("client_id")
    private UUID clientId;

    private int score;

    public MlScoreModel(UUID advertiserId, UUID clientId, int score) {
        this.advertiserId = advertiserId;
        this.clientId = clientId;
        this.score = score;
    }

    public MlScoreModel() {
    }
}
