package prod.individual.sirnilin.models.resonse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class AdsResponse {

    @JsonProperty("ad_id")
    private UUID adId;

    @JsonProperty("ad_title")
    private String adTitle;

    @JsonProperty("ad_text")
    private String adText;

    @JsonProperty("advertiser_id")
    private UUID advertiserId;

    public AdsResponse(UUID adId, String adTitle, String adText, UUID advertiserId) {
        this.adId = adId;
        this.adTitle = adTitle;
        this.adText = adText;
        this.advertiserId = advertiserId;
    }
}
