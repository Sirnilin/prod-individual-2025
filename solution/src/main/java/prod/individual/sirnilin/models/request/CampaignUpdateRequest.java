package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import prod.individual.sirnilin.models.TargetModel;

@Data
public class CampaignUpdateRequest {

    @NotNull
    @JsonProperty("cost_per_impression")
    private Float costPerImpression;

    @NotNull
    @JsonProperty("cost_per_click")
    private Float costPerClick;

    @NotBlank
    @JsonProperty("ad_title")
    private String adTitle;

    @NotBlank
    @JsonProperty("ad_text")
    private String adText;

    private TargetModel targeting;
}
