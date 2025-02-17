package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class MlScoreRequest {

    @JsonProperty("advertiser_id")
    private UUID advertiserId;

    @JsonProperty("client_id")
    private UUID clientId;

    private int score;
}
