package prod.individual.sirnilin.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import prod.individual.sirnilin.models.TargetModel;

@Data
public class CampaignCreateRequest {

    @NotNull
    @JsonProperty("impressions_limit")
    private Integer impressionsLimit;

    @NotNull
    @JsonProperty("clicks_limit")
    private Integer clicksLimit;

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

    @NotNull
    @JsonProperty("start_date")
    private Integer startDate;

    @NotNull
    @JsonProperty("end_date")
    private Integer endDate;

    private TargetModel targeting;
}
