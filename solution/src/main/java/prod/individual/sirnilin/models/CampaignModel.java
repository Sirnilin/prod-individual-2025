package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "campaigns")
@JsonPropertyOrder({
        "campaign_id",
        "advertiser_id",
        "impressions_limit",
        "clicks_limit",
        "cost_per_impression",
        "cost_per_click",
        "ad_title", "ad_text",
        "start_date",
        "end_date",
        "targeting"
})
public class CampaignModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("campaign_id")
    private UUID campaignId;

    @NotNull
    @JsonProperty("advertiser_id")
    private UUID advertiserId;

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "target_id")
    private TargetModel targeting;

    @JsonIgnore
    private int countImpressions;

    @JsonIgnore
    private int countClicks;
}
