package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class AdsClickRequest {

    @JsonProperty("client_id")
    private UUID clientId;

}
