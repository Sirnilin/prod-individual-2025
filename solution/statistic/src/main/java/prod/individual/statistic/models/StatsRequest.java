package prod.individual.statistic.models;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StatsRequest {

    private List<UUID> campaignIds;

    public StatsRequest(List<UUID> campaignIds) {
        this.campaignIds = campaignIds;
    }
}
