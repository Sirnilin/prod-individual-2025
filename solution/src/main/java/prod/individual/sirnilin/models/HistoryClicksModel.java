package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "history_clicks")
public class HistoryClicksModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonProperty("client_id")
    private UUID clientId;

    @JsonProperty("campaign_id")
    private UUID campaignId;

    @JsonProperty("advertiser_id")
    private UUID advertiserId;

    private int date;

    public HistoryClicksModel(UUID clientId, UUID campaignId, UUID advertiserId, int date) {
        this.clientId = clientId;
        this.campaignId = campaignId;
        this.advertiserId = advertiserId;
        this.date = date;
    }

    public HistoryClicksModel() {
    }
}
