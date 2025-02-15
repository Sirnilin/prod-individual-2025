package prod.individual.sirnilin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "history_impressions")
public class HistoryImpressionsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonProperty("campaign_id")
    private UUID clientId;

    @JsonProperty("advertiser_id")
    private UUID advertiserId;

    private int date;

    public HistoryImpressionsModel(UUID clientId, UUID advertiserId, int date) {
        this.clientId = clientId;
        this.advertiserId = advertiserId;
        this.date = date;
    }

    public HistoryImpressionsModel() {
    }
}
