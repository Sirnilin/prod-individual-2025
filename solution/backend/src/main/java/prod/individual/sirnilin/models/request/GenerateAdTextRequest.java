package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GenerateAdTextRequest {

    @JsonProperty("campaign_description")
    private String campaignDescription;

}
